package com.pcl_solutions.maven.reports;

import java.io.File;

/**
 * @author Phil Lello
 * 
 * @goal doxygen
 * @phase site
 */
public class DoxygenReportMojo extends AbstractDoxygenReportMojo {

	public boolean isAggregate() {
		return false;
	}

	@Override
	public void configureInputs() {
		getLog().debug("Configuring inputs for single report");
		if (!getOptions().containsKey("INPUT")) {

			if (getInputs() != null && getInputs().length > 0) {
				StringBuilder doxygenInputs = new StringBuilder();
				String root = getBaseDir();
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

				String fullInputs = doxygenInputs.substring(0,
						doxygenInputs.length() - " /\n".length());

				getOptions().put("INPUT", fullInputs);
			} else {
				getOptions().put("INPUT", "\"" + getSourceDirectory() + "\"");
			}
		}

	}

}
