/*
 **/
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
 */
public class TricentisCiBuilderTest {
    /** The Constant EMPTY_STRING. */
    private static final String EMPTY_STRING = "";
    /** The Constant CONF_DEFAULT. */
    static final String CONF_DEFAULT
            = "$TRICENTIS_HOME\\ToscaCI\\Client\\Testconfig.xml";
    /** The expected. */
    @Rule
    public ExpectedException expected = ExpectedException.none();

    /**
     * Test valid case.
     *
     * @throws InterruptedException the interrupted exception
     * @throws IOException          Signals that an I/O exception has occurred.
     */
    @Test
    public void testValidCase() throws InterruptedException, IOException {
        final TricentisCiBuilder builder = new TricentisCiBuilder("aa", "a");
        builder.setConfigurationFilePath("aaa");
        final ExecutionContext context = new ExecutionContext(builder, 0);
        builder.perform(context.run, context.workspace, context.launcher,
                context.listener);
        verify(context.starterFactory).create(builder, context.run,
                context.workspace, context.launcher, context.listener);
        verify(context.executor).execute(context.starter);
        verify(context.publisher).publish("results.xml", context.run,
                context.workspace, context.launcher, context.listener);
    }
    /**
     * Test positive execution code.
     *
     * @throws InterruptedException the interrupted exception
     * @throws IOException          Signals that an I/O exception has occurred.
     */
    @Test
    public void testPositiveExecutionCode()
            throws InterruptedException, IOException {
        final TricentisCiBuilder builder = new TricentisCiBuilder("aa", "a");
        testNonZeroExecutionCode(builder, 1);
    }
    /**
     * Test negative execution code.
     *
     * @throws InterruptedException the interrupted exception
     * @throws IOException          Signals that an I/O exception has occurred.
     */
    @Test
    public void testNegativeExecutionCode()
            throws InterruptedException, IOException {
        final TricentisCiBuilder builder = new TricentisCiBuilder("aa", "a");
        testNonZeroExecutionCode(builder, -1);
    }
    /**
     * Test exception occurred.
     *
     * @throws InterruptedException the interrupted exception
     * @throws IOException          Signals that an I/O exception has occurred.
     */
    @Test
    public void testExceptionOccurred()
            throws InterruptedException, IOException {
        expected.expect(RuntimeException.class);
        expected.expectMessage("Bad day");
        final TricentisCiBuilder builder = new TricentisCiBuilder("aa", "a");
        final ExecutionContext context = new ExecutionContext(builder, 0);
        context.executor = mock(ProcessExecutor.class);
        when(context.executor.execute(context.starter))
                .thenThrow(new RuntimeException("Bad day"));
        builder.setProcessExecutor(context.executor);
        builder.perform(context.run, context.workspace, context.launcher,
                context.listener);
    }
    /**
     * Test set tricentis path.
     */
    @Test
    public void testSetTricentisPath() {
        final String expected = "aaaa";
        final TricentisCiBuilder ciBuilder
                = new TricentisCiBuilder("\"" + expected + "\"", "endpoint");
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
        assertEquals(EMPTY_STRING, ciBuilder.getTricentisClientPath());
        ciBuilder.setTricentisClientPath("");
        assertEquals(EMPTY_STRING, ciBuilder.getTricentisClientPath());
        ciBuilder.setTricentisClientPath("        ");
        assertEquals(EMPTY_STRING, ciBuilder.getTricentisClientPath());
    }
    /**
     * Test set configuration file path.
     */
    @Test
    public void testSetConfigurationFilePath() {
        final String expected = "aaaa";
        final TricentisCiBuilder ciBuilder
                = new TricentisCiBuilder("something", "endpoint");
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
        assertEquals(EMPTY_STRING, ciBuilder.getConfigurationFilePath());
        ciBuilder.setConfigurationFilePath("");
        assertEquals(EMPTY_STRING, ciBuilder.getConfigurationFilePath());
        ciBuilder.setConfigurationFilePath("        ");
        assertEquals(EMPTY_STRING, ciBuilder.getConfigurationFilePath());
    }
    /**
     * Test descriptor is assignable.
     */
    @Test
    public void testDescriptorIsAssignable() {
        assertTrue(new TricentisCiBuilder.Descriptor()
                .isApplicable(AbstractProject.class));
    }
    /**
     * Test descriptor get display name.
     */
    @Test
    public void testDescriptorGetDisplayName() {
        assertEquals(Messages.pluginTitle(),
                new TricentisCiBuilder.Descriptor().getDisplayName());
    }
    /**
     * Test descriptor do check tricentis client path.
     *
     * @throws IOException      Signals that an I/O exception has occurred.
     * @throws ServletException the servlet exception
     */
    @Test
    public void testDescriptorDoCheckTricentisClientPath()
            throws IOException, ServletException {
        final AbstractProject<?, ?> project = mock(AbstractProject.class);
        final TricentisCiBuilder.Descriptor descriptor
                = new TricentisCiBuilder.Descriptor();
        assertEquals(FormValidation.error(Messages.fileNotFound()).toString(),
                descriptor.doCheckTricentisClientPath(project, "some path")
                        .toString());
        verify(project).checkPermission(Job.CONFIGURE);
    }
    /**
     * Test descriptor do check endpoint.
     *
     * @throws IOException      Signals that an I/O exception has occurred.
     * @throws ServletException the servlet exception
     */
    @Test
    public void testDescriptorDoCheckEndpoint()
            throws IOException, ServletException {
        final AbstractProject<?, ?> project = mock(AbstractProject.class);
        final TricentisCiBuilder.Descriptor descriptor
                = new TricentisCiBuilder.Descriptor();
        assertEquals(FormValidation.ok(),
                descriptor.doCheckEndpoint(project, "some path"));
        verify(project).checkPermission(Job.CONFIGURE);
        final FormValidation validation
                = descriptor.doCheckEndpoint(project, "");
        assertEquals(Messages.required(), validation.getMessage());
        assertEquals(FormValidation.Kind.ERROR, validation.kind);
        verify(project, times(2)).checkPermission(Job.CONFIGURE);
    }
    /**
     * Test non zero execution code.
     *
     * @param builder    the builder
     * @param returnCode the return code
     * @throws InterruptedException the interrupted exception
     * @throws IOException          Signals that an I/O exception has occurred.
     */
    private void testNonZeroExecutionCode(final TricentisCiBuilder builder,
            final int returnCode) throws InterruptedException, IOException {
        final ExecutionContext context
                = new ExecutionContext(builder, returnCode);
        try {
            builder.perform(context.run, context.workspace, context.launcher,
                    context.listener);
            fail("Should have been failed");
        } catch (final AbortException ex) {
            assertEquals(Messages.exitCodeNotZero(returnCode), ex.getMessage());
        }
        verify(context.starterFactory).create(builder, context.run,
                context.workspace, context.launcher, context.listener);
        verify(context.executor).execute(context.starter);
        verify(context.publisher).publish("results.xml", context.run,
                context.workspace, context.launcher, context.listener);
    }

    /**
     * The Class ExecutionContext.
     */
    private static class ExecutionContext {
        /** The run. */
        private final Run<?, ?> run;
        /** The workspace. */
        private final FilePath workspace;
        /** The listener. */
        private final TaskListener listener;
        /** The launcher. */
        private final Launcher launcher;
        /** The starter factory. */
        ProcStarterFactory starterFactory;
        /** The starter. */
        ProcStarter starter;
        /** The executor. */
        ProcessExecutor executor;
        /** The publisher. */
        JUnitResultsPublisher publisher;

        /**
         * Instantiates a new execution context.
         *
         * @param builder  the builder
         * @param exitCode the exit code
         * @throws IOException          Signals that an I/O exception has
         *                              occurred.
         * @throws InterruptedException the interrupted exception
         */
        public ExecutionContext(final TricentisCiBuilder builder,
                final int exitCode) throws IOException, InterruptedException {
            run = createRunMock();
            workspace = new FilePath(new File(""));
            final ByteArrayOutputStream out = new ByteArrayOutputStream();
            listener = new StreamBuildListener(out);
            launcher = mock(Launcher.class);
            starterFactory = mock(ProcStarterFactory.class);
            starter = launcher.new ProcStarter();
            starter.cmds("");
            executor = mock(ProcessExecutor.class);
            when(starterFactory.create(builder, run, workspace, launcher,
                    listener)).thenReturn(starter);
            when(executor.execute(starter)).thenReturn(exitCode);
            publisher = mock(JUnitResultsPublisher.class);
            builder.setProcStarterFactory(starterFactory);
            builder.setProcessExecutor(executor);
            builder.setResultsPublisher(publisher);
        }
        /**
         * Creates the run mock.
         *
         * @return the run
         * @throws IOException          Signals that an I/O exception has
         *                              occurred.
         * @throws InterruptedException the interrupted exception
         */
        private Run<?, ?> createRunMock()
                throws IOException, InterruptedException {
            final Run<?, ?> run = mock(Run.class);
            final EnvVars envVars = new EnvVars();
            when(run.getEnvironment(any(TaskListener.class)))
                    .thenReturn(envVars);
            return run;
        }
    }
}
