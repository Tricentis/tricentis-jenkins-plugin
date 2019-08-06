package com.tricentis.tosca.jenkins;

import java.io.IOException;
import java.io.PrintStream;

import javax.servlet.ServletException;

import org.apache.commons.lang.StringUtils;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;

import hudson.AbortException;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.Launcher.ProcStarter;
import hudson.Util;
import hudson.model.AbstractProject;
import hudson.model.Job;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.FormValidation;
import jenkins.tasks.SimpleBuildStep;

/**
 * Builder which runs Tricentis CI Client and publishes results.
 *
 * @author Sergey Oplavin
 *
 */
public class TricentisCiBuilder extends Builder implements SimpleBuildStep {

	private static final String RESULTS_FILE_NAME = "results.xml";
	private transient ProcStarterFactory procStarterFactory;
	private transient JUnitResultsPublisher resultsPublisher;
	private transient ProcessExecutor executor;
	private String tricentisClientPath;
	private String configurationFilePath;
	private String endpoint;
	private String testEvents;
	private static final String EMPTY_STRING = "";

	/**
	 * Constructor.
	 *
	 * @param tricentisClientPath client executable or jar file path.
	 * @param endpoint            endpoint.
	 */
	@DataBoundConstructor
	public TricentisCiBuilder(final String tricentisClientPath, final String endpoint) {
		setTricentisClientPath(tricentisClientPath);
		setEndpoint(endpoint);
	}

	@Override
	public void perform(final Run<?, ?> run, final FilePath workspace, final Launcher launcher,
			final TaskListener listener) throws InterruptedException, IOException {
		final PrintStream logger = listener.getLogger();
		logParameters(logger);
		assertParameters();
		final ProcStarter starter = getProcStarterFactory().create(this, run, workspace, launcher, listener);
		final int exitCode = getProcessExecutor().execute(starter);
		logger.println(Messages.publishJUnit());
		getResultsPublisher().publish(getResultsFile(), run, workspace, launcher, listener);
		if (exitCode != 0) {
			throw new AbortException(Messages.exitCodeNotZero(exitCode));
		}
		logger.println(Messages.done());
	}

	public String getTricentisClientPath() {
		if (tricentisClientPath == null)
			return "$TRICENTIS_HOME\\ToscaCI\\Client\\ToscaCIJavaClient.jar";
		return tricentisClientPath;
	}

	@DataBoundSetter
	public void setTricentisClientPath(final String tricentisClientPath) {
		if (tricentisClientPath == null || tricentisClientPath.trim().isEmpty())
			this.tricentisClientPath = EMPTY_STRING;
		else
			this.tricentisClientPath = fixPath(tricentisClientPath);
	}

	public String getConfigurationFilePath() {
		if (configurationFilePath == null)
			return "$TRICENTIS_HOME\\ToscaCI\\Client\\Testconfig.xml";
		return configurationFilePath;
	}

	@DataBoundSetter
	public void setConfigurationFilePath(final String configurationFilePath) {
		if (configurationFilePath == null || configurationFilePath.trim().isEmpty())
			this.configurationFilePath = EMPTY_STRING;
		else
			this.configurationFilePath = fixPath(configurationFilePath);
	}

	public String getTestEvents() {
		if (testEvents == null)
			return EMPTY_STRING;
		return testEvents;
	}

	@DataBoundSetter
	public void setTestEvents(final String testEvents) {
		if (testEvents == null || testEvents.trim().isEmpty())
			this.testEvents = EMPTY_STRING;
		else
			this.testEvents = testEvents;
	}

	public String getEndpoint() {
		if (endpoint == null)
			return "http://servername/DistributionServerService/ManagerService.svc";
		return endpoint;
	}

	@DataBoundSetter
	public void setEndpoint(final String endpoint) {
		if (endpoint == null || endpoint.trim().isEmpty())
			this.endpoint = EMPTY_STRING;
		else
			this.endpoint = Util.fixEmptyAndTrim(endpoint);
	}

	public String getResultsFile() {
		return RESULTS_FILE_NAME;
	}

	void setProcStarterFactory(final ProcStarterFactory procStarterFactory) {
		this.procStarterFactory = procStarterFactory;
	}

	ProcStarterFactory getProcStarterFactory() {
		if (procStarterFactory == null) {
			procStarterFactory = new DefaultProcStarterFactory();
		}
		return procStarterFactory;
	}

	void setResultsPublisher(final JUnitResultsPublisher resultsPublisher) {
		this.resultsPublisher = resultsPublisher;
	}

	JUnitResultsPublisher getResultsPublisher() {
		if (resultsPublisher == null) {
			resultsPublisher = new DefaultJUnitResultsPublisher();
		}
		return resultsPublisher;
	}

	void setProcessExecutor(final ProcessExecutor executor) {
		this.executor = executor;
	}

	ProcessExecutor getProcessExecutor() {
		if (executor == null) {
			executor = new DefaultProcessExecutor();
		}
		return executor;
	}

	private void logParameters(final PrintStream logger) {
		logger.println(Messages.runJobLog(Messages.pluginTitle()));
		logger.println(Messages.tricentisClientPath() + ": " + getTricentisClientPath());
		logger.println(Messages.endpoint() + ": " + getEndpoint());
		logger.println(Messages.configurationFilePath() + ": " + getConfigurationFilePath());
		logger.println(Messages.resultsFile() + ": " + getResultsFile());
		logger.println(Messages.testEvents() + ": " + getTestEvents());
	}

	private void assertParameters() {
		assertParameter(Messages.tricentisClientPath(), tricentisClientPath);
		assertParameter(Messages.endpoint(), endpoint);
	}

	private void assertParameter(final String name, final String value) {
		if (value == null) {
			throw new IllegalArgumentException(Messages.parametersNullError(name));
		}
	}

	private String fixPath(final String path) {
		return StringUtils.removeEnd(StringUtils.removeStart(path.trim(), "\""), "\"");
	}

	/**
	 * Descriptor for {@link TricentisCiBuilder}.
	 *
	 * @author Sergey Oplavin
	 *
	 */
	@Symbol("tricentisCI")
	@Extension
	public static class Descriptor extends BuildStepDescriptor<Builder> {

		@SuppressWarnings("rawtypes")
		@Override
		public boolean isApplicable(final Class<? extends AbstractProject> jobType) {
			return true;
		}

		@Override
		public String getDisplayName() {
			return Messages.pluginTitle();
		}

		public FormValidation doCheckTricentisClientPath(@AncestorInPath final AbstractProject<?, ?> project,
				@QueryParameter final String tricentisClientPath) throws IOException, ServletException {
			project.checkPermission(Job.CONFIGURE);
			return validateRequiredField(tricentisClientPath);
		}

		public FormValidation doCheckEndpoint(@AncestorInPath final AbstractProject<?, ?> project,
				@QueryParameter final String endpoint) throws IOException, ServletException {
			project.checkPermission(Job.CONFIGURE);
			return validateRequiredField(endpoint);
		}

		private FormValidation validateRequiredField(final String value) {
			final String trimmed = Util.fixEmptyAndTrim(value);
			return trimmed != null ? FormValidation.ok() : FormValidation.error(Messages.required());
		}

	}

}
