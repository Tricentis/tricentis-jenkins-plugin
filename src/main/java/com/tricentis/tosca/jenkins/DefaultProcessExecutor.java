/*
 *
 */
package com.tricentis.tosca.jenkins;

import java.io.IOException;
import hudson.Launcher.ProcStarter;

/**
 * Default implementation of {@link ProcessExecutor}. Just executrs given
 * {@link ProcStarter} and waits for its termination.
 *
 * @author Sergey Oplavin
 */
public class DefaultProcessExecutor implements ProcessExecutor {
    /**
     * Execute.
     *
     * @param starter the starter
     * @return the int
     * @throws IOException          Signals that an I/O exception has occurred.
     * @throws InterruptedException the interrupted exception
     */
    @Override
    public int execute(final ProcStarter starter)
            throws IOException, InterruptedException {
        return starter.join();
    }
}
