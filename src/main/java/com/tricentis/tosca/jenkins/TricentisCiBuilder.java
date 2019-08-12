/*
 **/
package com.tricentis.tosca.jenkins;

import java.io.File;
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
 */
public class TricentisCiBuilder extends Builder implements SimpleBuildStep {
    /** The Constant RESULTS_FILE_NAME. */
    private static final String RESULTS_FILE_NAME = "results.xml";
    /** The proc starter factory. */
    private transient ProcStarterFactory procStarterFactory;
    /** The results publisher. */
    private transient JUnitResultsPublisher resultsPublisher;
    /** The executor. */
    private transient ProcessExecutor executor;
    /** The tricentis client path. */
    private String tricentisClientPath;
    /** The configuration file path. */
    private String configurationFilePath;
    /** The endpoint. */
    private String endpoint;
    /** The test events. */
    private String testEvents;
    /** The Constant EMPTY_STRING. */
    private static final String EMPTY_STRING = "";
    /** The Constant DEFAULT_ENDPOINT. */
    private static final String DEFAULT_ENDPOINT
            = "http://servername/DistributionServerService/ManagerService.svc";

    /**
     * Constructor.
     *
     * @param newTricentisClientPath client executable or jar file path.
     * @param newEndpoint            endpoint.
     */
    @DataBoundConstructor
    public TricentisCiBuilder(final String newTricentisClientPath,
            final String newEndpoint) {
        setTricentisClientPath(newTricentisClientPath);
        setEndpoint(newEndpoint);
    }
    /**
     * Perform.
     *
     * @param run       the run
     * @param workspace the workspace
     * @param launcher  the launcher
     * @param listener  the listener
     * @throws InterruptedException the interrupted exception
     * @throws IOException          Signals that an I/O exception has occurred.
     */
    @Override
    public void perform(final Run<?, ?> run, final FilePath workspace,
            final Launcher launcher, final TaskListener listener)
            throws InterruptedException, IOException {
        final PrintStream logger = listener.getLogger();
        logParameters(logger);
        assertParameters();
        final ProcStarter starter = getProcStarterFactory().create(this, run,
                workspace, launcher, listener);
        final int exitCode = getProcessExecutor().execute(starter);
        logger.println(Messages.publishJUnit());
        getResultsPublisher().publish(getResultsFile(), run, workspace,
                launcher, listener);
        if (exitCode != 0) {
            throw new AbortException(Messages.exitCodeNotZero(exitCode));
        }
        logger.println(Messages.done());
    }
    /**
     * Gets the tricentis client path.
     *
     * @return the tricentis client path
     */
    public String getTricentisClientPath() {
        if (tricentisClientPath == null) {
            return "$TRICENTIS_HOME\\ToscaCI\\Client\\ToscaCIJavaClient.jar";
        }
        return tricentisClientPath;
    }
    /**
     * Sets the tricentis client path.
     *
     * @param newTricentisClientPath the new tricentis client path
     */
    @DataBoundSetter
    public void setTricentisClientPath(final String newTricentisClientPath) {
        if (newTricentisClientPath == null
                || newTricentisClientPath.trim().isEmpty()) {
            this.tricentisClientPath = EMPTY_STRING;
        } else {
            this.tricentisClientPath = fixPath(newTricentisClientPath);
        }
    }
    /**
     * Gets the configuration file path.
     *
     * @return the configuration file path
     */
    public String getConfigurationFilePath() {
        if (configurationFilePath == null) {
            return "$TRICENTIS_HOME\\ToscaCI\\Client\\Testconfig.xml";
        }
        return configurationFilePath;
    }
    /**
     * Sets the configuration file path.
     *
     * @param newConfigurationFilePath the new configuration file path
     */
    @DataBoundSetter
    public void
            setConfigurationFilePath(final String newConfigurationFilePath) {
        if (newConfigurationFilePath == null
                || newConfigurationFilePath.trim().isEmpty()) {
            this.configurationFilePath = EMPTY_STRING;
        } else {
            this.configurationFilePath = fixPath(newConfigurationFilePath);
        }
    }
    /**
     * Gets the test events.
     *
     * @return the test events
     */
    public String getTestEvents() {
        if (testEvents == null) {
            return EMPTY_STRING;
        }
        return testEvents;
    }
    /**
     * Sets the test events.
     *
     * @param newTestEvents the new test events
     */
    @DataBoundSetter
    public void setTestEvents(final String newTestEvents) {
        if (newTestEvents == null || newTestEvents.trim().isEmpty()) {
            this.testEvents = EMPTY_STRING;
        } else {
            this.testEvents = newTestEvents;
        }
    }
    /**
     * Gets the endpoint.
     *
     * @return the endpoint
     */
    public String getEndpoint() {
        if (endpoint == null) {
            return DEFAULT_ENDPOINT;
        }
        return endpoint;
    }
    /**
     * Sets the endpoint.
     *
     * @param newEndpoint the new endpoint
     */
    @DataBoundSetter
    public void setEndpoint(final String newEndpoint) {
        if (newEndpoint == null || newEndpoint.trim().isEmpty()) {
            this.endpoint = EMPTY_STRING;
        } else {
            this.endpoint = Util.fixEmptyAndTrim(newEndpoint);
        }
    }
    /**
     * Gets the results file.
     *
     * @return the results file
     */
    public String getResultsFile() {
        return RESULTS_FILE_NAME;
    }
    /**
     * Sets the proc starter factory.
     *
     * @param newProcStarterFactory the new proc starter factory
     */
    void setProcStarterFactory(final ProcStarterFactory newProcStarterFactory) {
        this.procStarterFactory = newProcStarterFactory;
    }
    /**
     * Gets the proc starter factory.
     *
     * @return the proc starter factory
     */
    ProcStarterFactory getProcStarterFactory() {
        if (procStarterFactory == null) {
            procStarterFactory = new DefaultProcStarterFactory();
        }
        return procStarterFactory;
    }
    /**
     * Sets the results publisher.
     *
     * @param newResultsPublisher the new results publisher
     */
    void setResultsPublisher(final JUnitResultsPublisher newResultsPublisher) {
        this.resultsPublisher = newResultsPublisher;
    }
    /**
     * Gets the results publisher.
     *
     * @return the results publisher
     */
    JUnitResultsPublisher getResultsPublisher() {
        if (resultsPublisher == null) {
            resultsPublisher = new DefaultJUnitResultsPublisher();
        }
        return resultsPublisher;
    }
    /**
     * Sets the process executor.
     *
     * @param newExecutor the new process executor
     */
    void setProcessExecutor(final ProcessExecutor newExecutor) {
        this.executor = newExecutor;
    }
    /**
     * Gets the process executor.
     *
     * @return the process executor
     */
    ProcessExecutor getProcessExecutor() {
        if (executor == null) {
            executor = new DefaultProcessExecutor();
        }
        return executor;
    }
    /**
     * Log parameters.
     *
     * @param logger the logger
     */
    private void logParameters(final PrintStream logger) {
        logger.println(Messages.runJobLog(Messages.pluginTitle()));
        logger.println(Messages.tricentisClientPath() + ": "
                + getTricentisClientPath());
        logger.println(Messages.endpoint() + ": " + getEndpoint());
        logger.println(Messages.configurationFilePath() + ": "
                + getConfigurationFilePath());
        logger.println(Messages.resultsFile() + ": " + getResultsFile());
        logger.println(Messages.testEvents() + ": " + getTestEvents());
    }
    /**
     * Assert parameters.
     */
    private void assertParameters() {
        assertParameter(Messages.tricentisClientPath(), tricentisClientPath);
        assertParameter(Messages.endpoint(), endpoint);
    }
    /**
     * Assert parameter.
     *
     * @param name  the name
     * @param value the value
     */
    private void assertParameter(final String name, final String value) {
        if (value == null) {
            throw new IllegalArgumentException(
                    Messages.parametersNullError(name));
        }
    }
    /**
     * Fix path.
     *
     * @param path the path
     * @return the string
     */
    private String fixPath(final String path) {
        return StringUtils.removeEnd(StringUtils.removeStart(path.trim(), "\""),
                "\"");
    }

    /**
     * Descriptor for {@link TricentisCiBuilder}.
     *
     * @author Sergey Oplavin
     */
    @Symbol("tricentisCI")
    @Extension
    public static class Descriptor extends BuildStepDescriptor<Builder> {
        /**
         * Checks if is applicable.
         *
         * @param jobType the job type
         * @return true, if is applicable
         */
        @SuppressWarnings("rawtypes")
        @Override
        public boolean
                isApplicable(final Class<? extends AbstractProject> jobType) {
            return true;
        }
        /**
         * Gets the display name.
         *
         * @return the display name
         */
        @Override
        public String getDisplayName() {
            return Messages.pluginTitle();
        }
        /**
         * Do check tricentis client path.
         *
         * @param project             the project
         * @param tricentisClientPath the tricentis client path
         * @return the form validation
         * @throws IOException      Signals that an I/O exception has occurred.
         * @throws ServletException the servlet exception
         */
        public FormValidation doCheckTricentisClientPath(
                @AncestorInPath final AbstractProject<?, ?> project,
                @QueryParameter final String tricentisClientPath)
                throws IOException, ServletException {
            project.checkPermission(Job.CONFIGURE);
            if (!isStringValid(tricentisClientPath)) {
                return FormValidation.error(Messages.required());
            }
            if (!fileExists(tricentisClientPath)) {
                return FormValidation.error(Messages.fileNotFound());
            }
            return FormValidation.ok();
        }
        /**
         * Do check endpoint.
         *
         * @param project  the project
         * @param endpoint the endpoint
         * @return the form validation
         * @throws IOException      Signals that an I/O exception has occurred.
         * @throws ServletException the servlet exception
         */
        public FormValidation doCheckEndpoint(
                @AncestorInPath final AbstractProject<?, ?> project,
                @QueryParameter final String endpoint)
                throws IOException, ServletException {
            project.checkPermission(Job.CONFIGURE);
            if (!isStringValid(endpoint)) {
                return FormValidation.error(Messages.required());
            }
            return FormValidation.ok();
        }
        /**
         * Do check test events.
         *
         * @param project               the project
         * @param testEvents            the test events
         * @param endpoint              the endpoint
         * @param configurationFilePath the configuration file path
         * @return the form validation
         * @throws IOException      Signals that an I/O exception has occurred.
         * @throws ServletException the servlet exception
         */
        public FormValidation doCheckTestEvents(
                @AncestorInPath final AbstractProject<?, ?> project,
                @QueryParameter final String testEvents,
                @QueryParameter final String endpoint,
                @QueryParameter final String configurationFilePath)
                throws IOException, ServletException {
            project.checkPermission(Job.CONFIGURE);
            String oneFieldString = validateOnlyOneField(testEvents,
                    configurationFilePath, endpoint);
            if (oneFieldString != null) {
                return FormValidation.error(oneFieldString);
            }
            if (isStringValid(testEvents) && !isDex(endpoint)) {
                return FormValidation.error(Messages.dexOnly());
            }
            return FormValidation.ok();
        }
        /**
         * Do check configuration file path.
         *
         * @param project               the project
         * @param configurationFilePath the configuration file path
         * @param testEvents            the test events
         * @param endpoint              the endpoint
         * @return the form validation
         * @throws IOException      Signals that an I/O exception has occurred.
         * @throws ServletException the servlet exception
         */
        public FormValidation doCheckConfigurationFilePath(
                @AncestorInPath final AbstractProject<?, ?> project,
                @QueryParameter final String configurationFilePath,
                @QueryParameter final String testEvents,
                @QueryParameter final String endpoint)
                throws IOException, ServletException {
            project.checkPermission(Job.CONFIGURE);
            String oneFieldString = validateOnlyOneField(testEvents,
                    configurationFilePath, endpoint);
            if (oneFieldString != null) {
                return FormValidation.error(oneFieldString);
            }
            if (isStringValid(configurationFilePath)
                    && !fileExists(configurationFilePath)) {
                return FormValidation.error(Messages.fileNotFound());
            }
            return FormValidation.ok();
        }
        /**
         * Validate only one field.
         *
         * @param val1     the val 1
         * @param val2     the val 2
         * @param endpoint the endpoint
         * @return the string
         */
        private String validateOnlyOneField(final String val1,
                final String val2, final String endpoint) {
            if (isDex(endpoint) && (val1.isEmpty() == val2.isEmpty())) {
                return Messages.onlyOne();
            }
            return null;
        }
        /**
         * File exists.
         *
         * @param pathVal the path
         * @return true, if successful
         */
        private boolean fileExists(final String pathVal) {
            String path = pathVal;
            if (path.startsWith("$")) {
                String newPathString = path.replace('\\', '/');
                int firstSeparator = newPathString.indexOf('/');
                if (firstSeparator == -1) {
                    path = System.getenv(path.substring(1));
                } else {
                    String expEnvVar = StringUtils.stripEnd(
                            System.getenv(path.substring(1, firstSeparator)),
                            "\\/");
                    path = expEnvVar + path.substring(firstSeparator);
                }
            }
            File file = new File(path.replace('\\', File.separatorChar)
                    .replace('/', File.separatorChar));
            return file.exists() && file.isFile();
        }
        /**
         * Checks if is string valid.
         *
         * @param value the value
         * @return true, if is string valid
         */
        private boolean isStringValid(final String value) {
            return value != null && !value.trim().isEmpty();
        }
        /**
         * Checks if is dex.
         *
         * @param endpoint the endpoint
         * @return true, if is dex
         */
        private boolean isDex(final String endpoint) {
            return endpoint.toLowerCase().contains("managerservice.svc");
        }
    }
}
