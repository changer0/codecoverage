/*******************************************************************************
 * Copyright (c) 2009, 2021 Mountainminds GmbH & Co. KG and Contributors
 * This program and the accompanying materials are made available under
 * the terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    John Keeping - initial implementation
 *    Marc R. Hoffmann - rework
 *
 *******************************************************************************/
package org.jacoco.cli.internal.commands;

import java.io.*;
import java.util.*;

import org.jacoco.cli.internal.Command;
import org.jacoco.core.analysis.*;
import org.jacoco.core.data.ExecutionDataStore;
import org.jacoco.core.tools.ExecFileLoader;
import org.jacoco.report.DirectorySourceFileLocator;
import org.jacoco.report.FileMultiReportOutput;
import org.jacoco.report.IReportVisitor;
import org.jacoco.report.ISourceFileLocator;
import org.jacoco.report.MultiReportVisitor;
import org.jacoco.report.MultiSourceFileLocator;
import org.jacoco.report.csv.CSVFormatter;
import org.jacoco.report.html.HTMLFormatter;
import org.jacoco.report.xml.XMLFormatter;
import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.Option;

/**
 * The <code>report</code> command.
 */
public class Report extends Command {

	@Argument(usage = "list of JaCoCo *.exec files to read", metaVar = "<execfiles>")
	List<File> execfiles = new ArrayList<File>();

	@Option(name = "--classfiles", usage = "location of Java class files", metaVar = "<path>", required = true)
	List<File> classfiles = new ArrayList<File>();

	@Option(name = "--sourcefiles", usage = "location of the source files", metaVar = "<path>")
	List<File> sourcefiles = new ArrayList<File>();

	@Option(name = "--tabwith", usage = "tab stop width for the source pages (default 4)", metaVar = "<n>")
	int tabwidth = 4;

	@Option(name = "--name", usage = "name used for this report", metaVar = "<name>")
	String name = "JaCoCo Coverage Report";

	@Option(name = "--encoding", usage = "source file encoding (by default platform encoding is used)", metaVar = "<charset>")
	String encoding;

	@Option(name = "--xml", usage = "output file for the XML report", metaVar = "<file>")
	File xml;

	@Option(name = "--csv", usage = "output file for the CSV report", metaVar = "<file>")
	File csv;

	@Option(name = "--html", usage = "output directory for the HTML report", metaVar = "<dir>")
	File html;

	@Option(name = "--excludes", usage = "used to exclude classes", metaVar = "<file>")
	File excludesFile;

	@Option(name = "--increment", usage = "incremental class ", metaVar = "<file>")
	File incrementFile;

	@Override
	public String description() {
		return "Generate reports in different formats by reading exec and Java class files.";
	}

	@Override
	public int execute(final PrintWriter out, final PrintWriter err)
			throws IOException {
		final ExecFileLoader loader = loadExecutionData(out);
		final IBundleCoverage bundle = analyze(loader.getExecutionDataStore(), out);
		replaceBundleIncrementClass(bundle, out);
		replaceBundleExcludeClass(bundle, out);
		writeReports(bundle, loader, out);
		return 0;
	}

	private final Set<String> incrementClassSet = new HashSet<String>();

	private void replaceBundleIncrementClass(IBundleCoverage bundle, final PrintWriter out) {
		if (incrementFile == null) {
			return;
		}
		analysisStringFile(out, incrementFile, incrementClassSet, "increment");
		Collection<IPackageCoverage> packages = bundle.getPackages();
		out.println("need to increment class: ");
		Set<Long> ids = new HashSet<Long>();
		for (IPackageCoverage aPackage : packages) {
			Collection<IClassCoverage> classes = aPackage.getClasses();
			for (IClassCoverage aClass : classes) {
				if (isInclude(incrementClassSet, aClass.getName())) {
					out.println(aClass.getName());
					ids.add(aClass.getId());
				}
			}
		}
		//执行删除
		for (IPackageCoverage aPackage : packages) {
			Collection<IClassCoverage> classes = aPackage.getClasses();
			Iterator<IClassCoverage> iterator = classes.iterator();
			while (iterator.hasNext()) {
				IClassCoverage aClass = iterator.next();
				if (!ids.contains(aClass.getId())) {
					iterator.remove();
				}
			}
		}
	}

	private final Set<String> excludeClassSet = new HashSet<String>();

	private void replaceBundleExcludeClass(IBundleCoverage bundle, final PrintWriter out) {
		if (excludesFile == null) {
			return;
		}
		analysisStringFile(out, excludesFile, excludeClassSet, "excludes");
		Collection<IPackageCoverage> packages = bundle.getPackages();
		out.println("need to exclude class: ");
		for (IPackageCoverage aPackage : packages) {
			Collection<IClassCoverage> classes = aPackage.getClasses();
			Iterator<IClassCoverage> iterator = classes.iterator();
			while (iterator.hasNext()) {
				IClassCoverage aClass = iterator.next();
				if (isInclude(excludeClassSet, aClass.getName())) {
					out.println(aClass.getName());
					iterator.remove();
				}
			}
		}
	}

	private void analysisStringFile(PrintWriter out, File file, Set<String> stringSet,  String log) {
		if (file == null) {
			return;
		}
		out.println(log + " file path: " + file.getPath());
		BufferedReader br = null;
		try {
			br = new BufferedReader(new FileReader(file));
			out.println(log + " file content: ");
			while(br.ready()) {
				String x = br.readLine();
				stringSet.add(x);
				out.println(x);
			}

		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				if (br != null) {
					br.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	private boolean isInclude(Set<String> stringSet, String className) {
		Iterator<String> iterator = stringSet.iterator();
		while (iterator.hasNext()) {
			String next = iterator.next();
			if (next.contains(className)) {
				iterator.remove();
				return true;
			}
		}
		return false;
	}

	private ExecFileLoader loadExecutionData(final PrintWriter out)
			throws IOException {
		final ExecFileLoader loader = new ExecFileLoader();
		if (execfiles.isEmpty()) {
			out.println("[WARN] No execution data files provided.");
		} else {
			for (final File file : execfiles) {
				out.printf("[INFO] Loading execution data file %s.%n",
						file.getAbsolutePath());
				loader.load(file);
			}
		}
		return loader;
	}

	private IBundleCoverage analyze(final ExecutionDataStore data,
			final PrintWriter out) throws IOException {
		final CoverageBuilder builder = new CoverageBuilder();
		final Analyzer analyzer = new Analyzer(data, builder);
		for (final File f : classfiles) {
			analyzer.analyzeAll(f);
		}
		printNoMatchWarning(builder.getNoMatchClasses(), out);
		return builder.getBundle(name);
	}

	private void printNoMatchWarning(final Collection<IClassCoverage> nomatch,
			final PrintWriter out) {
		if (!nomatch.isEmpty()) {
			out.println(
					"[WARN] Some classes do not match with execution data.");
			out.println(
					"[WARN] For report generation the same class files must be used as at runtime.");
			for (final IClassCoverage c : nomatch) {
				out.printf(
						"[WARN] Execution data for class %s does not match.%n",
						c.getName());
			}
		}
	}

	private void writeReports(final IBundleCoverage bundle,
			final ExecFileLoader loader, final PrintWriter out)
			throws IOException {
		out.printf("[INFO] Analyzing %s classes.%n",
				Integer.valueOf(bundle.getClassCounter().getTotalCount()));
		final IReportVisitor visitor = createReportVisitor();
		visitor.visitInfo(loader.getSessionInfoStore().getInfos(),
				loader.getExecutionDataStore().getContents());
		visitor.visitBundle(bundle, getSourceLocator());
		visitor.visitEnd();
	}

	private IReportVisitor createReportVisitor() throws IOException {
		final List<IReportVisitor> visitors = new ArrayList<IReportVisitor>();

		if (xml != null) {
			final XMLFormatter formatter = new XMLFormatter();
			visitors.add(formatter.createVisitor(new FileOutputStream(xml)));
		}

		if (csv != null) {
			final CSVFormatter formatter = new CSVFormatter();
			visitors.add(formatter.createVisitor(new FileOutputStream(csv)));
		}

		if (html != null) {
			final HTMLFormatter formatter = new HTMLFormatter();
			visitors.add(
					formatter.createVisitor(new FileMultiReportOutput(html)));
		}

		return new MultiReportVisitor(visitors);
	}

	private ISourceFileLocator getSourceLocator() {
		final MultiSourceFileLocator multi = new MultiSourceFileLocator(
				tabwidth);
		for (final File f : sourcefiles) {
			multi.add(new DirectorySourceFileLocator(f, encoding, tabwidth));
		}
		return multi;
	}

}
