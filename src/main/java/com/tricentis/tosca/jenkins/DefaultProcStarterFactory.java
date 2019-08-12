/*
 **/
package com.tricentis.tosca.jenkins;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import org.apache.commons.lang.StringUtils;
import hudson.EnvVars;
import hudson.FilePath;
import hudson.Launcher;
import hudson.Launcher.ProcStarter;
import hudson.model.Executor;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.util.ArgumentListBuilder;

/**
 * Default implementation for {@link ProcStarterFactory}.
 *
 * @author Sergey Oplavin
 */
public class DefaultProcStarterFactory implements ProcStarterFactory {
    /** The Constant MODE_SWITCH. */
    private static final String MODE_SWITCH = "-m";
    /** The Constant RESULTS_SWITCH. */
    private static final String RESULTS_SWITCH = "-r";
    /** The Constant CONFIG_SWITCH. */
    private static final String CONFIG_SWITCH = "-c";
    /** The Constant ENDPOINT_SWITCH. */
    private static final String ENDPOINT_SWITCH = "-e";
    /** The Constant REPORT_TYPE_SWITH. */
    private static final String REPORT_TYPE_SWITH = "-t";
    /** The Constant SPEC_EXIT_CODE_SWITCH. */
    private static final String SPEC_EXIT_CODE_SWITCH = "-x";
    /** The Constant DEFAULT_MODE. */
    private static final String DEFAULT_MODE = "distributed";
    /** The Constant JUNIT_REPORT_TYPE. */
    private static final String JUNIT_REPORT_TYPE = "junit";
    /** The Constant SPEC_EXIT_CODE_VALUE. */
    private static final String SPEC_EXIT_CODE_VALUE = "True";
    /** The Constant JAVA_HOME. */
    private static final String JAVA_HOME = "JAVA_HOME";
    /** The Constant CONFIG_FILE_PATH. */
    private static final String CONFIG_FILE_PATH
            = "/jenkins-tricentis/config.xml";
    /** The Constant CONFIG_FILE_START. */
    private static final String CONFIG_FILE_START
            = "<?xml version=\"1.0\" encoding=\"utf-16\" ?>\n" + "\n"
                    + "<testConfiguration>\n" + "\n" + "   <TestEvents>";
    /** The Constant CONFIG_FILE_END. */
    private static final String CONFIG_FILE_END
            = "\n" + "    </TestEvents>\n" + "\n" + "</testConfiguration>";
    /** The Constant TEST_EVENT_START. */
    private static final String TEST_EVENT_START = "\n" + "        <TestEvent>";
    /** The Constant TEST_EVENT_END. */
    private static final String TEST_EVENT_END = "</TestEvent>";

    /**
     * Creates the.
     *
     * @param runner    the runner
     * @param run       the run
     * @param workspace the workspace
     * @param launcher  the launcher
     * @param listener  the listener
     * @return the proc starter
     * @throws InterruptedException the interrupted exception
     * @throws IOException          Signals that an I/O exception has occurred.
     */
    @Override
    public ProcStarter create(final TricentisCiBuilder runner,
            final Run<?, ?> run, final FilePath workspace,
            final Launcher launcher, final TaskListener listener)
            throws InterruptedException, IOException {
        String clientPath = runner.getTricentisClientPath();
        final EnvVars vars = run.getEnvironment(listener);
        final String configPath = runner.getConfigurationFilePath();
        final String testEvents = runner.getTestEvents();
        final String endpoint = runner.getEndpoint();
        final String application;
        final PrintStream logger = listener.getLogger();
        final Executor executor = run.getExecutor();
        if (executor == null) {
            logger.println("Tricentis-CI Plugin: Executer is null");
            throw new InterruptedException("Executer is null");
        }
        if (!isStringValid(clientPath)) {
            executor.interrupt(Result.FAILURE);
        }
        clientPath = vars.expand(clientPath);
        if (clientPath.toLowerCase().endsWith(".jar")) {
            final String javaHome = vars.get(JAVA_HOME);
            if (StringUtils.isEmpty(javaHome)) {
                throw new RuntimeException(Messages.setJavaHome());
            }
            application = new FilePath(launcher.getChannel(), javaHome)
                    .child("bin").child("java").getRemote();
        } else {
            application = null;
        }
        final ArgumentListBuilder builder = new ArgumentListBuilder();
        if (application != null) {
            builder.add(application);
            builder.add("-jar");
        }
        builder.add(clientPath, MODE_SWITCH, DEFAULT_MODE, REPORT_TYPE_SWITH,
                JUNIT_REPORT_TYPE, SPEC_EXIT_CODE_SWITCH, SPEC_EXIT_CODE_VALUE,
                RESULTS_SWITCH,
                workspace.child(vars.expand(runner.getResultsFile()))
                        .getRemote());
        if (isStringValid(testEvents)) {
            if (isStringValid(configPath)) {
                logger.println(
                        "Tricentis-CI Plugin: TestEvents and ConfigPath set");
                executor.interrupt(Result.FAILURE);
            }
            String path = buildXmlFile(testEvents.split(";"), workspace,
                    executor, logger);
            if (isStringValid(path)) {
                builder.add(CONFIG_SWITCH, path);
            }
        } else if (isStringValid(configPath)) {
            builder.add(CONFIG_SWITCH, vars.expand(configPath));
        }
        if (isStringValid(endpoint)) {
            builder.add(ENDPOINT_SWITCH, vars.expand(runner.getEndpoint()));
        } else {
            logger.println("Tricentis-CI Plugin: Endpoint not set");
            executor.interrupt(Result.FAILURE);
        }
        return launcher.launch().cmds(builder).stdout(listener).envs(vars)
                .pwd(workspace);
    }
    /**
     * Builds the xml file.
     *
     * @param testEvents the test events
     * @param workspace  the workspace
     * @param executor   the executor
     * @param logger     the logger
     * @return the string
     */
    private String buildXmlFile(final String[] testEvents,
            final FilePath workspace, final Executor executor,
            final PrintStream logger) {
        File file;
        try {
            file = new File(new File(workspace.toURI()).getAbsolutePath()
                    + "\\temp-jenkins-tricentis.xml");
            boolean isNewlyCreated = file.createNewFile();
        } catch (IOException e1) {
            logger.println("Tricentis-CI Plugin: " + e1.getMessage());
            executor.interrupt(Result.FAILURE);
            return null;
        } catch (InterruptedException e1) {
            logger.println("Tricentis-CI Plugin: " + e1.getMessage());
            executor.interrupt(Result.FAILURE);
            return null;
        }
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(
                new FileOutputStream(file), StandardCharsets.UTF_8))) {
            writer.write(CONFIG_FILE_START);
            for (String testEvent : testEvents) {
                writer.append(TEST_EVENT_START);
                writer.append(testEvent);
                writer.append(TEST_EVENT_END);
            }
            writer.append(CONFIG_FILE_END);
        } catch (IOException e) {
            logger.println("Tricentis-CI Plugin: (Filewriter IOException)");
            logger.println("Tricentis-CI Plugin: " + e.getMessage());
            executor.interrupt(Result.FAILURE);
            return null;
        }
        return file.getAbsolutePath().replace('\\', '/');
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
}
