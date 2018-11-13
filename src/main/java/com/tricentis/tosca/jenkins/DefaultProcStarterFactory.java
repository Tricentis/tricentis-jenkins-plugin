package com.tricentis.tosca.jenkins;

import java.io.IOException;

import org.apache.commons.lang.StringUtils;

import hudson.EnvVars;
import hudson.FilePath;
import hudson.Launcher;
import hudson.Launcher.ProcStarter;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.util.ArgumentListBuilder;

/**
 * Default implementation for {@link ProcStarterFactory}.
 *
 * @author Sergey Oplavin
 *
 */
public class DefaultProcStarterFactory implements ProcStarterFactory {

	private static final String MODE_SWITCH = "-m";
	private static final String RESULTS_SWITCH = "-r";
	private static final String CONFIG_SWITCH = "-c";
	private static final String ENDPOINT_SWITCH = "-e";
	private static final String REPORT_TYPE_SWITH = "-t";
	private static final String SPEC_EXIT_CODE_SWITCH = "-x";
	private static final String DEFAULT_MODE = "distributed";
	private static final String JUNIT_REPORT_TYPE = "junit";
	private static final String SPEC_EXIT_CODE_VALUE = "True";

	private static final String JAVA_HOME = "JAVA_HOME";

	@Override
	public ProcStarter create(final TricentisCiBuilder runner, final Run<?, ?> run, final FilePath workspace, final Launcher launcher,
			final TaskListener listener)
					throws InterruptedException, IOException {
		String clientPath = runner.getTricentisClientPath();
		final EnvVars vars = run.getEnvironment(listener);
		clientPath = vars.expand(clientPath);
		final String configPath = runner.getConfigurationFilePath();
		final String endpoint = runner.getEndpoint();
		final String application;
		if (clientPath.toLowerCase().endsWith(".jar")) {
			final String javaHome = vars.get(JAVA_HOME);
			if (StringUtils.isEmpty(javaHome)) {
				throw new RuntimeException(Messages.setJavaHome());
			}
			application = new FilePath(launcher.getChannel(), javaHome).child("bin").child("java").getRemote();
		} else {
			application = null;
		}

		final ArgumentListBuilder builder = new ArgumentListBuilder();

		if (application != null) {
			builder.add(application);
			builder.add("-jar");
		}

		builder.add(clientPath, MODE_SWITCH, DEFAULT_MODE, REPORT_TYPE_SWITH, JUNIT_REPORT_TYPE, SPEC_EXIT_CODE_SWITCH, SPEC_EXIT_CODE_VALUE, RESULTS_SWITCH,
				workspace.child(vars.expand(runner.getResultsFile())).getRemote());
		if (configPath != null) {
			builder.add(CONFIG_SWITCH, vars.expand(configPath));
		}
		if (endpoint != null) {
			builder.add(ENDPOINT_SWITCH, vars.expand(runner.getEndpoint()));
		}
		return launcher.launch()
				.cmds(builder)
				.stdout(listener).envs(vars).pwd(workspace);
	}

}
