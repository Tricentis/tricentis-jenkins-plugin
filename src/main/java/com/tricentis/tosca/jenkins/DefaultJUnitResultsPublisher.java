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
 *
 */
public class DefaultJUnitResultsPublisher implements JUnitResultsPublisher {

	@Override
	public void publish(final String reportFileName, final Run<?, ?> run, final FilePath workspace, final Launcher launcher, final TaskListener listener)
			throws InterruptedException, IOException {
		final JUnitResultArchiver archiver = new JUnitResultArchiver(reportFileName);
		archiver.perform(run, workspace, launcher, listener);
	}

}
