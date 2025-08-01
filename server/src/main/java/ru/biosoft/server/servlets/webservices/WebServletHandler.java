package ru.biosoft.server.servlets.webservices;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.fileupload2.core.DiskFileItemFactory;
import org.apache.commons.fileupload2.core.FileItem;
import org.apache.commons.fileupload2.jakarta.servlet6.JakartaServletFileUpload;

import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import ru.biosoft.access.core.Environment;
import ru.biosoft.util.TextUtil2;

public class WebServletHandler
{

    private final String sessionId = "";
    private final static WebServicesServlet servlet = new WebServicesServlet();
    private final static SystemSession session = new SystemSession();

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
        String sessionId = "";

        if( request.getCookies() != null )
        {
            for ( Cookie cookie : request.getCookies() )
            {
                if( cookie.getName().equals("JSESSIONID") )
                    sessionId = cookie.getValue();
            }
        }
        //TODO: compare with some registered session
        //        if( !sessionId.equals(this.sessionId) )
        //        {
        //            sendForbidden(response);
        //            return;
        //        }
        String target = request.getRequestURI();
        Object docRootObj = Environment.getValue( "DocRoot" );
        String docRoot = docRootObj != null ? docRootObj.toString() : "";
        if( !target.startsWith(docRoot) )
        {
            sendNotFound(response, target);
            return;
        }
        
        
        int pos = target.indexOf('?');
        Map<String, String[]> uriParameters = request.getParameterMap();
        
        final String subTarget = target.substring(1);
        final Map<String, Object> arguments = new HashMap<>();
        response.setStatus(HttpServletResponse.SC_OK);
        
        final ServletOutputStream out = response.getOutputStream();//new ByteArrayOutputStream();
        try
        {
            if( JakartaServletFileUpload.isMultipartContent(request) )
            {

                DiskFileItemFactory factory = DiskFileItemFactory.builder().get();
                JakartaServletFileUpload upload = new JakartaServletFileUpload(factory);
                //upload.setFileSizeMax(MAX_FILE_SIZE);
                //upload.setSizeMax(MAX_REQUEST_SIZE);
//                String uploadPath = request.getServletContext().getRealPath("") + File.separator + UPLOAD_DIRECTORY;
//                File uploadDir = new File(uploadPath);
//                if (!uploadDir.exists()) {
//                    uploadDir.mkdir();
//                }

                try {
                    List<FileItem> formItems = upload.parseRequest(request);

                    if (formItems != null && formItems.size() > 0) {
                        for (FileItem item : formItems) {
                            if( item.isFormField() )
                            {
                                String name = item.getFieldName();
                                String value = item.getString();
                                arguments.put(name, new String[] { value });
                            }
                            else
                            {
                                String name = item.getFieldName();
                                arguments.put(name, new FileItem[] { item });
                            }
                            //                            if (!item.isFormField()) {
                            //                                String fileName = new File(item.getName()).getName();
                            //                                item.write(Path.of(uploadPath, fileName));
                            //                                request.setAttribute("message", "File " + fileName + " has uploaded successfully!");
                            //                            }
                        }
                    }
                } catch (Exception ex) {
                    //request.setAttribute("message", "There was an error: " + ex.getMessage());
                }
                //getServletContext().getRequestDispatcher("/result.jsp").forward(request, response);
            }
            else
            {
                //???uriParameters.addAll(Arrays.asList(TextUtil.split(new String(EntityUtils.toByteArray(entity)), '&')));
            }
            for ( String uriParameter : uriParameters.keySet() )
            {

                arguments.put(TextUtil2.decodeURL(uriParameter), uriParameters.get(uriParameter));
            }
            if( extraArguments != null )
            {
                arguments.putAll(extraArguments);
            }

            //TODO: modified
            //servlet.service(subTarget, session, arguments, out, new ServerHttpResponseWrapper(response));
            servlet.service(subTarget, session, arguments, out, response);

        }
        catch (Throwable t)
        {
            new JSONResponse(out).error(t);
        }

        //TODO: did the out of the response send fully?
        //        response.setEntity(new ByteArrayEntity(out.toByteArray()));
        response.flushBuffer();

    }

    private static void sendForbidden(HttpServletResponse response) throws IOException
    {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        String forbiddenMessage = "<html><body><h1>Access to this server is forbidden</h1></body></html>";
        response.setContentType("text/html; charset=UTF-8");
        response.getWriter().println(forbiddenMessage);
    }

    private static void sendNotFound(final HttpServletResponse response, final String target) throws IOException
    {
        response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        String forbiddenMessage = "<html><body><h1>The requested URL " + target + " is not found</h1></body></html>";
        response.setContentType("text/html; charset=UTF-8");
        response.getWriter().println(forbiddenMessage);
    }
}
