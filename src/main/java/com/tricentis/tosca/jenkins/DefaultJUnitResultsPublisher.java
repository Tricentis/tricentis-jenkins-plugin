/*
 *
 */
package com.tricentis.tosca.jenkins;

import java.io.IOException;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.junit.JUnitResultArchiver;

/**
 * Default implementation of {@link JUnitResultsPublisher} which redirect call
 * to {@link JUnitResultArchiver}.
 *
 * @author Sergey Oplavin
 */
public class DefaultJUnitResultsPublisher implements JUnitResultsPublisher {
    /**
     * Publish.
     *
     * @param reportFileName the report file name
     * @param run            the run
     * @param workspace      the workspace
     * @param launcher       the launcher
     * @param listener       the listener
     * @throws InterruptedException the interrupted exception
     * @throws IOException          Signals that an I/O exception has occurred.
     */
    @Override
    public void publish(final String reportFileName, final Run<?, ?> run,
            final FilePath workspace, final Launcher launcher,
            final TaskListener listener)
            throws InterruptedException, IOException {
        final JUnitResultArchiver archiver
                = new JUnitResultArchiver(reportFileName);
        archiver.perform(run, workspace, launcher, listener);
    }
}
