/*
 *
 */
package com.tricentis.tosca.jenkins;

import java.io.IOException;
import hudson.Launcher.ProcStarter;

/**
 * Executes given {@link ProcStarter}.
 *
 * @author Sergey Oplavin
 */
public interface ProcessExecutor {
    /**
     * Execute {@link ProcStarter}.
     *
     * @param procStarter process starter.
     * @return execution code.
     * @throws IOException          Signals that an I/O exception has occurred.
     * @throws InterruptedException the interrupted exception
     */
    int execute(ProcStarter procStarter)
            throws IOException, InterruptedException;
}
