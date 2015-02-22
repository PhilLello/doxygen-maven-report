package com.pcl_solutions.maven.reports;

import java.io.File;

import org.apache.maven.project.MavenProject;

/**
 * 
 * @author robin
 * 
 * @goal aggregate
 * @phase site
 */
public class AggregateDoxygenReportMojo extends AbstractDoxygenReportMojo {

	public boolean isAggregate() {
		return true;
	}

	@Override
	public void configureInputs() {
		getLog().debug("Configuring inputs for aggregate report");
		if (!getOptions().containsKey("INPUT")) {

			boolean inputSettings = false;

			if (getInputs() != null && getInputs().length > 0) {
				inputSettings = true;
			}

			StringBuilder doxygenInputs = new StringBuilder();
			for (MavenProject subProject : getReactorProjects()) {
				getLog().debug("sub project: " + subProject);
				subProject.getBasedir();

				if (inputSettings) {
					String root = subProject.getBasedir().getAbsolutePath();

					if (!root.endsWith("/") && !root.endsWith("\\")) {
						root = root + File.separator;
					}

					for (String input : getInputs()) {
						String fullInput = root + input;
						getLog().debug("Adding input: " + fullInput);
						doxygenInputs.append("\"");
						doxygenInputs.append(fullInput);
						doxygenInputs.append("\"");
						doxygenInputs.append(" \\");
						doxygenInputs.append("\n");
					}
				} else {
					doxygenInputs.append("\"");
					doxygenInputs.append(""); // source directory
					doxygenInputs.append("\"");
					doxygenInputs.append(" \\");
					doxygenInputs.append("\n");
				}
			}

			String fullInputs = doxygenInputs.substring(0,
					doxygenInputs.length() - " /\n".length());

			getOptions().put("INPUT", fullInputs);
		}

	}
}
