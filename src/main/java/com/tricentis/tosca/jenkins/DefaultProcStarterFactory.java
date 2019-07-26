package com.tricentis.tosca.jenkins;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;

import org.apache.commons.jelly.tags.core.UseBeanTag;
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
	private static final String CONFIG_FILE_PATH="/jenkins-tricentis/config.xml";
	private static final String CONFIG_FILE_START = "<?xml version=\"1.0\" encoding=\"utf-16\" ?>\n" + "\n"
			+ "<testConfiguration>\n" + "\n" + "   <TestEvents>";
	private static final String CONFIG_FILE_END = "\n" + "    </TestEvents>\n" + "\n" + "</testConfiguration>";
	private static final String TEST_EVENT_START = "\n" + "        <TestEvent>";
	private static final String TEST_EVENT_END = "</TestEvent>";

	@Override
	public ProcStarter create(final TricentisCiBuilder runner, final Run<?, ?> run, final FilePath workspace, final Launcher launcher,
			final TaskListener listener)
					throws InterruptedException, IOException {
		String clientPath = runner.getTricentisClientPath();
		final EnvVars vars = run.getEnvironment(listener);
		clientPath = vars.expand(clientPath);
		final String configPath = runner.getConfigurationFilePath();
		final String testEvents = runner.getTestEvents();
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
		if (testEvents != null && !testEvents.isEmpty()) {
			String path = BuildXmlFile(testEvents.split(";"),workspace);
			if (path != null) {
				builder.add(CONFIG_SWITCH, path);
			}
		} else if (configPath != null) {
			builder.add(CONFIG_SWITCH, vars.expand(configPath));
		}
		if (endpoint != null) {
			builder.add(ENDPOINT_SWITCH, vars.expand(runner.getEndpoint()));
		}
		return launcher.launch()
				.cmds(builder)
				.stdout(listener).envs(vars).pwd(workspace);
	}

	private String BuildXmlFile(String[] testEvents,FilePath workspace) {

		File file;
		try {
			file = new File(workspace.createTempFile("temp-jenkins-tricentis", ".xml").toURI());
		} catch (IOException e1) {
			return null;
		} catch (InterruptedException e1) {
			return null;
		}

		try (BufferedWriter writer = new BufferedWriter(
				new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8))) {

			writer.write(CONFIG_FILE_START);
			for (String testEvent : testEvents) {
				writer.append(TEST_EVENT_START);
				writer.append(testEvent);
				writer.append(TEST_EVENT_END);
			}
			writer.append(CONFIG_FILE_END);
		} catch (IOException e) {
			// FileOutputStream exception
			return null;
		}
		return file.getAbsolutePath().replace('\\', '/');

	}
}
