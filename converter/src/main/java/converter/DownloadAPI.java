package converter;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class DownloadAPI
{

    public static void handle(final HttpServletRequest request, final HttpServletResponse response, String method) throws IOException
    {
        handle( request, response, method, null );
    }

    public static void handle(final HttpServletRequest request, final HttpServletResponse response, String method, Map<String, Object> extraArguments) throws IOException
    {
        if( !method.equals( "GET" ) && !method.equals( "HEAD" ) && !method.equals( "POST" ) )
        {
            throw new IllegalArgumentException( method + " method not supported" );
        }
        response.setStatus( HttpServletResponse.SC_OK );

        String downloadId = request.getParameter( "downloadId" );

        if( downloadId != null )
        {
            File zipFolder = null;
            String name = null;
            Long downloadIdL = Long.parseLong( downloadId );
            if( ConverterAPI.idToFolder.containsKey( downloadIdL ) )
            {
                File fullFolder = ConverterAPI.idToFolder.get( downloadIdL );
                String type = request.getParameter( "type" );

                switch (type)
                {
                case "diagrams":
                    zipFolder = new File( fullFolder, "diagrams" );
                    name = "diagrams";
                    break;
                case "nextflow":
                    zipFolder = new File( fullFolder, "nextflow" );
                    name = "nextflow_scripts";
                    break;
                default:
                    zipFolder = fullFolder;
                    name = "workflow";
                }
            }
            else
            {
                ConverterAPI.sendError( response, "Cached files not found, please, try to upload again." );
                return;
            }

            response.setContentType( "application/zip" );
            response.setHeader( "Content-Disposition", "attachment; filename=\""+name+".zip\"" );
            zipFolderToStream( zipFolder, response.getOutputStream() );
        }
        else
            ConverterAPI.sendError( response, "Incorrect form data" );
        return;
    }

    private static void zipFolderToStream(File folder, OutputStream out) throws IOException
    {
        try (ZipOutputStream zos = new ZipOutputStream( out ))
        {
            Files.walkFileTree( folder.toPath(), new SimpleFileVisitor<Path>()
            {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    zos.putNextEntry( new ZipEntry( folder.toPath().relativize( file ).toString() ) );
                    Files.copy(file, zos);
                    zos.closeEntry();
                    return FileVisitResult.CONTINUE;
                 }
             });
        }
        
//        try (ZipOutputStream zos = new ZipOutputStream( out ))
//        {
//            Files.walk( folder.toPath() ).filter( Files::isRegularFile ).forEach( file -> {
//                try (FileInputStream fis = new FileInputStream( file.toFile() ))
//                {
//                    ZipEntry entry = new ZipEntry( folder.toPath().relativize( file ).toString() );
//                    zos.putNextEntry( entry );
//                    fis.transferTo( zos );
//                    zos.closeEntry();
//                }
//                catch (FileNotFoundException e)
//                {
//                }
//                catch (IOException e)
//                {
//                }
//            } );
//        }
    }

}
