package biouml.plugins.wdl;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Logger;

import biouml.model.Compartment;
import biouml.model.Diagram;
import one.util.streamex.StreamEx;
import ru.biosoft.access.DataCollectionUtils;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.TextDataElement;
import ru.biosoft.util.ApplicationUtils;

public class NextFlowRunner
{
    private static final String BIOUML_FUNCTIONS_NF = "resources/biouml_function.nf";
    private static final Logger log = Logger.getLogger(NextFlowRunner.class.getName());

    public static File generateFunctions(String outputDir) throws IOException
    {
        InputStream inputStream = NextFlowRunner.class.getResourceAsStream(BIOUML_FUNCTIONS_NF);
        File result = new File(outputDir, "biouml_function.nf");
        Files.copy(inputStream, result.toPath(), StandardCopyOption.REPLACE_EXISTING);
        return result;
    }

    public static void runNextFlow(String name, Map<String, Object> parameters, String nextFlowScript, String outputDir, boolean useWsl,
            String towerAddress) throws Exception
    {
        File dir = new File(outputDir);
        dir.mkdirs();
        String parent = new File(outputDir).getAbsolutePath().replace("\\", "/");
        parameters = linkParameters(parameters, dir);
        File config = generateConfig(name, parameters, outputDir);

        File f = new File(outputDir, name + ".nf");
        ApplicationUtils.writeString(f, nextFlowScript);

        String[] baseCommand = new String[] {"nextflow", f.getName(), "-c", config.getName()};
        String[] wslCommand = new String[] {"wsl", "--cd", parent};
        String[] towerCommand = new String[] {"-with-tower", "\'" + towerAddress + "\'"};

        List<String> command = new ArrayList<>();

        if( useWsl )
            command.addAll(StreamEx.of(wslCommand).toList());

        command.addAll(StreamEx.of(baseCommand).toList());

        if( towerAddress != null )
            command.addAll(StreamEx.of(towerCommand).toList());

        ProcessBuilder builder = new ProcessBuilder(command.stream().toArray(String[]::new));

        if( !useWsl )
            builder.directory(new File(outputDir));

        System.out.println("COMMAND: " + StreamEx.of(builder.command()).joining(" "));
        Process process = builder.start();

        executeProcess(process);
    }

    public static void executeProcess(Process process) throws Exception
    {
        CommandRunner r = new CommandRunner(process);
        Thread thread = new Thread(r);
        thread.start();
        process.waitFor();
    }

    private static class CommandRunner implements Runnable
    {
        Process process;

        public CommandRunner(Process process)
        {
            this.process = process;
        }

        public void log(BufferedReader input) throws IOException
        {
            String line = null;
            while( ( line = input.readLine() ) != null )
            {
                System.out.println(line);
                log.info(line);
            }
        }

        public void run()
        {
            BufferedReader input = new BufferedReader(new InputStreamReader(process.getInputStream()));
            try
            {
                log(input);
            }
            catch( IOException e )
            {
                e.printStackTrace();
            }
            BufferedReader err = new BufferedReader(new InputStreamReader(process.getErrorStream()));
            try
            {
                log(err);
            }
            catch( IOException e )
            {
                e.printStackTrace();
            }
        }
    }

    public static Map<String, Object> linkParameters(Map<String, Object> parameters, File dir)
    {
        Map<String, Object> result = new HashMap<>();
        for( Entry<String, Object> e : parameters.entrySet() )
        {
            try
            {
                Object value = e.getValue();
                result.put(e.getKey(), e.getValue());
                if( value instanceof String )
                {
                    File f = new File((String)value);
                    //                    Path link =Files.createLink(Path.of(dir.getAbsolutePath(), f.getName()), f.toPath());
                    File copy = copyFile(f, dir);
                    result.put(e.getKey(), "./" + copy.getName());
                }
            }
            catch( Exception ex )
            {
                ex.printStackTrace();
            }

        }
        return result;
    }

    public static File copyFile(File src, File targetDir) throws Exception
    {
        File dest = new File(targetDir, src.getName());
        if( src.isFile() )
        {
            ApplicationUtils.copyFile(dest, src, null);
        }
        else if( src.isDirectory() )
        {
            dest.mkdir();
            for( File f : src.listFiles() )
                copyFile(f, dest);
        }
        return dest;
    }

    public static File generateConfig(String name, Map<String, Object> parameters, String outputDir) throws Exception
    {
        File config = new File(outputDir, name + ".config");

        try (BufferedWriter bw = new BufferedWriter(new FileWriter(config)))
        {
            bw.write("docker.enabled = true");
            for( Entry<String, Object> e : parameters.entrySet() )
            {

                String value = ( e.getValue() instanceof String ) ? "\"" + e.getValue() + "\"" : e.getValue().toString();
                bw.write("\n");
                bw.write("params." + e.getKey() + " = " + value + "\n");
            }
        }
        return config;
    }

    public static String runNextFlow(Diagram diagram, String nextFlowScript, WorkflowSettings settings, String outputDir, boolean useWsl)
            throws Exception
    {
        if( settings.getOutputPath() == null )
            throw new InvalidParameterException("Output path not specified");

        new File(outputDir).mkdirs();
        DataCollectionUtils.createSubCollection(settings.getOutputPath());

        File config = new File(outputDir, "nextflow.config");
        ApplicationUtils.writeString(config, "docker.enabled = true");

        File json = settings.generateParametersJSON(outputDir);

        settings.exportCollections(outputDir);

        generateFunctions(outputDir);

        exportIncludes(diagram, outputDir);

        if( nextFlowScript == null )
            nextFlowScript = new NextFlowGenerator().generate(diagram);
        NextFlowPreprocessor preprocessor = new NextFlowPreprocessor();
        nextFlowScript = preprocessor.preprocess(nextFlowScript);

        String name = diagram.getName();
        File f = new File(outputDir, name + ".nf");
        ApplicationUtils.writeString(f, nextFlowScript);

        ProcessBuilder builder;
        if( useWsl )
        {
            String parent = new File(outputDir).getAbsolutePath().replace("\\", "/");
            builder = new ProcessBuilder("wsl", "--cd", parent, "nextflow", f.getName(), "-c", "nextflow.config", "-params-file",
                    json.getName());
        }
        else
        {
            builder = new ProcessBuilder("nextflow", f.getName(), "-c", "nextflow.config", "-params-file", json.getName());
            builder.directory(new File(outputDir));
        }

        System.out.println("COMMAND: " + StreamEx.of(builder.command()).joining(" "));
        Process process = builder.start();

        new Thread(new Runnable()
        {
            public void run()
            {
                BufferedReader input = new BufferedReader(new InputStreamReader(process.getInputStream()));
                String line = null;

                try
                {
                    while( ( line = input.readLine() ) != null )
                        log.info(line);
                }
                catch( IOException e )
                {
                    e.printStackTrace();
                }
                //                
                //for some reason cwl-runner outputs everything into error stream
                BufferedReader err = new BufferedReader(new InputStreamReader(process.getErrorStream()));
                line = null;

                try
                {
                    while( ( line = err.readLine() ) != null )
                        log.info(line);
                }
                catch( IOException e )
                {
                    e.printStackTrace();
                }
            }
        }).start();

        process.waitFor();
        importResults(diagram, settings, outputDir);
        return "";
        //        StreamGobbler inputReader = new StreamGobbler( process.getInputStream(), true );
        //        StreamGobbler errorReader = new StreamGobbler( process.getErrorStream(), true );
        //        process.waitFor();
        //
        //        if( process.exitValue() == 0 )
        //        {
        //            String logString = "";
        //            String outStr = inputReader.getData();
        //            if( !outStr.isEmpty() )
        //            {
        //                logString += outStr;
        //                log.info( outStr );
        //            }
        //            //for some reason cwl-runner outputs everything into error stream
        //            String errorStr = errorReader.getData();
        //            if( !errorStr.isEmpty() )
        //            {
        //                logString += errorStr;
        //                log.info( errorStr );
        //            }
        //            importResults( diagram, settings, outputDir );
        //            return logString;
        //        }
        //        else
        //        {
        //            String errorStr = errorReader.getData();
        //            log.info( errorStr );
        //            throw new Exception( "Nextflow executed with error: " + errorStr );
        //        }

    }

    public static void importResults(Diagram diagram, WorkflowSettings settings, String outputDir) throws Exception
    {
        if( settings.getOutputPath() == null )
            return;
        DataCollection dc = settings.getOutputPath().getDataCollection();

        for( Compartment n : WorkflowUtil.getAllCalls(diagram) )
        {
            if( WorkflowUtil.getDiagramRef(n) != null )
            {
                String ref = WorkflowUtil.getDiagramRef(n);
                Diagram externalDiagram = (Diagram)diagram.getOrigin().get(ref);
                importResults(externalDiagram, settings, outputDir);
                continue;
            }
            String taskRef = WorkflowUtil.getTaskRef(n);
            String folderName = ( taskRef );
            File folder = new File(outputDir, folderName);
            if( !folder.exists() || !folder.isDirectory() )
            {
                log.info("No results for " + n.getName());
                continue;
            }
            DataCollection nested = DataCollectionUtils.createSubCollection(dc.getCompletePath().getChildPath(folderName));
            for( File f : folder.listFiles() )
            {
                String data = ApplicationUtils.readAsString(f);
                nested.put(new TextDataElement(f.getName(), nested, data));
            }
        }
    }

    public static void exportIncludes(Diagram diagram, String outputDir) throws Exception
    {
        for( Diagram d : getIncludes(diagram) )
            WorkflowUtil.export(d, new File(outputDir));
    }

    public static Set<Diagram> getIncludes(Diagram diagram)
    {
        Set<Diagram> result = new HashSet<>();
        for( ImportProperties ip : WorkflowUtil.getImports(diagram) )
        {
            DataElementPath dep = ip.getSource();
            if( dep != null )
            {
                DataElement de = dep.getDataElement();
                if( de instanceof Diagram )
                {
                    result.add((Diagram)de);
                    continue;
                }
            }
            String name = ip.getSourceName();
            DataElement de = DataElementPath.create(diagram.getOrigin(), name).getDataElement();
            if( de instanceof Diagram )
            {
                result.add((Diagram)de);
            }
        }
        Set<Diagram> additionals = new HashSet<Diagram>();
        for( Diagram d : result )
            additionals.addAll(getIncludes(d));
        result.addAll(additionals);
        return result;
    }
}
