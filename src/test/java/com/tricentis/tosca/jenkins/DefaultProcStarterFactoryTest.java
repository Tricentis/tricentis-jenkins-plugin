/*
 *
 */
package com.tricentis.tosca.jenkins;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import hudson.EnvVars;
import hudson.FilePath;
import hudson.Launcher.DummyLauncher;
import hudson.Launcher.ProcStarter;
import hudson.model.Executor;
import hudson.model.Run;
import hudson.model.StreamBuildListener;
import hudson.model.TaskListener;

/**
 * Tests for {@link DefaultProcStarterFactory}.
 *
 * @author Sergey Oplavin
 */
public class DefaultProcStarterFactoryTest {
    /** The Constant TRICENTIS_HOME. */
    private static final String TRICENTIS_HOME = "TRICENTIS_HOME";
    /** The Constant TRICENTIS_HOME_VALUE. */
    private static final String TRICENTIS_HOME_VALUE
            = "C:\\Program Files\\Tricentis\\Tosca Testsuite";
    /** The Constant VAR. */
    private static final String VAR = "VAR";
    /** The Constant VAR_VALUE. */
    private static final String VAR_VALUE = "foo";
    /** The Constant RESULTS. */
    private static final String RESULTS = "results.xml";
    /** The expected. */
    @Rule
    public ExpectedException expected = ExpectedException.none();

    /**
     * Test create with jar runner.
     *
     * @throws InterruptedException the interrupted exception
     * @throws IOException          Signals that an I/O exception has occurred.
     */
    @Test
    public void testCreateWithJarRunner()
            throws InterruptedException, IOException {
        final String jarName = "/tosca.jar";
        final String conf = "conf/";
        final String endpoint = "/endpoint";
        final String toscaRunnerPath = "$" + TRICENTIS_HOME + jarName;
        final String confPath = conf + "$" + VAR;
        final String testEvents = "";
        final String endpointValue = "$" + VAR + endpoint;
        final TricentisCiBuilder runner
                = new TricentisCiBuilder(toscaRunnerPath, endpointValue);
        runner.setConfigurationFilePath(confPath);
        runner.setTestEvents(testEvents);
        final EnvVars envVars = createEnvVars();
        envVars.put("JAVA_HOME", "javahome");
        final FilePath workspace = new FilePath(new File(""));
        final ProcStarter starter = createStarter(runner, envVars, workspace);
        final List<String> expectedCmd = createExpectedCmd(
                "javahome" + File.separator + "bin" + File.separator + "java",
                TRICENTIS_HOME_VALUE + jarName, conf + VAR_VALUE, testEvents,
                VAR_VALUE + endpoint, workspace);
        assertEquals(expectedCmd, starter.cmds());
        assertArrayEquals(toArray(envVars), starter.envs());
        assertEquals(workspace, starter.pwd());
    }
    /**
     * Test create with jar runner with no java home.
     *
     * @throws InterruptedException the interrupted exception
     * @throws IOException          Signals that an I/O exception has occurred.
     */
    @Test
    public void testCreateWithJarRunnerWithNoJavaHome()
            throws InterruptedException, IOException {
        expected.expect(RuntimeException.class);
        expected.expectMessage(Messages.setJavaHome());
        final TricentisCiBuilder runner
                = new TricentisCiBuilder("ToscaCIJavaClient.jar", null);
        final EnvVars envVars = createEnvVars();
        final FilePath workspace = new FilePath(new File(""));
        createStarter(runner, envVars, workspace);
    }
    /**
     * Test create with exe runner.
     *
     * @throws InterruptedException the interrupted exception
     * @throws IOException          Signals that an I/O exception has occurred.
     */
    @Test
    public void testCreateWithExeRunner()
            throws InterruptedException, IOException {
        final String toscaRunnerPath = "ToscaCIClient.exe";
        final String confPath = "conf";
        final String testEvents = "";
        final String endpoint = "endpoint";
        final FilePath workspace = new FilePath(new File(""));
        final TricentisCiBuilder runner
                = new TricentisCiBuilder(toscaRunnerPath, endpoint);
        runner.setConfigurationFilePath(confPath);
        runner.setTestEvents(testEvents);
        final ProcStarter starter
                = createStarter(runner, createEnvVars(), workspace);
        assertEquals(createExpectedCmd(null, toscaRunnerPath, confPath,
                testEvents, endpoint, workspace), starter.cmds());
    }
    /**
     * Creates the with no config path.
     *
     * @throws InterruptedException the interrupted exception
     * @throws IOException          Signals that an I/O exception has occurred.
     */
    @Test
    public void createWithNoConfigPath()
            throws InterruptedException, IOException {
        final String toscaRunnerPath = "ToscaCIClient.exe";
        final String confPath = null;
        final String testEvents = null;
        final String endpoint = "endpoint";
        final FilePath workspace = new FilePath(new File(""));
        final TricentisCiBuilder runner
                = new TricentisCiBuilder(toscaRunnerPath, endpoint);
        runner.setConfigurationFilePath(confPath);
        final ProcStarter starter
                = createStarter(runner, createEnvVars(), workspace);
        assertEquals(createExpectedCmd(null, toscaRunnerPath, confPath,
                testEvents, endpoint, workspace), starter.cmds());
    }
    /**
     * Creates the with no endpoint and no config.
     *
     * @throws InterruptedException the interrupted exception
     * @throws IOException          Signals that an I/O exception has occurred.
     */
    @Test
    public void createWithNoEndpointAndNoConfig()
            throws InterruptedException, IOException {
        final String toscaRunnerPath = "ToscaCIClient.exe";
        final String confPath = null;
        final String testEvents = null;
        final String endpoint = null;
        final FilePath workspace = new FilePath(new File(""));
        final TricentisCiBuilder runner
                = new TricentisCiBuilder(toscaRunnerPath, endpoint);
        runner.setConfigurationFilePath(confPath);
        final ProcStarter starter
                = createStarter(runner, createEnvVars(), workspace);
        assertEquals(createExpectedCmd(null, toscaRunnerPath, confPath,
                testEvents, endpoint, workspace), starter.cmds());
    }
    /**
     * Creates the expected cmd.
     *
     * @param application the application
     * @param toscaClient the tosca client
     * @param conf        the conf
     * @param testeEvents the teste events
     * @param endpoint    the endpoint
     * @param workspace   the workspace
     * @return the list
     */
    private List<String> createExpectedCmd(final String application,
            final String toscaClient, final String conf,
            final String testeEvents, final String endpoint,
            final FilePath workspace) {
        final String defaultEndpoint
                = "http://servername/DistributionServerService/ManagerService.svc";
        final List<String> cmds = new ArrayList<>();
        if (application != null) {
            cmds.add(application);
            cmds.add("-jar");
        }
        cmds.addAll(Arrays.asList(toscaClient, "-m", "distributed", "-t",
                "junit", "-x", "True", "-r",
                workspace.child(RESULTS).getRemote()));
        if (conf != null && !conf.isEmpty()) {
            cmds.add("-c");
            cmds.add(conf);
        }
        if (endpoint != null && !endpoint.isEmpty()) {
            cmds.add("-e");
            cmds.add(endpoint);
        }
        return cmds;
    }
    /**
     * Creates the starter.
     *
     * @param runner    the runner
     * @param envVars   the env vars
     * @param workspace the workspace
     * @return the proc starter
     * @throws InterruptedException the interrupted exception
     * @throws IOException          Signals that an I/O exception has occurred.
     */
    private ProcStarter createStarter(final TricentisCiBuilder runner,
            final EnvVars envVars, final FilePath workspace)
            throws InterruptedException, IOException {
        final Run<?, ?> run = mock(Run.class);
        final DefaultProcStarterFactory factory
                = new DefaultProcStarterFactory();
        when(run.getEnvironment(any(TaskListener.class))).thenReturn(envVars);
        when(run.getExecutor()).thenReturn(mock(Executor.class));
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        final TaskListener listener = new StreamBuildListener(out);
        return factory.create(runner, run, workspace,
                new DummyLauncher(listener), listener);
    }
    /**
     * To array.
     *
     * @param envVars the env vars
     * @return the string[]
     */
    private String[] toArray(final EnvVars envVars) {
        final String[] array = new String[envVars.size()];
        int i = 0;
        for (final Entry<String, String> entry : envVars.entrySet()) {
            array[i] = entry.getKey() + "=" + entry.getValue();
            i++;
        }
        return array;
    }
    /**
     * Creates the env vars.
     *
     * @return the env vars
     */
    private EnvVars createEnvVars() {
        final EnvVars envVars = new EnvVars();
        envVars.put(TRICENTIS_HOME, TRICENTIS_HOME_VALUE);
        envVars.put(VAR, VAR_VALUE);
        return envVars;
    }
}
