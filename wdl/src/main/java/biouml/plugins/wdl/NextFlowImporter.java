package biouml.plugins.wdl;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.logging.Logger;

import biouml.model.Diagram;
import one.util.streamex.StreamEx;
import ru.biosoft.util.ApplicationUtils;
import ru.biosoft.util.TempFiles;

public class NextFlowImporter
{
    private static final Logger log = Logger.getLogger( NextFlowRunner.class.getName() );
    
    public static void runNextFlowDry(String nextFlowScript, Diagram diagram) throws Exception
    {
        String outputDir = TempFiles.path("nextflow").getAbsolutePath();
        boolean useWsl = System.getProperty("os.name").startsWith("Windows");
        new File(outputDir).mkdirs();
        File f = new File(outputDir, "temp.nf");
        f.createNewFile();
        ApplicationUtils.writeString(f, nextFlowScript);

        ProcessBuilder builder;
        if( useWsl )
        {
            String parent = new File(outputDir).getAbsolutePath().replace("\\", "/");
            builder = new ProcessBuilder("wsl", "--cd", parent, "nextflow", f.getName(), "-preview", "-with-dag dag.dot");
        }
        else
        {
            builder = new ProcessBuilder("nextflow", f.getName(), "-preview", "-with-dag dag.dot");
            builder.directory(new File(outputDir));
        }

        System.out.println("COMMAND: " + StreamEx.of(builder.command()).joining(" "));
        Process process = builder.start();
        new Thread( new Runnable()
        {
            public void run()
            {
                BufferedReader input = new BufferedReader( new InputStreamReader( process.getInputStream() ) );
                String line = null;

                try
                {
                    while( ( line = input.readLine() ) != null )
                        log.info( line );
                }
                catch( IOException e )
                {
                    e.printStackTrace();
                }
                //                
                //for some reason cwl-runner outputs everything into error stream
                BufferedReader err = new BufferedReader( new InputStreamReader( process.getErrorStream() ) );
                line = null;

                try
                {
                    while( ( line = err.readLine() ) != null )
                        log.info( line );
                }
                catch( IOException e )
                {
                    e.printStackTrace();
                }
            }
        } ).start();

        process.waitFor();

        File dag = new File(outputDir, "dag.dot");
        parseDot(dag, diagram);
    }
    
    public static void parseDot(File f, Diagram diagram) throws Exception
    {
       String content = ApplicationUtils.readAsString(f);
    }
    
    
}