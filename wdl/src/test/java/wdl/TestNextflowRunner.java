package wdl;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import biouml.plugins.wdl.NextFlowRunner;
import ru.biosoft.util.ApplicationUtils;

public class TestNextflowRunner
{
    public static String NEXTFLOW_PATH = "C:/Users/Damag/git/lims-test-hemotology/workflows/fastqc.nf";
    public static String OUTPUT_DIR = "C:/Users/Damag/lims"; 
    
    public static String PROJECT_DIR_ARGUMENT = "/mnt/c/Users/Damag/git/lims-test-hemotology/projects/test-hematology";
            
    public static void main(String ... args) throws Exception
    {
        File nextflow = new File(NEXTFLOW_PATH);
        String nextFlowScript = ApplicationUtils.readAsString(nextflow);
        Map<String, Object> params = new HashMap<>();
        params.put("projectDir", PROJECT_DIR_ARGUMENT);
        
        String towerAddress = "http://172.24.112.1:8200/nf";
        boolean isWindows = System.getProperty( "os.name" ).startsWith( "Windows" );
        String outputDir = OUTPUT_DIR; 
                
        NextFlowRunner.runNextFlow("1", "test", params, nextFlowScript, outputDir, isWindows, towerAddress);
    }
}
