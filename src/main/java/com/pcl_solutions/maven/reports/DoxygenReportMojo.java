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

import org.apache.commons.io.FileUtils;
import org.apache.maven.doxia.siterenderer.Renderer;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.reporting.AbstractMavenReport;
import org.apache.maven.reporting.MavenReportException;
import org.apache.maven.wagon.PathUtils;

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
    

    /**
     * The projects in the reactor for aggregation report.
     */
    @Parameter( property = "reactorProjects", readonly = true )
    private List<MavenProject> reactorProjects;
    
    /**
     * Doxygen configuration map
     *
     * @parameter
     */
    private Map<String,String> options;
    
    /**
     * Folders relative to base.dir for doxygen to search
     * 
     * @parameter
     */
    private String[] inputs;

    public void executeReport(Locale defaultLocale) throws MavenReportException
    {
      getLog().info(this.getClass().getName());
      Process process;
      FileWriter configFileWriter;
      BufferedWriter configWriter;
      String workDirectory = buildDirectory+File.separator+"doxygen";
      String doxyfile = workDirectory+File.separator+"Doxyfile";
      String[] args = { "doxygen", doxyfile };
      if (!options.containsKey("PROJECT_NAME"))
        options.put("PROJECT_NAME", "\""+projectName+"\"");
      if (!options.containsKey("PROJECT_VERSION"))
        options.put("PROJECT_VERSION", "\""+projectVersion+"\"");
      if (!options.containsKey("OUTPUT_DIRECTORY"))
        options.put("OUTPUT_DIRECTORY", "\""+outputDirectory+File.separator+"doxygen\"");
      if (!options.containsKey("INPUT"))
    	  
    	  if(inputs != null && inputs.length > 0)
    	  {
    		StringBuilder doxygenInputs = new StringBuilder();
    		String root = baseDir;
    		if(!root.endsWith("/") && !root.endsWith("\\"))
    		{
    			root = root + File.separator;
    		}
    		
    		for(String input : inputs)
    		{
    			String fullInput = root + input;
    			getLog().debug("Adding input: " + fullInput);
    			doxygenInputs.append("\"");
    			doxygenInputs.append(fullInput);
    			doxygenInputs.append("\"");
    			doxygenInputs.append(" \\");
    			doxygenInputs.append("\n");
    		}
    		
    		String fullInputs = doxygenInputs.substring(0, doxygenInputs.length() - " /\n".length());
    		
    		options.put("INPUT", fullInputs);
    	  }else
    	  {
    		  options.put("INPUT", "\""+sourceDirectory+"\"");
    	  }
      try
      {
        File f = new File(workDirectory);
        f.mkdirs();
        f = new File(doxyfile);
        if (!f.exists())
        {
          f.createNewFile();
        }
        configFileWriter = new FileWriter(f.getAbsoluteFile());
        configWriter = new BufferedWriter(configFileWriter);
        for (Map.Entry<String, String> entry : options.entrySet())
        {
          configWriter.write(entry.getKey() + "=" + entry.getValue() + "\n");
        }
        configWriter.close();
        process = Runtime.getRuntime().exec(args);
        ErrStreamForwarder errForwarder = new ErrStreamForwarder(process.getErrorStream());
        StdStreamForwarder stdForwarder = new StdStreamForwarder(process.getInputStream());

        process.waitFor();

        errForwarder.stop();
        stdForwarder.stop();
      }
      catch (Exception e)
      {
        getLog().error(e);
      }
      
      getLog().debug("Here");
      getLog().debug(String.valueOf(isAggregate()));
      getLog().debug(String.valueOf(project.isExecutionRoot()));
      if ( isAggregate() && project.isExecutionRoot() )
      {
          getLog().debug("Attempting to aggregate");
          for ( MavenProject subProject : reactorProjects )
          {
              if ( subProject != getProject() && getDoxygenDirectory() != null )
              {
                  String doxygenDirRelative =
                      PathUtils.toRelative( getProject().getBasedir(), getDoxygenDirectory().getAbsolutePath() );
                  File doxygenDir = new File( subProject.getBasedir(), doxygenDirRelative );
                  getLog().debug("Copying from: " + doxygenDir.getAbsolutePath());
                  getLog().debug("Copying to: " +  getDoxygenDirectory());
                  // Copy files
                  try
                  {
                      FileUtils.copyDirectory(doxygenDir, getDoxygenDirectory());
                  }
                  catch (IOException e)
                  {
                      getLog().error(e);
                  }
              }
          }
      }
    }

    public boolean isAggregate()
    {
        return false;
    }

    private File getDoxygenDirectory()
    {
      return new File(options.get("OUTPUT_DIRECTORY"));
    }

    public String getOutputName()
    {
      return "doxygen/html/index";
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
   
    private ResourceBundle getBundle( Locale locale )
    {
      return ResourceBundle.getBundle( "doxygen", locale, this.getClass().getClassLoader() );
    }

    public boolean isExternalReport()
    {
      return true;
    }
    
    private class ErrStreamForwarder implements Runnable
    {

        private final BufferedReader errorReader;

        private boolean running = true;

        private Thread forwarder;


        public ErrStreamForwarder(InputStream errStream)
        {
            errorReader = new BufferedReader(new InputStreamReader(errStream));

            forwarder = new Thread(this);
            forwarder.start();
        }

        public void stop()
        {
            running = false;
            if (forwarder != null)
            {
                try
                {
                    forwarder.join();
                }
                catch (InterruptedException e)
                {
                    // do nothing
                }
            }
        }

        public void run()
        {
            String line;
            while (running)
            {
                try
                {
                    line = errorReader.readLine();
                    if (line != null)
                    {
                        getLog().error(line);
                    }
                }
                catch (IOException e)
                {
                    getLog().error("Error reading process streams", e);
                }
            }
            try
            {
                errorReader.close();
            }
            catch (IOException e)
            {
                getLog().warn("Failed to close process readers", e);
            }

        }
    }

    private class StdStreamForwarder implements Runnable
    {

        private final BufferedReader stdReader;

        private boolean running = true;

        private Thread forwarder;


        public StdStreamForwarder(InputStream errStream)
        {
            stdReader = new BufferedReader(new InputStreamReader(errStream));

            forwarder = new Thread(this);
            forwarder.start();
        }

        public void stop()
        {
            running = false;
            if (forwarder != null)
            {
                try
                {
                    forwarder.join();
                }
                catch (InterruptedException e)
                {
                    // do nothing
                }
            }
        }

        public void run()
        {
            String line;
            while (running)
            {
                try
                {
                    line = stdReader.readLine();
                    if (line != null)
                    {
                        getLog().info(line);
                    }
                }
                catch (IOException e)
                {
                    getLog().error("Error reading process streams", e);
                }
            }
            try
            {
                stdReader.close();
            }
            catch (IOException e)
            {
                getLog().warn("Failed to close process readers", e);
            }

        }
    }
}

