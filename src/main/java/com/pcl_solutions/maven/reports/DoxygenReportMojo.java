package com.pcl_solutions.maven.reports;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import org.apache.maven.doxia.siterenderer.Renderer;
import org.apache.maven.project.MavenProject;
import org.apache.maven.reporting.AbstractMavenReport;
import org.apache.maven.reporting.MavenReportException;

/**
 * @author Phil Lello
 *
 * @goal doxygen
 * @phase site
 */
public class DoxygenReportMojo extends AbstractMavenReport
{
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
   * @parameter expression="${project.build.directory}/doxygen"
   * @required
   * @readonly
   */
  private String workDirectory;

  /**
   * @parameter default-value="${project}"
   * @required
   * @readonly
   */
  private MavenProject project;
 
  /**
   * @component
   * @required
   * @readonly
   */
  private Renderer siteRenderer;

  /**
   * Doxygen configuration map
   *
   * @parameter
   */
  private Map<String,String> options;

  public void executeReport(Locale defaultLocale) throws MavenReportException
  {
    String line;
    Process process;
    BufferedReader processOutput, processErrors;
    FileWriter configFileWriter;
    BufferedWriter configWriter;
    String doxyfile = workDirectory+File.separator+"Doxyfile";
    String[] args = { "doxygen", doxyfile };
    try {
    File f = new File(workDirectory);
    f.mkdirs();
    f = new File(doxyfile);
    if (!f.exists())
      f.createNewFile();
    configFileWriter = new FileWriter(f.getAbsoluteFile());
    configWriter = new BufferedWriter(configFileWriter);
    for (Map.Entry<String, String> entry : options.entrySet())
    {
      configWriter.write(entry.getKey()+"="+entry.getValue()+"\n");
    }
    configWriter.close();
    process = Runtime.getRuntime().exec(args);
    processOutput = new BufferedReader(new InputStreamReader(process.getInputStream()));
    processErrors = new BufferedReader(new InputStreamReader(process.getErrorStream()));
    boolean processAlive = true;
    do {
      while (processOutput.ready()) {
        line = processOutput.readLine();
        if (line != null) getLog().info(line);
      }
      while (processErrors.ready()) {
        line = processErrors.readLine();
        if (line != null) getLog().error(line);
      }
      try {
        process.exitValue();
        processAlive = false;
      } catch (IllegalThreadStateException e) {
      }
    } while (processAlive);
    process.waitFor();
    } catch (Exception e) {
      getLog().error(e);
    }
  }

  protected MavenProject getProject()
  {
    return project;
  }
 
  protected String getOutputDirectory()
  {
    return outputDirectory;
  }
 
  protected Renderer getSiteRenderer()
  {
    return siteRenderer;
  }
 
  public String getDescription( Locale locale )
  {
    return getBundle( locale ).getString( "report.myreport.description" );
  }
 
  public String getName( Locale locale )
  {
    return getBundle( locale ).getString( "report.myreport.name" );
  }
 
  public String getOutputName()
  {
    return "doxygen/html/index";
  }
 
  private ResourceBundle getBundle( Locale locale )
  {
    return ResourceBundle.getBundle( "doxygen", locale, this.getClass().getClassLoader() );
  }

  public boolean isExternalReport()
  {
    return true;
  }
}

