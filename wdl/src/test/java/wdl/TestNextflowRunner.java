package wdl;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import biouml.plugins.wdl.NextFlowRunner;
import ru.biosoft.util.ApplicationUtils;

public class TestNextflowRunner
{
    public static String LIMS_TEST_HEMATOLOGY = "C:/Users/Damag/git/lims-test-hemotology";
    public static String NEXTFLOW_PATH = LIMS_TEST_HEMATOLOGY+"/workflows/fastqc.nf";
    public static String PROJECT_DIR_ARGUMENT = "C:/Users/Damag/git/lims-test-hemotology/projects/test-hematology";
    
    public static String OUTPUT_DIR = "C:/Users/Damag/lims"; 
            
    public static void main(String ... args) throws Exception
    {
        File nextflow = new File(NEXTFLOW_PATH);
        String nextFlowScript = ApplicationUtils.readAsString(nextflow);
        Map<String, Object> params = new HashMap<>();
        params.put("projectDir", PROJECT_DIR_ARGUMENT);
        
        String towerAddress = null;
        boolean isWindows = System.getProperty( "os.name" ).startsWith( "Windows" );
        String outputDir = OUTPUT_DIR; 
                
        NextFlowRunner.runNextFlow("test", params, nextFlowScript, outputDir, isWindows, towerAddress);

    }
}
