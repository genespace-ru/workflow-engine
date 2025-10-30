package converter;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.StringReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import javax.annotation.Nonnull;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;

import biouml.model.Diagram;
import biouml.model.util.DiagramImageGenerator;
import biouml.plugins.wdl.NextFlowGenerator;
import biouml.plugins.wdl.diagram.WDLImporter;
import biouml.plugins.wdl.parser.AstStart;
import biouml.plugins.wdl.parser.WDLParser;
import ru.biosoft.util.ApplicationUtils;

public class Converter
{

    public static void main(String ... args)
    {
        try
        {
            ConverterParameters parameters = new ConverterParameters(args);

            if( parameters.showHelp )
                log("HELP WILL BE ADDED LATER");

            String filePath = parameters.filePath;

            Path path = Paths.get(filePath);
            String absolutePath;
            String parent;
            if( path.isAbsolute() )
            {
                absolutePath = path.toString();
                parent = path.getParent().toString();
            }
            else
            {
                String jarPath = Converter.class.getProtectionDomain().getCodeSource().getLocation().getFile();
                parent = new File(jarPath).getParent();
                absolutePath = parent + "/" + filePath;
            }
            String name = new File(absolutePath).getName();
            Diagram diagram = loadDiagram(absolutePath);

            if( parameters.showImage )
            {
                File imageFile = new File(parent + "/" + name + ".png");
                exportImage(diagram, imageFile);
                log("Image generated: " + imageFile.getName());
            }

            String nextFlow = new NextFlowGenerator().generateNextFlow(diagram, true);
            File nextFlowFile = new File(parent + "/" + name + ".nf");
            ApplicationUtils.writeString(nextFlowFile, nextFlow);
            log("Nextflow script generated: " + nextFlowFile.getName());
            log("All done!");

        }
        catch( Exception ex )
        {
            ex.printStackTrace();
        }
    }

    private static Diagram loadDiagram(String path) throws Exception
    {
        File f = new File(path);

        String name = f.getName();
        name = f.getName().endsWith(".wdl") ? name.substring(0, name.length() - 4) : name;

        WDLImporter importer = new WDLImporter();

        String text = ApplicationUtils.readAsString(f);

        AstStart start = new WDLParser().parse(new StringReader(text));
        return importer.generateDiagram(start, null, "diagram");
    }

    public static void exportImage(@Nonnull
    Diagram diagram, @Nonnull
    File file) throws Exception
    {
        BufferedImage image = DiagramImageGenerator.generateDiagramImage(diagram, 1, true);

        ImageWriter writer = ImageIO.getImageWritersBySuffix("png").next();

        file.delete();
        try (ImageOutputStream stream = ImageIO.createImageOutputStream(file))
        {
            writer.setOutput(stream);
            writer.write(image);
        }
        writer.dispose();
    }

    private static void log(String s)
    {
        System.out.println(getCurrentTime() + s);
    }

    public static String getCurrentTime()
    {
        Calendar cal = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("[ HH:mm:ss ] ");
        return sdf.format(cal.getTime());
    }
}