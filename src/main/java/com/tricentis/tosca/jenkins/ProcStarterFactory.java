/*
 *
 */
package com.tricentis.tosca.jenkins;

import java.io.IOException;
import hudson.FilePath;
import hudson.Launcher;
import hudson.Launcher.ProcStarter;
import hudson.model.Run;
import hudson.model.TaskListener;

/**
 * Factory for producing {@link ProcStarter} objects to run Tosca client .exe or
 * jar file with arguments provided by runner.
 *
 * @author Sergey Oplavin
 */
public interface ProcStarterFactory {
    /**
     * Create a new, ready to run {@link ProcStarter} object instance.
     *
     * @param runner    runner
     * @param run       Jenkins run
     * @param workspace worksapce
     * @param launcher  launcher
     * @param listener  listener
     * @return a new, ready to run {@link ProcStarter} object instance
     * @throws InterruptedException the interrupted exception
     * @throws IOException          Signals that an I/O exception has occurred.
     */
    ProcStarter create(TricentisCiBuilder runner, Run<?, ?> run,
            FilePath workspace, Launcher launcher, TaskListener listener)
            throws InterruptedException, IOException;
}
