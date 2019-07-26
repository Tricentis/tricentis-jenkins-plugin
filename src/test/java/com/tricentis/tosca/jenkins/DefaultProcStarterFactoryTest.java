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
import hudson.model.Run;
import hudson.model.StreamBuildListener;
import hudson.model.TaskListener;

/**
 * Tests for {@link DefaultProcStarterFactory}.
 *
 * @author Sergey Oplavin
 *
 */
public class DefaultProcStarterFactoryTest {
	private static final String TRICENTIS_HOME = "TRICENTIS_HOME";
	private static final String TRICENTIS_HOME_VALUE = "C:\\Program Files\\Tricentis\\Tosca Testsuite";
	private static final String VAR = "VAR";
	private static final String VAR_VALUE = "foo";
	private static final String RESULTS = "results.xml";

	@Rule
	public ExpectedException expected = ExpectedException.none();

	@Test
	public void testCreateWithJarRunner() throws InterruptedException, IOException {
		final String jarName = "/tosca.jar";
		final String conf = "conf/";
		final String endpoint = "/endpoint";
		final String toscaRunnerPath = "$" + TRICENTIS_HOME + jarName;
		final String confPath = conf + "$" + VAR;
		final String testEvents = "";
		final String endpointValue = "$" + VAR + endpoint;
		final TricentisCiBuilder runner = new TricentisCiBuilder(toscaRunnerPath, endpointValue);
		runner.setConfigurationFilePath(confPath);
		runner.setTestEvents(testEvents);
		final EnvVars envVars = createEnvVars();
		envVars.put("JAVA_HOME", "javahome");
		final FilePath workspace = new FilePath(new File(""));
		final ProcStarter starter = createStarter(runner, envVars, workspace);

		final List<String> expectedCmd = createExpectedCmd("javahome" + File.separator + "bin" + File.separator + "java", TRICENTIS_HOME_VALUE + jarName,
				conf + VAR_VALUE, VAR_VALUE + endpoint, workspace);

		assertEquals(expectedCmd, starter.cmds());
		assertArrayEquals(toArray(envVars), starter.envs());
		assertEquals(workspace, starter.pwd());
	}

	@Test
	public void testCreateWithJarRunnerWithNoJavaHome() throws InterruptedException, IOException {
		expected.expect(RuntimeException.class);
		expected.expectMessage(Messages.setJavaHome());
		final TricentisCiBuilder runner = new TricentisCiBuilder("ToscaCIJavaClient.jar", null);
		final EnvVars envVars = createEnvVars();
		final FilePath workspace = new FilePath(new File(""));
		createStarter(runner, envVars, workspace);
	}

	@Test
	public void testCreateWithExeRunner() throws InterruptedException, IOException {
		final String toscaRunnerPath = "ToscaCIClient.exe";
		final String confPath = "conf";
		final String testEvents = "";
		final String endpoint = "endpoint";
		final FilePath workspace = new FilePath(new File(""));
		final TricentisCiBuilder runner = new TricentisCiBuilder(toscaRunnerPath, endpoint);
		runner.setConfigurationFilePath(confPath);
		runner.setTestEvents(testEvents);
		final ProcStarter starter = createStarter(runner, createEnvVars(), workspace);

		assertEquals(createExpectedCmd(null, toscaRunnerPath, confPath, endpoint, workspace), starter.cmds());
	}

	@Test
	public void createWithNoConfigPath() throws InterruptedException, IOException {
		final String toscaRunnerPath = "ToscaCIClient.exe";
		final String confPath = null;
		final String endpoint = "endpoint";
		final FilePath workspace = new FilePath(new File(""));
		final TricentisCiBuilder runner = new TricentisCiBuilder(toscaRunnerPath, endpoint);
		runner.setConfigurationFilePath(confPath);
		final ProcStarter starter = createStarter(runner, createEnvVars(), workspace);

		assertEquals(createExpectedCmd(null, toscaRunnerPath, confPath, endpoint, workspace), starter.cmds());
	}

	@Test
	public void createWithNoEndpoint() throws InterruptedException, IOException {
		final String toscaRunnerPath = "ToscaCIClient.exe";
		final String confPath = null;
		final String endpoint = null;
		final FilePath workspace = new FilePath(new File(""));
		final TricentisCiBuilder runner = new TricentisCiBuilder(toscaRunnerPath, endpoint);
		runner.setConfigurationFilePath(confPath);
		final ProcStarter starter = createStarter(runner, createEnvVars(), workspace);

		assertEquals(createExpectedCmd(null, toscaRunnerPath, confPath, endpoint, workspace), starter.cmds());
	}

	@Test
	public void createWithNoEndpointAndNoConfig() throws InterruptedException, IOException {
		final String toscaRunnerPath = "ToscaCIClient.exe";
		final String confPath = null;
		final String endpoint = null;
		final FilePath workspace = new FilePath(new File(""));
		final TricentisCiBuilder runner = new TricentisCiBuilder(toscaRunnerPath, endpoint);
		runner.setConfigurationFilePath(confPath);
		final ProcStarter starter = createStarter(runner, createEnvVars(), workspace);

		assertEquals(createExpectedCmd(null, toscaRunnerPath, confPath, endpoint, workspace), starter.cmds());
	}

	private List<String> createExpectedCmd(final String application, final String toscaClient, final String conf, final String endpoint,
			final FilePath workspace) {
		final List<String> cmds = new ArrayList<>();
		if (application != null) {
			cmds.add(application);
			cmds.add("-jar");
		}
		cmds.addAll(
				Arrays.asList(toscaClient, "-m", "distributed", "-t", "junit", "-x", "True", "-r", workspace.child(RESULTS).getRemote()));
		if (conf != null) {
			cmds.add("-c");
			cmds.add(conf);
		}
		if (endpoint != null) {
			cmds.add("-e");
			cmds.add(endpoint);
		}
		return cmds;
	}

	private ProcStarter createStarter(final TricentisCiBuilder runner, final EnvVars envVars, final FilePath workspace)
			throws InterruptedException, IOException {
		final Run<?, ?> run = mock(Run.class);
		final DefaultProcStarterFactory factory = new DefaultProcStarterFactory();
		when(run.getEnvironment(any(TaskListener.class))).thenReturn(envVars);
		final ByteArrayOutputStream out = new ByteArrayOutputStream();
		final TaskListener listener = new StreamBuildListener(out);
		return factory.create(runner, run, workspace, new DummyLauncher(listener), listener);
	}

	private String[] toArray(final EnvVars envVars) {
		final String[] array = new String[envVars.size()];
		int i = 0;
		for (final Entry<String, String> entry : envVars.entrySet()) {
			array[i] = entry.getKey() + "=" + entry.getValue();
			i++;
		}
		return array;
	}

	private EnvVars createEnvVars() {
		final EnvVars envVars = new EnvVars();
		envVars.put(TRICENTIS_HOME, TRICENTIS_HOME_VALUE);
		envVars.put(VAR, VAR_VALUE);
		return envVars;
	}

}
