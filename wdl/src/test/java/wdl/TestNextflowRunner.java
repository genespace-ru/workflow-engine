package wdl;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import com.developmentontheedge.beans.DynamicProperty;
import com.developmentontheedge.beans.DynamicPropertySet;
import com.developmentontheedge.beans.DynamicPropertySetSupport;

import biouml.plugins.wdl.GeneSpaceContext;
import biouml.plugins.wdl.WorkflowSettings;
import biouml.plugins.wdl.nextflow.NextFlowRunner;
import ru.biosoft.util.ApplicationUtils;

public class TestNextflowRunner
{
    public static String NEXTFLOW_PATH = "C:/Users/Damag/git/lims-test-hemotology/workflows/fastqc.nf";
    public static String OUTPUT_DIR = "C:/Users/Damag/lims"; 
    
    public static String PROJECT_DIR_ARGUMENT = "/mnt/c/Users/Damag/git/lims-test-hemotology/projects/test-hematology";

    public static void main(String ... args) throws Exception
    {
        //runIlya();
        //runAnna( false );
        runAnna( true );
    }

    private static void runIlya() throws Exception
    {
        File nextflow = new File(NEXTFLOW_PATH);
        String nextFlowScript = ApplicationUtils.readAsString(nextflow);
        Map<String, Object> params = new HashMap<>();
        params.put("projectDir", PROJECT_DIR_ARGUMENT);
        
        String towerAddress = "http://172.24.112.1:8200/nf";
        boolean isWindows = System.getProperty( "os.name" ).startsWith( "Windows" );
        //String outputDir = OUTPUT_DIR; 

        Path outputDir = Paths.get( OUTPUT_DIR );
        GeneSpaceContext context = new GeneSpaceContext( outputDir, outputDir, outputDir, outputDir );
        WorkflowSettings settings = new WorkflowSettings();
        settings.setParameters( initParameters( params ) );
        NextFlowRunner.runNextFlow( "1", "test", nextFlowScript, isWindows, settings, towerAddress, context, null );
    }

    private static void runAnna(boolean useDocker) throws Exception
    {
        NEXTFLOW_PATH = "/home/anna/projects/github/genespace-ru/lims-test-hemotology/workflows/fastqc.nf";
        OUTPUT_DIR = "/home/anna/projects/github/genespace-ru/lims-test-hemotology/runs";
        if( useDocker )
            OUTPUT_DIR += "/docker";
        else
            OUTPUT_DIR += "/local";
        PROJECT_DIR_ARGUMENT = "/home/anna/projects/github/genespace-ru/lims-test-hemotology/projects/test-hematology";

        String resultsDir = PROJECT_DIR_ARGUMENT + "/results";
        File nextflow = new File( NEXTFLOW_PATH );
        String nextFlowScript = ApplicationUtils.readAsString( nextflow );
        Map<String, Object> params = new HashMap<>();
        params.put( "readsDir", PROJECT_DIR_ARGUMENT + "/samples" ); //absolute path on server
        params.put( "fastqcDir", resultsDir + "/fastqc" ); //absolute path on server
        params.put( "multiqcDir", resultsDir + "/multiqc" ); //absolute path on server
        params.put( "parseUrl", "http://172.17.0.1:8200/nf/parse/multiqc" ); //ip of docker0
        params.put( "parseData", "\\\"prjId\\\":" + 10 + ",\\\"workflowId\\\":" + 158 );

        String towerAddress = "http://172.17.0.1:8200/nf";
        boolean isWindows = System.getProperty( "os.name" ).startsWith( "Windows" );

        Path outputDir = Paths.get( OUTPUT_DIR );
        GeneSpaceContext context = new GeneSpaceContext( new File( PROJECT_DIR_ARGUMENT ).getParentFile().toPath(), new File( NEXTFLOW_PATH ).getParentFile().toPath(), null,
                outputDir );
        WorkflowSettings settings = new WorkflowSettings();
        settings.setParameters( initParameters( params ) );
        NextFlowRunner.runNextFlow( "158", "test", nextFlowScript, isWindows, settings, towerAddress, context, null );
    }

    public static DynamicPropertySet initParameters(Map<String, Object> params)
    {
        DynamicPropertySet parameters = new DynamicPropertySetSupport();
        for ( Entry<String, Object> param : params.entrySet() )
        {
            String name = param.getKey();
            Object value = param.getValue();
            Class clazz = String.class;
            DynamicProperty dp = new DynamicProperty( name, clazz, value );
            parameters.add( dp );
        }
        return parameters;
    }

}
