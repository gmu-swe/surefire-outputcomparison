package net.jonbell.surefire.provider;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Scanner;

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.surefire.report.ReportTestCase;
import org.apache.maven.plugins.surefire.report.ReportTestSuite;
import org.apache.maven.plugins.surefire.report.SurefireReportParser;
import org.apache.maven.reporting.MavenReportException;

@Mojo(name = "analyze", defaultPhase = LifecyclePhase.PRE_INTEGRATION_TEST)
public class OutputComparingMojo extends AbstractMojo {

	@Parameter(required = true)
	private List<TestResultSource> testResultSources;

	@Parameter(defaultValue = "${project.reporting.outputEncoding}", readonly = true, required = true)
	private String reportEncoding;

	public OutputComparingMojo() {

	}

	public void execute() throws MojoExecutionException, MojoFailureException {
		final Log consoleLogger = getLog();
		consoleLogger.info("Using configured reporting directories:");
		for (TestResultSource s : testResultSources)
			consoleLogger.info(s.name + ": " + s.directory);
		consoleLogger.info("------------------------------------------------------------------------");
		consoleLogger.info("TESTS");
		consoleLogger.info("------------------------------------------------------------------------");
		ArrayList<String> testsToRun = new ArrayList<String>();
		testsToRun.addAll(testResultSources.get(0).getTests().keySet());
		testsToRun.sort(new Comparator<String>() {
			public int compare(String o1, String o2) {
				return o1.compareTo(o2);
			}
		});
		int totalTests = 0;
		int totalErrors = 0;
		int totalFailures = 0;
		int totalSkipped = 0;
		for (String t : testsToRun) {
			consoleLogger.info("Running " + t);
			int thisNTests = 0;
			ReportTestSuite last = null;
			String lastOutput = null;
			boolean errored = false;
			for (TestResultSource s : testResultSources) {
				consoleLogger.info("Execution: " + s.name);
				ReportTestSuite r = s.getTests().get(t);
				if (r == null) {
					errored = true;
					consoleLogger.info("Test execution not found!");
					continue;
				}
				consoleLogger.info("\tTests run: " + (r.getNumberOfTests() + r.getNumberOfFailures() + r.getNumberOfErrors()) + ", Failures: " + r.getNumberOfFailures() + ", Errors: " + r.getNumberOfErrors());
				File outputFile = new File(s.directory, t + "-output.txt");
				String output = null;
				if (outputFile.exists()) {
					try {
						Scanner scan = new Scanner(outputFile, reportEncoding);
						output = scan.useDelimiter("\\Z").next();
						scan.close();
					} catch (IOException e) {
						e.printStackTrace();
						errored = true;
						consoleLogger.info("Test output file not found!");
						continue;
					}
					consoleLogger.info("\tOutput:");
					consoleLogger.info(output);
				}
				if (r.getNumberOfErrors() > 0 || r.getNumberOfFailures() > 0) {
					for (ReportTestCase tcase : r.getTestCases()) {
						if (tcase.hasFailure()) {
							consoleLogger.info("FAILURE: " + tcase.getFullClassName() + "." + tcase.getName() + ": " + tcase.getFailureMessage() + tcase.getFailureDetail());
						}
					}
				}
				if (last == null) {
					// for(ReportTestCase tc : )
					last = r;
					lastOutput = output;
					thisNTests = last.getNumberOfTests();
					totalTests += last.getNumberOfTests();
					totalSkipped += last.getNumberOfSkipped();
					totalFailures += last.getNumberOfSkipped() + last.getNumberOfErrors();
				} else {
					if (r.getNumberOfErrors() != last.getNumberOfErrors() || r.getNumberOfFailures() != last.getNumberOfFailures() || r.getNumberOfSkipped() != last.getNumberOfSkipped() || r.getNumberOfSkipped() != last.getNumberOfSkipped()) {
						errored = true;
						consoleLogger.info("^^^ Differing # of tests! <<<< FAILURE! - in " + t + "#" + s.name);
					} else {
						if (!(output == null && lastOutput == null) && !output.equals(lastOutput)) {
							errored = true;
							consoleLogger.info("^^^ Differing output! <<<< FAILURE! - in " + t + "#" + s.name);
						}
					}
				}
			}
			if(errored)
				totalErrors++;
			consoleLogger.info("Total: Tests run: " + thisNTests + ", Failures: 0, Errors: " + (errored ? 1 : 0) + ", Skipped: 0");
			consoleLogger.info("-------------------------------------------");
		}
		consoleLogger.info("");
		consoleLogger.info("Results: Tests run: " + totalTests + ", Failures: " + totalFailures + ", Errors: " + totalErrors + ", Skipped: " + totalSkipped);
		if(totalErrors > 0)
			throw new MojoFailureException("Tests failed.");
	}
}
