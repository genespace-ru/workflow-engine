package converter;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Date;
import java.util.Map;

import org.apache.commons.fileupload2.core.DiskFileItemFactory;
import org.apache.commons.fileupload2.core.FileItem;
import org.apache.commons.fileupload2.jakarta.servlet6.JakartaServletFileUpload;

import biouml.model.util.DiagramXmlWriter;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import ru.biosoft.access.core.Environment;
import ru.biosoft.server.servlets.webservices.BiosoftWebResponse;
import ru.biosoft.server.servlets.webservices.JSONResponse;
import ru.biosoft.server.servlets.webservices.WebServicesServlet;
import ru.biosoft.util.TempFiles;
import ru.biosoft.util.TextUtil2;

import biouml.plugins.wdl.nextflow.NextFlowGenerator;
import biouml.model.Diagram;
import ru.biosoft.util.ApplicationUtils;

public class ConverterAPI
{
    
    public static final String UPLOAD_DIRECTORY = System.getProperty( "biouml.upload_dir", System.getProperty("java.io.tmpdir"));
    
    public void convert()
    {
        
    }
    
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
        response.setStatus(HttpServletResponse.SC_OK);
        
        File fileToConvert = null;
        
        final ServletOutputStream out = response.getOutputStream();//new ByteArrayOutputStream();
        try
        {
            if( JakartaServletFileUpload.isMultipartContent(request) )
            {

                DiskFileItemFactory factory = DiskFileItemFactory.builder().get();
                JakartaServletFileUpload upload = new JakartaServletFileUpload(factory);
                File uploadDir = getUploadFolder();

                try {
                    List<FileItem> formItems = upload.parseRequest(request);
                    if (formItems != null && formItems.size() > 0) {
                        for (FileItem item : formItems) {
                            if (!item.isFormField() && !item.getName().isEmpty()) {
                                String fileName = new File(item.getName()).getName();
                                File destinationFile = new File(uploadDir, fileName);
                                item.write(destinationFile.toPath());
                                fileToConvert =  destinationFile;
                            }
                            else
                            {
                                String name = item.getFieldName();
                                String value = item.getString();
                                arguments.put(name, value);
                            }
                        }
                    }
                } catch (Exception ex) {
                    sendError(response, "Error parsing input arguments: " + ex.getMessage());
                    return;
                }
                
                for ( String uriParameter : uriParameters.keySet() )
                {
                    arguments.put(TextUtil2.decodeURL(uriParameter), uriParameters.get(uriParameter));
                }
                if(fileToConvert == null && arguments.get( "inputWdlText" ) != null)
                {
                    File dir = TempFiles.getTempDirectory();
                    
                    fileToConvert = new File(dir, "input.wdl");
                    ApplicationUtils.writeString(fileToConvert, arguments.getOrDefault( "inputWdlText", "" ).toString());
                }
                if(fileToConvert != null)
                {
                    String convertType = arguments.getOrDefault( "convertType", "diagram" ).toString();
                    Diagram diagram = null;
                    try 
                    {
                        diagram = Converter.loadDiagram(fileToConvert.getAbsolutePath());
                    }
                    catch (Exception ex1)
                    {
                        sendError(response, "Error loading diagram from wdl: " + ex1.getMessage());
                        return;
                    }
                    File convertedFile = null;
                    String contentType = "text/xml";
                    String suffix = "";
                    if(convertType.equals( "diagram" )) //wdl to diagram
                    {
                        String outputType = arguments.getOrDefault( "outputType", "image" ).toString();
                        
                        if(outputType.equals( "image" )) //png image
                        {
                            suffix = ".png";
                            convertedFile = TempFiles.file("export_image"+suffix);
                            try (FileOutputStream fos = new FileOutputStream( convertedFile ))
                            {
                                Converter.exportImage(diagram, convertedFile);
                                contentType = "image/x-png";
                            }
                            catch(Exception ex1)
                            {
                                sendError(response, "Error writing diagram image: " + ex1.getMessage());
                                return;
                            }
                        }
                        else //plain diagram file
                        {
                            convertedFile = TempFiles.file("export_diagram");
                            try (FileOutputStream fos = new FileOutputStream( convertedFile ))
                            {
                                DiagramXmlWriter writer = diagram.getType().getDiagramWriter();
                                writer.setStream( fos );
                                writer.write( diagram );
                            }
                            catch(Exception ex1)
                            {
                                sendError(response, "Error writing diagram file: " + ex1.getMessage());
                                return;
                            }
                        }
                    }
                    else if(convertType.equals( "nextflow" ))
                    {
                        suffix = ".nf";
                        convertedFile = TempFiles.file("export_nextflow.nf");
                        try 
                        {
                            String nextFlow = new NextFlowGenerator().generate(diagram);
                            ApplicationUtils.writeString(convertedFile, nextFlow);
                        }
                        catch(Exception ex1)
                        {
                            sendError(response, "Error converting to nextflow: " + ex1.getMessage());
                            return;
                        }
                        
                    }
                    else
                    {
                        sendError(response, "Not supported convert format " + convertType);
                        return;
                    }
                    
                    if(convertedFile != null)
                    {
                        BiosoftWebResponse resp = new BiosoftWebResponse(response, response.getOutputStream());
                        String fileName = ApplicationUtils.getFileNameWithoutExtension(fileToConvert.getName());
                        resp.setHeader("Content-Disposition", "attachment;filename=\"" + fileName + suffix + "\"");
                        resp.setHeader("Content-Length", String.valueOf(convertedFile.length()));
                        resp.setHeader("Access-Control-Allow-Origin", "*");
                        resp.setHeader("Access-Control-Allow-Credentials", "true");
                        resp.setHeader("Access-Control-Allow-Methods", "POST, GET");
                        resp.setHeader("Access-Control-Allow-Headers", "Content-Type");
                        resp.setContentType(contentType);
                        ApplicationUtils.copyStream(resp.getOutputStream(), new FileInputStream(convertedFile));
                        convertedFile.delete();
                    }
                    else
                    {
                        sendError(response, "Can not convert file " + fileToConvert.getName());
                    }
                }            
                else
                {
                    sendError(response, "No input data to convert");
                }
            }
            else
            {
                sendError(response, "Incorrect form data");
            }
        }
        catch(Exception e)
        {
            sendError(response, "There was an error: " + e.getMessage());
        }
    }
    
    private static void sendError(final HttpServletResponse response, final String errorMessage) throws IOException
    {
        response.setHeader("Access-Control-Allow-Origin", "*");
        new JSONResponse(new BiosoftWebResponse(response, response.getOutputStream())).error(errorMessage);
        //response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, errorMessage);
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
