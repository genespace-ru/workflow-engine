package converter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import org.apache.commons.fileupload2.core.DiskFileItemFactory;
import org.apache.commons.fileupload2.core.FileItem;
import org.apache.commons.fileupload2.jakarta.servlet6.JakartaServletFileUpload;
import org.json.JSONArray;
import org.json.JSONObject;

import biouml.model.Diagram;
import biouml.plugins.wdl.nextflow.NextFlowGenerator;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import ru.biosoft.server.servlets.webservices.BiosoftWebResponse;
import ru.biosoft.server.servlets.webservices.JSONResponse;
import ru.biosoft.util.ApplicationUtils;
import ru.biosoft.util.TempFiles;
import ru.biosoft.util.TextUtil2;
import ru.biosoft.util.archive.ArchiveFactory;

public class ConverterAPI
{
    protected static Map<Long, File> idToFolder = new ConcurrentHashMap<>();
    private static AtomicLong id = new AtomicLong();
    public static final String UPLOAD_DIRECTORY = System.getProperty( "biouml.upload_dir", System.getProperty("java.io.tmpdir"));
    
    public static void handle(final HttpServletRequest request, final HttpServletResponse response, String method) throws IOException
    {
        handle(request, response, method, null);
    }
    
    public static void handle(final HttpServletRequest request, final HttpServletResponse response, String method, Map<String, Object> extraArguments) throws IOException
    {
        if( !method.equals("GET") && !method.equals("HEAD") && !method.equals("POST") )
        {
            throw new IllegalArgumentException(method + " method not supported");
        }

        Map<String, String[]> uriParameters = request.getParameterMap();
        
        final Map<String, Object> arguments = new HashMap<>();
        

        File fileToConvert = null;
        
        for ( String uriParameter : uriParameters.keySet() )
        {
            arguments.put( TextUtil2.decodeURL( uriParameter ), uriParameters.get( uriParameter ) );
        }

        if( JakartaServletFileUpload.isMultipartContent( request ) )
        {
            DiskFileItemFactory factory = DiskFileItemFactory.builder().get();
            JakartaServletFileUpload upload = new JakartaServletFileUpload( factory );
            File uploadDir = getUploadFolder();
            try
            {
                List<FileItem> formItems = upload.parseRequest( request );
                if( formItems != null && formItems.size() > 0 )
                {
                    for ( FileItem item : formItems )
                    {
                        if( !item.isFormField() && !item.getName().isEmpty() )
                        {
                            String fileName = new File( item.getName() ).getName();
                            File destinationFile = new File( uploadDir, fileName );
                            item.write( destinationFile.toPath() );
                            fileToConvert = destinationFile;
                        }
                        else
                        {
                            String name = item.getFieldName();
                            String value = item.getString();
                            arguments.put( name, value );
                        }
                    }
                }
            }
            catch (Exception ex)
            {
                sendError( response, "Error parsing input arguments: " + ex.getMessage() );
                return;
            }

            // Check for folderId after parsing multipart content
            if( arguments.get( "folderId" ) != null )
            {
                String folderId = (String) arguments.get( "folderId" );
                File parentFolder = idToFolder.get( Long.parseLong( folderId ) );
                if( parentFolder == null )
                {
                    sendError( response, "Incorrect upload id, please, upload archived file again" );
                    return;
                }
                String location = (String) arguments.get( "location" );
                fileToConvert = new File( parentFolder, location );
            }
            else if( fileToConvert == null && arguments.get( "inputWdlText" ) != null )
            {
                File dir = TempFiles.getTempDirectory();

                fileToConvert = new File( dir, "input.wdl" );
                ApplicationUtils.writeString( fileToConvert, arguments.getOrDefault( "inputWdlText", "" ).toString() );
            }
            File archivedFolder = TempFiles.dir( fileToConvert.getName() + "_unpacked" );
            try
            {
                ArchiveFactory.unpack( fileToConvert, archivedFolder );
                Path dir = archivedFolder.toPath();
                List<String> files = collectWdlFiles( dir, dir );
                BiosoftWebResponse resp = new BiosoftWebResponse( response, response.getOutputStream() );
                JSONResponse jsonResp = new JSONResponse( resp );
                if( !files.isEmpty() )
                {
                    JSONObject res = new JSONObject();
                    Long idl = id.incrementAndGet();
                    res.put( "folderId", idl.toString() );
                    idToFolder.put( idl, archivedFolder );
                    JSONArray array = new JSONArray( files );
                    res.put( "files", array );
                    response.setStatus( HttpServletResponse.SC_OK );
                    response.setHeader( "Access-Control-Allow-Origin", "*" );
                    response.setHeader( "Access-Control-Allow-Credentials", "true" );
                    response.setHeader( "Access-Control-Allow-Methods", "POST, GET" );
                    response.setHeader( "Access-Control-Allow-Headers", "Content-Type" );
                    response.setContentType( "application/json" );
                    jsonResp.sendJSON( res );
                }
                else
                {
                    jsonResp.error( "No wdl files in supplied archive " + fileToConvert.getName() );
                }
                return;

            }
            catch (IllegalArgumentException e)
            {
                //not an archive
            }
            catch (IOException e)
            {
                sendError( response, "There was an error during unpack: " + e.getMessage() );
                return;
            }
            catch (Exception e)
            {
                sendError( response, "There was an error: " + e.getMessage() );
                return;
            }

        }
        else
        {
            sendError( response, "Incorrect form data" );
            return;
        }

        if( fileToConvert != null )
        {
            boolean isMultiple = false;
            JSONObject result = new JSONObject();
            Map<String, Diagram> diagrams = null;
            Diagram diagram = null;
            try
            {
                diagrams = Converter.loadDiagrams( fileToConvert.getAbsolutePath() );
                if( diagrams.isEmpty() )
                {
                    sendError( response, "No diagrams were loaded from wdl" );
                    return;
                }
                String mainDiagramName = Converter.getDiagramName( fileToConvert.getAbsolutePath() );
                if( diagrams.size() == 1 )
                    diagram = diagrams.entrySet().iterator().next().getValue();
                else
                {
                    isMultiple = true;
                    if( diagrams.containsKey( mainDiagramName ) )
                        diagram = diagrams.get( mainDiagramName );
                }

                //diagram = Converter.loadDiagram( fileToConvert.getAbsolutePath() );
            }
            catch (Exception ex1)
            {
                sendError( response, "Error loading diagram from wdl: " + ex1.getMessage() );
                return;
            }
            List<String> convertErrors = new ArrayList<>();
            if( diagram != null )
            {
                File convertedFile = TempFiles.file( "export_image.png" );
                try (FileOutputStream fos = new FileOutputStream( convertedFile ))
                {
                    Converter.exportImage( diagram, convertedFile );
                    byte[] imageBytes = Files.readAllBytes( convertedFile.toPath() );
                    result.put( "diagram", Base64.getEncoder().encodeToString( imageBytes ) );
                }
                catch (Exception ex1)
                {
                    convertErrors.add( "Error writing diagram image: " + ex1.getMessage() );
                }
                finally
                {
                    convertedFile.delete();
                }
                //TODO: uncomment if diagram xml is required
                //            try (ByteArrayOutputStream baos = new ByteArrayOutputStream())
                //            {
                //                DiagramXmlWriter writer = diagram.getType().getDiagramWriter();
                //                writer.setStream( baos );
                //                writer.write( diagram );
                //                String diagramStr = baos.toString( StandardCharsets.UTF_8 );
                //                result.put( "diagram_xml", diagramStr );
                //            }
                //            catch (Exception ex1)
                //            {
                //                convertErrors.add( "Error writing diagram file: " + ex1.getMessage() );
                //            }

                try
                {
                    String nextFlow = new NextFlowGenerator().generate( diagram );
                    result.put( "nextflow", nextFlow );
                }
                catch (Exception ex1)
                {
                    convertErrors.add( "Error converting to nextflow: " + ex1.getMessage() );
                }
            }
            
            if( isMultiple )
            {
                Long conversionId = id.incrementAndGet();
                result.put( "downloadId", conversionId.toString() );
                File convertedFolder = TempFiles.dir( "converted_" + conversionId );
                idToFolder.put( conversionId, convertedFolder );
                File diagramsFolder = new File( convertedFolder, "diagrams" );
                diagramsFolder.mkdir();
                File nextflowsFolder = new File( convertedFolder, "nextflow" );
                nextflowsFolder.mkdir();
                for ( String name : diagrams.keySet() )
                {
                    Diagram current = diagrams.get( name );
                    File convertedFile = new File( diagramsFolder, name );
                    File nextflowFile = new File( nextflowsFolder, name + ".nf" );
                    try
                    {
                        Converter.exportImage( current, convertedFile );
                        String nextFlow = new NextFlowGenerator().generate( current );
                        ApplicationUtils.writeString( nextflowFile, nextFlow );
                    }
                    catch (Exception ex1)
                    {
                        convertErrors.add( "Error converting: " + ex1.getMessage() );
                    }
                }

            }

            if( !convertErrors.isEmpty() )
            {
                if( !result.has( "diagram" ) && !result.has( "nextflow" ) )
                {
                    sendError( response, convertErrors.stream().collect( Collectors.joining( "\n" ) ) );
                    return;
                }
                else
                {
                    result.put( "error", convertErrors.stream().collect( Collectors.joining( "\n" ) ) );
                }
            }
            response.setStatus( HttpServletResponse.SC_OK );
            response.setHeader( "Access-Control-Allow-Origin", "*" );
            response.setHeader( "Access-Control-Allow-Credentials", "true" );
            response.setHeader( "Access-Control-Allow-Methods", "POST, GET" );
            response.setHeader( "Access-Control-Allow-Headers", "Content-Type" );
            response.setContentType( "application/json" );

            BiosoftWebResponse resp = new BiosoftWebResponse( response, response.getOutputStream() );
            JSONResponse jsonResp = new JSONResponse( resp );
            jsonResp.sendJSON( result );
        }
        else
        {
            sendError( response, "Nothing to convert" );
        }
    }
    
    private static List<String> collectWdlFiles(Path base, Path dir) throws IOException
    {
        List<String> result = new ArrayList<>();
            Files.walkFileTree( base, new SimpleFileVisitor<Path>()
            {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException
                {
                    if( dir.getFileName().toString().equals( ".git" ) )
                    {
                        return FileVisitResult.SKIP_SUBTREE;
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException
                {
                    if( file.getFileName().toString().endsWith( ".wdl" ) )
                    {
                        result.add( dir.relativize( file ).toString() );
                    }
                    return FileVisitResult.CONTINUE;
                }
            } );
        return result;
    }

    protected static void sendError(final HttpServletResponse response, final String errorMessage) throws IOException
    {
        response.setHeader("Access-Control-Allow-Origin", "*");
        response.setStatus( HttpServletResponse.SC_BAD_REQUEST );
        new JSONResponse(new BiosoftWebResponse(response, response.getOutputStream())).error(errorMessage);
    }
    
    private static synchronized File getUploadFolder()
    {
        SimpleDateFormat df = new SimpleDateFormat( "yyyyMMddHHmmssSSS" );
        while(true)
        {
            String name = "upload_wdl_" + df.format( new Date() );
            File tempFile = new File(UPLOAD_DIRECTORY, name);
            if(!tempFile.exists())
            {
                tempFile.mkdir();
                return tempFile;
            }
            try
            {
                Thread.sleep( 1 );
            }
            catch( InterruptedException e )
            {
                // ignore
            }
        }
    }
}
