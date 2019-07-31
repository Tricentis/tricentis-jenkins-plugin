package com.tricentis.tosca.jenkins;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;

import javax.servlet.ServletException;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import hudson.AbortException;
import hudson.EnvVars;
import hudson.FilePath;
import hudson.Launcher;
import hudson.Launcher.ProcStarter;
import hudson.model.AbstractProject;
import hudson.model.Job;
import hudson.model.Run;
import hudson.model.StreamBuildListener;
import hudson.model.TaskListener;
import hudson.util.FormValidation;

/**
 * Tests for {@link TricentisCiBuilder}.
 *
 * @author Sergey Oplavin
 *
 */
public class TricentisCiBuilderTest {

	static final String SPACE = " ";
	static final String CONF_DEFAULT = "$TRICENTIS_HOME\\ToscaCI\\Client\\Testconfig.xml";
	static final String TESTEVENTS_DEFAULT = "";
	static final String CLIENT_DEFAULT = "$TRICENTIS_HOME\\ToscaCI\\Client\\ToscaCIJavaClient.jar";
	static final String ENDPOINT_DEFAULT = "http://servername/DistributionServerService/ManagerService.svc";

	@Rule
	public ExpectedException expected = ExpectedException.none();

	@Test
	public void testValidCase() throws InterruptedException, IOException {
		final TricentisCiBuilder builder = new TricentisCiBuilder("aa", "a");
		builder.setConfigurationFilePath("aaa");
		final ExecutionContext context = new ExecutionContext(builder, 0);

		builder.perform(context.run, context.workspace, context.launcher, context.listener);

		verify(context.starterFactory).create(builder, context.run, context.workspace, context.launcher,
				context.listener);
		verify(context.executor).execute(context.starter);
		verify(context.publisher).publish("results.xml", context.run, context.workspace, context.launcher,
				context.listener);
	}

	@Test
	public void testPositiveExecutionCode() throws InterruptedException, IOException {
		final TricentisCiBuilder builder = new TricentisCiBuilder("aa", "a");
		testNonZeroExecutionCode(builder, 1);
	}

	@Test
	public void testNegativeExecutionCode() throws InterruptedException, IOException {
		final TricentisCiBuilder builder = new TricentisCiBuilder("aa", "a");
		testNonZeroExecutionCode(builder, -1);
	}

	@Test
	public void testExceptionOccurred() throws InterruptedException, IOException {
		expected.expect(RuntimeException.class);
		expected.expectMessage("Bad day");
		final TricentisCiBuilder builder = new TricentisCiBuilder("aa", "a");
		final ExecutionContext context = new ExecutionContext(builder, 0);
		context.executor = mock(ProcessExecutor.class);
		when(context.executor.execute(context.starter)).thenThrow(new RuntimeException("Bad day"));
		builder.setProcessExecutor(context.executor);

		builder.perform(context.run, context.workspace, context.launcher, context.listener);
	}

	@Test
	public void testSetTricentisPath() {
		final String expected = "aaaa";
		final TricentisCiBuilder ciBuilder = new TricentisCiBuilder("\"" + expected + "\"", "endpoint");
		assertEquals(expected, ciBuilder.getTricentisClientPath());

		ciBuilder.setTricentisClientPath(expected);
		assertEquals(expected, ciBuilder.getTricentisClientPath());

		ciBuilder.setTricentisClientPath("\"" + expected + "\"");
		assertEquals(expected, ciBuilder.getTricentisClientPath());

		ciBuilder.setTricentisClientPath("\"" + expected);
		assertEquals(expected, ciBuilder.getTricentisClientPath());

		ciBuilder.setTricentisClientPath(expected + "\"");
		assertEquals(expected, ciBuilder.getTricentisClientPath());

		ciBuilder.setTricentisClientPath(null);
		assertEquals(SPACE, ciBuilder.getTricentisClientPath());

		ciBuilder.setTricentisClientPath("");
		assertEquals(SPACE, ciBuilder.getTricentisClientPath());

		ciBuilder.setTricentisClientPath("        ");
		assertEquals(SPACE, ciBuilder.getTricentisClientPath());
	}

	@Test
	public void testSetConfigurationFilePath() {
		final String expected = "aaaa";
		final TricentisCiBuilder ciBuilder = new TricentisCiBuilder("something", "endpoint");
		assertEquals(CONF_DEFAULT, ciBuilder.getConfigurationFilePath());

		ciBuilder.setConfigurationFilePath(expected);
		assertEquals(expected, ciBuilder.getConfigurationFilePath());

		ciBuilder.setConfigurationFilePath("\"" + expected + "\"");
		assertEquals(expected, ciBuilder.getConfigurationFilePath());

		ciBuilder.setConfigurationFilePath("\"" + expected);
		assertEquals(expected, ciBuilder.getConfigurationFilePath());

		ciBuilder.setConfigurationFilePath(expected + "\"");
		assertEquals(expected, ciBuilder.getConfigurationFilePath());

		ciBuilder.setConfigurationFilePath(null);
		assertEquals(SPACE, ciBuilder.getConfigurationFilePath());

		ciBuilder.setConfigurationFilePath("");
		assertEquals(SPACE, ciBuilder.getConfigurationFilePath());

		ciBuilder.setConfigurationFilePath("        ");
		assertEquals(SPACE, ciBuilder.getConfigurationFilePath());
	}

	@Test
	public void testDescriptorIsAssignable() {
		assertTrue(new TricentisCiBuilder.Descriptor().isApplicable(AbstractProject.class));
	}

	@Test
	public void testDescriptorGetDisplayName() {
		assertEquals(Messages.pluginTitle(), new TricentisCiBuilder.Descriptor().getDisplayName());
	}

	@Test
	public void testDescriptorDoCheckTricentisClientPath() throws IOException, ServletException {
		final AbstractProject<?, ?> project = mock(AbstractProject.class);
		final TricentisCiBuilder.Descriptor descriptor = new TricentisCiBuilder.Descriptor();
		assertEquals(FormValidation.ok(), descriptor.doCheckTricentisClientPath(project, "some path"));
		verify(project).checkPermission(Job.CONFIGURE);
		final FormValidation validation = descriptor.doCheckTricentisClientPath(project, "");
		assertEquals(Messages.required(), validation.getMessage());
		assertEquals(FormValidation.Kind.ERROR, validation.kind);
		verify(project, times(2)).checkPermission(Job.CONFIGURE);
	}

	@Test
	public void testDescriptorDoCheckEndpoint() throws IOException, ServletException {
		final AbstractProject<?, ?> project = mock(AbstractProject.class);
		final TricentisCiBuilder.Descriptor descriptor = new TricentisCiBuilder.Descriptor();
		assertEquals(FormValidation.ok(), descriptor.doCheckEndpoint(project, "some path"));
		verify(project).checkPermission(Job.CONFIGURE);
		final FormValidation validation = descriptor.doCheckEndpoint(project, "");
		assertEquals(Messages.required(), validation.getMessage());
		assertEquals(FormValidation.Kind.ERROR, validation.kind);
		verify(project, times(2)).checkPermission(Job.CONFIGURE);
	}

	private void testNonZeroExecutionCode(final TricentisCiBuilder builder, final int returnCode)
			throws InterruptedException, IOException {
		final ExecutionContext context = new ExecutionContext(builder, returnCode);
		try {
			builder.perform(context.run, context.workspace, context.launcher, context.listener);
			fail("Should have been failed");
		} catch (final AbortException ex) {
			assertEquals(Messages.exitCodeNotZero(returnCode), ex.getMessage());
		}

		verify(context.starterFactory).create(builder, context.run, context.workspace, context.launcher,
				context.listener);
		verify(context.executor).execute(context.starter);
		verify(context.publisher).publish("results.xml", context.run, context.workspace, context.launcher,
				context.listener);
	}

	private static class ExecutionContext {
		private final Run<?, ?> run;
		private final FilePath workspace;
		private final TaskListener listener;
		private final Launcher launcher;
		ProcStarterFactory starterFactory;
		ProcStarter starter;
		ProcessExecutor executor;
		JUnitResultsPublisher publisher;

		public ExecutionContext(final TricentisCiBuilder builder, final int exitCode)
				throws IOException, InterruptedException {
			run = createRunMock();
			workspace = new FilePath(new File(""));
			final ByteArrayOutputStream out = new ByteArrayOutputStream();
			listener = new StreamBuildListener(out);
			launcher = mock(Launcher.class);
			starterFactory = mock(ProcStarterFactory.class);
			starter = launcher.new ProcStarter();
			starter.cmds("");
			executor = mock(ProcessExecutor.class);
			when(starterFactory.create(builder, run, workspace, launcher, listener)).thenReturn(starter);
			when(executor.execute(starter)).thenReturn(exitCode);
			publisher = mock(JUnitResultsPublisher.class);

			builder.setProcStarterFactory(starterFactory);
			builder.setProcessExecutor(executor);
			builder.setResultsPublisher(publisher);
		}

		private Run<?, ?> createRunMock() throws IOException, InterruptedException {
			final Run<?, ?> run = mock(Run.class);
			final EnvVars envVars = new EnvVars();
			when(run.getEnvironment(any(TaskListener.class))).thenReturn(envVars);
			return run;
		}

	}

}
