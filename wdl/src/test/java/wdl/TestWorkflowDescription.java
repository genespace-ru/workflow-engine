package wdl;

import java.io.File;

import biouml.plugins.wdl.model.ScriptInfo;
import biouml.plugins.wdl.model.TaskInfo;
import biouml.plugins.wdl.nextflow.NextFlowImporter;
import ru.biosoft.util.ApplicationUtils;

public class TestWorkflowDescription
{
    public static String NEXTFLOW_PATH = "C:/Users/Damag/nextflow_work/sc analysis/main.nf";

    public static void main(String ... args) throws Exception
    {
        File nextflowFile = new File(NEXTFLOW_PATH);
        String nextflow = ApplicationUtils.readAsString(nextflowFile);
        ScriptInfo scriptInfo = new NextFlowImporter().parseNextflow("test", nextflow );
        
        for (String taskName: scriptInfo.getTaskNames())
        {
            TaskInfo taskInfo = scriptInfo.getTask(taskName);
            String docker = taskInfo.getMetaProperty("container");     
            System.out.println("Process " + taskName+", Docker: "+ docker);
        }
    }
}
