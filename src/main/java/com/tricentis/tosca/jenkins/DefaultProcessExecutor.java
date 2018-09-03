package com.tricentis.tosca.jenkins;

import java.io.IOException;

import hudson.Launcher.ProcStarter;

/**
 * Default implementation of {@link ProcessExecutor}. Just executrs given
 * {@link ProcStarter} and waits for its termination.
 *
 * @author Sergey Oplavin
 *
 */
public class DefaultProcessExecutor implements ProcessExecutor {

	@Override
	public int execute(final ProcStarter starter) throws IOException, InterruptedException {
		return starter.join();
	}

}
