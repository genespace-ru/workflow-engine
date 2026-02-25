package wdl;

import java.io.File;
import biouml.model.Diagram;
import biouml.plugins.wdl.WDLGenerator;
import biouml.plugins.wdl.diagram.WDLImporter;

public class TestWDLImporter
{

    public static String WDL_PATH = "C:/Users/Damag/eclipse_2024_6/BioUML/src/biouml/plugins/wdl/test_examples/wdl/test_scatter.wdl";

    public static void main(String ... args) throws Exception
    {
        File wdl = new File(WDL_PATH);

        WDLImporter importer = new WDLImporter();

        Diagram generatedDiagram = importer.generateDiagram(wdl, "test", null);
        
        System.out.println(generatedDiagram +" generated!");
        
        String generatedWDL = new WDLGenerator().generate(generatedDiagram);
        System.out.println("Generated WDL: ");
        System.out.println(generatedWDL);
    }
}
