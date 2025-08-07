package biouml.plugins.wdl.web;

import java.io.OutputStream;
import java.io.StringReader;
import java.util.logging.Level;

import org.json.JSONArray;
import org.json.JSONObject;

import biouml.model.Diagram;
import biouml.model.server.WebDiagramsProvider;
import biouml.plugins.wdl.NextFlowGenerator;
import biouml.plugins.wdl.WorkflowSettings;
import biouml.plugins.wdl.WDLGenerator;
import biouml.plugins.wdl.WDLRunner;
import biouml.plugins.wdl.diagram.WDLImporter;
import biouml.plugins.wdl.parser.AstStart;
import biouml.plugins.wdl.parser.WDLParser;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.server.JSONUtils;
import ru.biosoft.server.servlets.webservices.BiosoftWebRequest;
import ru.biosoft.server.servlets.webservices.JSONResponse;
//import ru.biosoft.server.servlets.webservices.providers.WebDiagramsProvider;
import ru.biosoft.server.servlets.webservices.providers.WebJSONProviderSupport;
import ru.biosoft.util.TempFiles;

public class WDLWebProvider extends WebJSONProviderSupport
{
    private static final String DIAGRAM_TO_WDL = "diagram2wdl";
    private static final String WDL_TO_DIAGRAM = "wdl2diagram";
    private static final String RUN_WDL = "run";

    String outputDir = TempFiles.path( "nextflow" ).getAbsolutePath();

    @Override
    public void process(BiosoftWebRequest arguments, JSONResponse response) throws Exception
    {
        
        String action = arguments.getAction();
        if( DIAGRAM_TO_WDL.equals( action ) )
        {
            DataElementPath diagramPath = arguments.getDataElementPath();
            Diagram diagram = WebDiagramsProvider.getDiagram( diagramPath.toString(), false );
            String wdl = new WDLGenerator().generateWDL( diagram );
            String nextflow = new NextFlowGenerator().generateNextFlow( diagram );
            JSONObject res = new JSONObject();
            res.put( "wdl", wdl );
            res.put( "nextflow", nextflow );
            response.sendJSON( res );

        }
        else if( WDL_TO_DIAGRAM.equals( action ) )
        {
            DataElementPath diagramPath = arguments.getDataElementPath();
            Diagram diagram = WebDiagramsProvider.getDiagram( diagramPath.toString(), false );
            String text = arguments.get( "wdl" );
            text = text.replace( "<<<", "{" ).replace( ">>>", "}" );//TODO: fix parsing <<< >>>
            AstStart start = new WDLParser().parse( new StringReader( text ) );
            WDLImporter wdlImporter = new WDLImporter();
            diagram = wdlImporter.generateDiagram( start, diagram );
            wdlImporter.layout( diagram );
            diagramPath.save( diagram );
            OutputStream out = response.getOutputStream();
            WebDiagramsProvider.sendDiagramChanges( diagram, out, "json" );
        }
        else if( RUN_WDL.equals( action ) )
        {
            DataElementPath diagramPath = arguments.getDataElementPath();
            Diagram diagram = WebDiagramsProvider.getDiagram( diagramPath.toString(), false );
            WorkflowSettings settings = new WorkflowSettings();
            settings.initParameters( diagram );
            JSONArray jsonSettings = arguments.getJSONArray( "settings" );
            JSONUtils.correctBeanOptions( settings, jsonSettings );
            try
            {
                WDLRunner.runNextFlow( diagram, settings, outputDir, false );
                response.sendString( settings.getOutputPath().toString() );
            }
            catch (Exception e)
            {
                log.log( Level.SEVERE, e.getMessage() );
                response.error( e.getMessage() );
            }
        }
    }
}
