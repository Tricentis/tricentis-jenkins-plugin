/*
 *
 */
package com.tricentis.tosca.jenkins;

import java.io.IOException;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Run;
import hudson.model.TaskListener;

/**
 * Publishes JUnit results.
 *
 * @author Sergey Oplavin
 */
public interface JUnitResultsPublisher {
    /**
     * Publish results.
     *
     * @param reportFileName name of the file containing JUnit report.
     * @param run            run.
     * @param workspace      workspace where report file is located.
     * @param launcher       launcher.
     * @param listener       listener.
     * @throws InterruptedException the interrupted exception
     * @throws IOException          Signals that an I/O exception has occurred.
     */
    void publish(String reportFileName, Run<?, ?> run, FilePath workspace,
            Launcher launcher, TaskListener listener)
            throws InterruptedException, IOException;
}
