package net.jonbell.surefire.provider;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.maven.plugins.surefire.report.ReportTestSuite;
import org.apache.maven.plugins.surefire.report.SurefireReportParser;
import org.apache.maven.reporting.MavenReportException;

public class TestResultSource {
	public String directory;
	public String name;

	private SurefireReportParser parser;
	private Map<String, ReportTestSuite> tests;

	public Map<String, ReportTestSuite> getTests() {
		if (tests == null) {
			this.tests = new HashMap<String, ReportTestSuite>();
			try {
				List<ReportTestSuite> tests = this.getParser().parseXMLReportFiles();
				for (ReportTestSuite s : tests) {
					this.tests.put(s.getFullClassName(), s);
				}
			} catch (MavenReportException ex) {
				ex.printStackTrace();
			}
		}
		return tests;
	}

	public SurefireReportParser getParser() {
		if (parser == null)
			this.parser = new SurefireReportParser(Collections.singletonList(new File(directory)), Locale.US);
		return parser;
	}

	@Override
	public String toString() {
		return "TestResultSource [directory=" + directory + ", name=" + name + ", tests=" + getTests() + "]";
	}

}
