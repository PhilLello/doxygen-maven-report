package com.pcl_solutions.maven.reports;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

import org.apache.maven.doxia.siterenderer.Renderer;
import org.apache.maven.project.MavenProject;
import org.apache.maven.reporting.AbstractMavenReport;
import org.apache.maven.reporting.MavenReportException;

public abstract class AbstractDoxygenReportMojo extends AbstractMavenReport {

	/**
	 * The projects in the reactor for aggregation report.
	 * 
	 * @parameter expression="reactorProjects"
	 */
	private List<MavenProject> reactorProjects;

	/**
	 * Doxygen configuration map
	 * 
	 * @parameter
	 */
	private Map<String, String> options;

	/**
	 * Folders relative to base.dir for doxygen to search
	 * 
	 * @parameter
	 */
	private String[] inputs;

	/**
	 * Directory where reports will go.
	 * 
	 * @parameter expression="${project.reporting.outputDirectory}"
	 * @required
	 * @readonly
	 */
	private String outputDirectory;

	/**
	 * Directory for work file(s).
	 * 
	 * @parameter expression="${project.build.directory}"
	 * @required
	 * @readonly
	 */
	private String buildDirectory;

	/**
	 * @parameter default-value="${project}"
	 * @required
	 * @readonly
	 */
	private MavenProject project;

	/**
	 * @parameter default-value="${project.name}"
	 * @required
	 * @readonly
	 */
	private String projectName;

	/**
	 * @parameter default-value="${project.version}"
	 * @required
	 * @readonly
	 */
	private String projectVersion;

	/**
	 * @parameter default-value="${project.build.sourceDirectory}"
	 * @required
	 * @readonly
	 */
	private String sourceDirectory;

	/**
	 * @parameter default-value="${basedir}"
	 * @required
	 * @readonly
	 */
	private String baseDir;

	/**
	 * @component
	 * @required
	 * @readonly
	 */
	private Renderer siteRenderer;

	public String getDescription(Locale locale) {
		return getBundle(locale).getString("report.myreport.description");
	}

	public String getName(Locale locale) {
		return getBundle(locale).getString("report.myreport.name");
	}

	public String getOutputName() {
		return "doxygen/html/index";
	}

	@Override
	protected MavenProject getProject() {
		return project;
	}

	@Override
	protected String getOutputDirectory() {
		return outputDirectory;
	}

	@Override
	protected Renderer getSiteRenderer() {
		return siteRenderer;
	}

	private ResourceBundle getBundle(Locale locale) {
		return ResourceBundle.getBundle("doxygen", locale, this.getClass()
				.getClassLoader());
	}

	public String getProjectName() {
		return projectName;
	}

	public String getProjectVersion() {
		return projectVersion;
	}

	public String getSourceDirectory() {
		return sourceDirectory;
	}

	public String getBaseDir() {
		return baseDir;
	}

	public String getBuildDirectory() {
		return buildDirectory;
	}

	public Map<String, String> getOptions() {
		return options;
	}

	public String[] getInputs() {
		return inputs;
	}

	public List<MavenProject> getReactorProjects() {
		return reactorProjects;
	}

	protected File getDoxygenDirectory() {
		return new File(getOptions().get("OUTPUT_DIRECTORY"));
	}

	public boolean isExternalReport() {
		return true;
	}

	public abstract void configureInputs();

	public void executeReport(Locale defaultLocale) throws MavenReportException {
		getLog().info(this.getClass().getName());
		Process process;
		FileWriter configFileWriter;
		BufferedWriter configWriter;
		String workDirectory = getBuildDirectory() + File.separator + "doxygen";
		String doxyfile = workDirectory + File.separator + "Doxyfile";
		String[] args = { "doxygen", doxyfile };
		if (!getOptions().containsKey("PROJECT_NAME")) {
			getOptions().put("PROJECT_NAME", "\"" + getProjectName() + "\"");
		}

		if (!getOptions().containsKey("PROJECT_VERSION")) {
			getOptions().put("PROJECT_VERSION",
					"\"" + getProjectVersion() + "\"");
		}

		if (!getOptions().containsKey("OUTPUT_DIRECTORY")) {
			getOptions().put("OUTPUT_DIRECTORY",
					"\"" + getOutputDirectory() + File.separator + "doxygen\"");
		}

		configureInputs();
		try {
			File f = new File(workDirectory);
			f.mkdirs();
			f = new File(doxyfile);
			if (!f.exists()) {
				f.createNewFile();
			}
			configFileWriter = new FileWriter(f.getAbsoluteFile());
			configWriter = new BufferedWriter(configFileWriter);
			for (Map.Entry<String, String> entry : getOptions().entrySet()) {
				configWriter.write(entry.getKey() + "=" + entry.getValue()
						+ "\n");
			}
			configWriter.close();
			process = Runtime.getRuntime().exec(args);
			ErrStreamForwarder errForwarder = new ErrStreamForwarder(
					process.getErrorStream());
			StdStreamForwarder stdForwarder = new StdStreamForwarder(
					process.getInputStream());

			process.waitFor();

			errForwarder.stop();
			stdForwarder.stop();
		} catch (Exception e) {
			getLog().error(e);
		}
	}

	protected class ErrStreamForwarder implements Runnable {

		private final BufferedReader errorReader;

		private boolean running = true;

		private Thread forwarder;

		public ErrStreamForwarder(InputStream errStream) {
			errorReader = new BufferedReader(new InputStreamReader(errStream));

			forwarder = new Thread(this);
			forwarder.start();
		}

		public void stop() {
			running = false;
			if (forwarder != null) {
				try {
					forwarder.join();
				} catch (InterruptedException e) {
					// do nothing
				}
			}
		}

		public void run() {
			String line;
			while (running) {
				try {
					line = errorReader.readLine();
					if (line != null) {
						getLog().error(line);
					}
				} catch (IOException e) {
					getLog().error("Error reading process streams", e);
				}
			}
			try {
				errorReader.close();
			} catch (IOException e) {
				getLog().warn("Failed to close process readers", e);
			}

		}
	}

	protected class StdStreamForwarder implements Runnable {

		private final BufferedReader stdReader;

		private boolean running = true;

		private Thread forwarder;

		public StdStreamForwarder(InputStream errStream) {
			stdReader = new BufferedReader(new InputStreamReader(errStream));

			forwarder = new Thread(this);
			forwarder.start();
		}

		public void stop() {
			running = false;
			if (forwarder != null) {
				try {
					forwarder.join();
				} catch (InterruptedException e) {
					// do nothing
				}
			}
		}

		public void run() {
			String line;
			while (running) {
				try {
					line = stdReader.readLine();
					if (line != null) {
						getLog().info(line);
					}
				} catch (IOException e) {
					getLog().error("Error reading process streams", e);
				}
			}
			try {
				stdReader.close();
			} catch (IOException e) {
				getLog().warn("Failed to close process readers", e);
			}

		}
	}

}
