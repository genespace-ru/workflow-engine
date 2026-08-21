package biouml.plugins.wdl.web;

import java.io.OutputStream;
import java.util.logging.Level;

import org.json.JSONArray;
import org.json.JSONObject;

import biouml.model.Diagram;
import biouml.model.server.WebDiagramsProvider;
import biouml.plugins.wdl.WDLGenerator;
import biouml.plugins.wdl.WorkflowSettings;
import biouml.plugins.wdl.cwl.CWLGenerator;
import biouml.plugins.wdl.diagram.WDLImporter;
import biouml.plugins.wdl.diagram.WDLLayouter;
import biouml.plugins.wdl.nextflow.NextFlowGenerator;
import biouml.plugins.wdl.nextflow.NextFlowRunner;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementImporter;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.server.JSONUtils;
import ru.biosoft.server.servlets.webservices.BiosoftWebRequest;
import ru.biosoft.server.servlets.webservices.JSONResponse;
import ru.biosoft.server.servlets.webservices.WebServicesServlet;
//import ru.biosoft.server.servlets.webservices.providers.WebDiagramsProvider;
import ru.biosoft.server.servlets.webservices.providers.WebJSONProviderSupport;
import ru.biosoft.util.FileItem;
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
            String wdl = new WDLGenerator().generate( diagram );
            String nextflow = new NextFlowGenerator().generate( diagram );
            String cwl = "";
            try
            {
             cwl = new CWLGenerator().generate( diagram );
            }
            catch (Exception ex)
            {
            	
            }
            JSONObject res = new JSONObject();
            res.put( "wdl", wdl );
            res.put( "nextflow", nextflow );
            res.put( "cwl", cwl );
            response.sendJSON( res );

        }
        else if( WDL_TO_DIAGRAM.equals( action ) )
        {
            DataElementPath diagramPath = arguments.getDataElementPath();
            Diagram diagram = WebDiagramsProvider.getDiagram( diagramPath.toString(), false );
            String text = arguments.get( "wdl" );
            WDLImporter wdlImporter = new WDLImporter();
            diagram = wdlImporter.generateDiagram( text, diagram );
            new WDLLayouter().layout( diagram );
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
                String log = NextFlowRunner.runNextFlowByDiagram( diagram, settings, outputDir, false );
                response.sendString( settings.getOutputPath().toString() );
            }
            catch (Exception e)
            {
                log.log( Level.SEVERE, e.getMessage() );
                response.error( e.getMessage() );
            }
        }
        else if( "import".equals( action ) )
        {

            String fileID = arguments.get( "fileID" );
            final FileItem file = WebServicesServlet.getUploadedFile( fileID );
            DataElement result = null;
            try
            {
                DataCollection<?> dc = arguments.getDataCollection();
                final String origFileName = file.getOriginalName();
                final String fileName = origFileName.replaceFirst( "\\.\\w+$", "" );
                DataElementImporter importer = new WDLImporter();
                importer.getProperties( dc, file, fileName );
                result = importer.doImport( dc, file, fileName, null, log );
                response.sendString( result.getCompletePath().toString() );
            }
            catch (Exception e)
            {
                log.log( Level.SEVERE, e.getMessage() );
                response.error( e.getMessage() );
            }
        }
    }
}
