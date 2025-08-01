package ru.biosoft.util;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.beans.SimpleBeanInfo;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.concurrent.ConcurrentHashMap;

import javax.swing.ImageIcon;

import java.util.Arrays;
import java.util.List;

import java.util.logging.Level;
import java.util.logging.Logger;
//import org.eclipse.core.runtime.Platform;
//import org.osgi.framework.Bundle;

//import com.developmentontheedge.application.Application;
import com.developmentontheedge.beans.DynamicProperty;
import com.developmentontheedge.beans.Preferences;

import ru.biosoft.access.ClassLoading;
import ru.biosoft.exception.InternalException;
import ru.biosoft.jobcontrol.JobControl;
import ru.biosoft.util.entry.RegularFileEntry;
//import ru.biosoft.util.entry.BundleEntry;
import ru.biosoft.access.core.PluginEntry;
//import ru.biosoft.util.entry.RegularFileEntry;

public class ApplicationUtils
{
    private static final String THREADS_NUMBER_PREFERENCE = "threadsNumber";
    private static final String MAX_SORTING_SIZE_PREFERENCE = "maxSortingSize";

    public static final int DEFAULT_MAX_SORTING_SIZE = 100000;

    static Logger log = Logger.getLogger( ApplicationUtils.class.getName() );

    static public ImageIcon getImageIcon(URL url)
    {
        if( url == null )
            return null;

        ImageIcon imageIcon = new ImageIcon(url);
        return imageIcon;
    }

    static Map<String, ImageIcon> imageMap = new ConcurrentHashMap<>();

    static public ImageIcon getImageIcon(String imagename)
    {
        ImageIcon imageIcon = imageMap.get(imagename);

        if( imageIcon != null )
            return imageIcon;

        int idx = imagename.indexOf(':');
        if( idx > 2 )
        {
            String pluginName = imagename.substring(0, idx);
            log.fine( "Loading image from plugin " + pluginName );
            String resource = imagename.substring(idx + 1);

            URL url = ApplicationUtils.class.getClassLoader().getResource(resource);
            if( url != null )
            {
                imageIcon = getImageIcon(url);
                imageMap.put(imagename, imageIcon);
                return imageIcon;
            }
            //            else if( resource.lastIndexOf("/") > -1 )//TODO: try only by name since maven copy all resources in target folders 
            //            {
            //                String resourceName = resource.substring(resource.lastIndexOf("/") + 1);
            //                url = ApplicationUtils.class.getClassLoader().getResource(resourceName);
            //                if( url != null )
            //                {
            //                    imageIcon = getImageIcon(url);
            //                    imageMap.put(imagename, imageIcon);
            //                    return imageIcon;
            //                }
            //            }

            //            if(pluginName.equals("default"))
            //            {
            //                URL url = ApplicationUtils.class.getClassLoader().getResource(resource);
            //                if( url != null )
            //                {
            //                    imageIcon = getImageIcon(url);
            //                    imageMap.put(imagename, imageIcon);
            //                    return imageIcon;
            //                }
            //            }
            //            Bundle bundle = null;
            //            try
            //            {
            //                bundle = Platform.getBundle(pluginName);
            //            }
            //            catch( Throwable t )
            //            {
            //                log.log( Level.SEVERE, "can not load plugin", t );
            //            }
            //            if( bundle != null )
            //            {
            //                log.fine( "Loading image from bundle " + bundle );
            //                int idx2 = resource.indexOf("?");
            //                if( idx2 != -1 ) // Probably it's CustomImageLoader
            //                {
            //                    try
            //                    {
            //                        String className = resource.substring(0, idx2);
            //                        CustomImageLoader imageLoader = (CustomImageLoader)ClassLoading.loadClass(className).newInstance();
            //                        String path = resource.substring(idx2 + 1);
            //                        imageIcon = imageLoader.loadImage(pluginName + ":" + path);
            //                        if( imageIcon != null )
            //                            imageMap.put(imagename, imageIcon);
            //                        return imageIcon;
            //                    }
            //                    catch( Exception e )
            //                    {
            //                        log.log( Level.WARNING, "can not load image from resource: " + resource, e );
            //                    }
            //                }
            //                URL url = bundle.getResource(resource);
            //                if( url != null )
            //                {
            //                    imageIcon = getImageIcon(url);
            //                    imageMap.put(imagename, imageIcon);
            //                    return imageIcon;
            //                }
            //            }
        }

        URL url = ClassLoader.getSystemResource(imagename);

        if( url != null )
        {
            imageIcon = getImageIcon(url);
            imageMap.put(imagename, imageIcon);
            return imageIcon;
        }

        SimpleBeanInfo sbi = new SimpleBeanInfo();
        Image img = sbi.loadImage(imagename);

        if( img != null )
        {

            imageIcon = new ImageIcon(img);
            imageMap.put(imagename, imageIcon);
            return imageIcon;
        }
        
        //In some cases we try to get image by the path to file. Here we check if such file exists 
        //However not sure what should happened if such file does not exist  or is of wrong type
        //TODO: do something in that regard
        if( !new File( imagename ).exists() )
        {   
            log.log( Level.SEVERE, "Image file doesn't exists: " + imagename, new Exception() );
        }   
        imageIcon = new ImageIcon( imagename );
        imageMap.put(imagename, imageIcon);
        return imageIcon;
    }

    static public ImageIcon getImageIcon(String basePath, String name)
    {
        return name.indexOf(':') != -1 ? getImageIcon(name) : getImageIcon(basePath + File.separator + name);
    }

    static public void moveToCenter(Component f)
    {
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        f.setLocation(screenSize.width / 2 - f.getSize().width / 2, screenSize.height / 2 - f.getSize().height / 2);
    }

    public static String getFileNameWithoutExtension(String fileName)
    {
        int whereDot = fileName.lastIndexOf('.');
        if( 0 < whereDot && whereDot <= fileName.length() - 2 )
        {
            return fileName.substring(0, whereDot);
        }
        return fileName;
    }
    //TODO: commented, Platform
    //    public static URL getResourceURL(String pluginName, String fileName)
    //    {
    //        try
    //        {
    //            Bundle plugin = Platform.getBundle(pluginName);
    //            if(plugin == null)
    //            {
    //                return new File("../plugconfig/"+pluginName, fileName).toURI().toURL();
    //            }
    //            return new URL(plugin.getEntry( "/" ), fileName);
    //        }
    //        catch( MalformedURLException e )
    //        {
    //            throw new InternalException( e );
    //        }
    //    }

    //TODO: commented, not used in BioUML
    //    @Deprecated
    //    public static File getPluginPath(String pluginName)
    //    {
    //        Bundle plugin = Platform.getBundle(pluginName);
    //        if(plugin == null)
    //        {
    //            return new File("../plugins/"+pluginName+"_"+"0.9.8");
    //        }
    //        String bundle = plugin.getLocation();
    //        String path = bundle.substring(bundle.indexOf("@") + 1, bundle.lastIndexOf("/")).replace('/', File.separatorChar);
    //        String home = System.getProperty("biouml.server.path");
    //        if( home == null )
    //            home = System.getProperty("user.dir");
    //        return new File(home, path);
    //    }
    /**
     * Resolves path to the plugin file resource
     * 
     * @param pluginPath path like "ru.biosoft.access:resource"
     * @return File object pointing to the resource
     */
    public static PluginEntry resolvePluginPath(String pluginPath)
    {
        return resolvePluginPath(pluginPath, "");
    }

    public static PluginEntry resolvePluginPath(String pluginPath, String parentPath)
    {

        if( pluginPath == null )
            return null;
        int colonPos = pluginPath.indexOf(':');
        if( colonPos < 3 )
        {
            File f = parentPath.isEmpty() ? new File(pluginPath) : new File(parentPath, pluginPath);
            return new RegularFileEntry(f);
        }
        String pluginName = pluginPath.substring(0, colonPos);
        String path = pluginPath.substring(colonPos + 1);
        //TODO: commented, Platform, Bundle
        //            Bundle plugin = Platform.getBundle(pluginName);
        //            if(plugin == null)
        //            {
        //                if(new File("../plugconfig").exists())
        //                    return new RegularFileEntry(new File(new File("../plugconfig", pluginName), path));
        //                else
        return new RegularFileEntry(new File(pluginPath));
        //            }
        //            path = "/" + path;
        //            if( plugin.getEntryPaths( path ) != null )
        //            {
        //                path += "/";
        //            }
        //            return new BundleEntry( plugin, path );
}

    public static int getMaxSortingSize()
    {
        int maxSortingSize = DEFAULT_MAX_SORTING_SIZE;
        try
        {
            Preferences preferences = ServerPreferences.getPreferences();
            DynamicProperty property = preferences.getProperty(MAX_SORTING_SIZE_PREFERENCE);
            if( property == null || property.getValue().equals(0) )
            {
                property = new DynamicProperty(MAX_SORTING_SIZE_PREFERENCE, "Max sorting size", "Maximum size of the table which supports sorting", Integer.class,
                        DEFAULT_MAX_SORTING_SIZE);
                preferences.add(property);
            }
            maxSortingSize = (Integer) property.getValue();
        }
        catch (Exception e)
        {
        }
        return maxSortingSize;
    }

    public static int getPreferredThreadsNumber()
    {
        int nThreads = 0;

        Preferences preferences = ServerPreferences.getPreferences();

        if( preferences != null )
        {
            DynamicProperty property = preferences.getProperty(THREADS_NUMBER_PREFERENCE);
            if( property == null )
            {
                property = new DynamicProperty(THREADS_NUMBER_PREFERENCE, "Preferred number of threads", "Set 0 for number of processors available", Integer.class, 0);
                preferences.add(property);
            }

            try
            {
                nThreads = (Integer) property.getValue();
            }
            catch (Exception e)
            {
                property.setValue(0);
            }
        }

        if( nThreads <= 0 )
        {
            nThreads = Runtime.getRuntime().availableProcessors();
        }
        return nThreads;
    }

    public static String detectEncoding(File file) throws Exception
    {
        String[] Styles={"Cp1251", "UTF-8", "UTF-16", "US-ASCII", "ISO-8859-1"};
        for (String style: Styles)
        {
            String line="";
            int ind=0;
            StringBuilder sb = new StringBuilder();
            try (BufferedReader br = new BufferedReader( new InputStreamReader( new FileInputStream( file ), style ) ))
            {
                while( ( line = br.readLine() ) != null && ( ind < 20 ) )
                {
                    sb.append( line );
                    ind++;
                }
            }
            String text = sb.toString();
            int lowCode=2, upCode=150, sum=0;
            long num=Math.round(0.9*text.length());
            for (int i=0;i<text.length();i++)
            {
                if (text.charAt(i)==0) break;
                if ((text.charAt(i)<upCode)&&(text.charAt(i)>lowCode))
                {
                    sum++;
                }
            }
            if (sum>num)
                return style;
        }
        throw new RuntimeException("The code style of text from file can not be recognized");
    }

    public static String trimStackAsString( Throwable exc, int nLines )
    {
        StringBuilder sb = new StringBuilder();

        List<StackTraceElement> stackList = Arrays.asList( exc.getStackTrace() );
        if( stackList.size() > nLines )
        {
            stackList = stackList.subList( 0, nLines );
        }

        for( StackTraceElement stackEl : stackList )
        {
            sb.append( "     at " ).append( stackEl.toString() ).append( "\n" );
        }
        return sb.toString();
    }

    public static URL getImageURL(String imagePath)
    {
        int idx = imagePath.indexOf(':');
        if ( idx > 2 )
        {
            String pluginName = imagePath.substring(0, idx);
            log.fine("Loading image from plugin " + pluginName);
            String resource = imagePath.substring(idx + 1);
            if ( pluginName.equals("default") )
            {
                URL url = ApplicationUtils.class.getClassLoader().getResource(resource);
                if ( url != null )
                {
                    return url;
                }
            }
            //TODO: commented, Platform
            //            Bundle bundle = null;
            //            try
            //            {
            //                bundle = Platform.getBundle(pluginName);
            //            }
            //            catch (Throwable t)
            //            {
            //                log.log(Level.SEVERE, "can not load plugin", t);
            //            }
            //            if ( bundle != null )
            //            {
            //                log.fine("Loading image from bundle " + bundle);
            //                int idx2 = resource.indexOf("?");
            //
            //                URL url = bundle.getResource(resource);
            //                if ( url != null )
            //                {
            //                    return url;
            //                }
            //            }
            return null;
        }
        URL url = ClassLoader.getSystemResource(imagePath);
        return url;
    }

    //TODO: copy, code copied from com.developmentontheedge.application.ApplicationUtils 

    /**
     * Tries to create a hardlink first, copy if failed
     */
    public static void linkOrCopyFile(File dst, File src, JobControl jc) throws IOException
    {
        if( dst.getAbsolutePath().equals(src.getAbsolutePath()) )
        {
            if( jc != null )
                jc.setPreparedness(100);
            return;
        }
        try
        {
            dst.delete();
            Files.createLink(dst.toPath(), src.toPath());
            if( jc != null )
                jc.setPreparedness(100);
        }
        catch (Throwable e)
        {
            copyFile(dst, src, jc);
        }
    }

    //TODO: copy, code copied from com.developmentontheedge.application.ApplicationUtils
    public static void copyFile(File dst, File src, JobControl jc) throws IOException
    {
        try (FileInputStream source = new FileInputStream(src); FileOutputStream destination = new FileOutputStream(dst))
        {
            FileChannel sourceFileChannel = source.getChannel();
            FileChannel destinationFileChannel = destination.getChannel();

            long size = sourceFileChannel.size();
            long chunk = 10 * 1024 * 1024;
            for ( long pos = 0; pos < size; pos += chunk )
            {
                sourceFileChannel.transferTo(pos, Math.min(chunk, size - pos), destinationFileChannel);
                if( jc != null )
                {
                    jc.setPreparedness((int) (pos * 100 / size));
                    if( jc.getStatus() == JobControl.TERMINATED_BY_REQUEST )
                    {
                        return;
                    }
                }
            }
        }
    }

    //TODO: copy, code copied from com.developmentontheedge.application.ApplicationUtils
    public static String getRelativeFilePath(File parent, File file)
    {
        String relative = "";
        while ( !file.equals(parent) )
        {
            relative = file.getName() + (relative.isEmpty() ? "" : File.separator) + relative;
            file = file.getParentFile();
            if( file == null )
                return null;
        }
        return relative;
    }

    //TODO: copy, code copied from com.developmentontheedge.application.ApplicationUtils
    public static String readAsString(File src, int maxChars) throws IOException
    {
        char[] result = new char[maxChars];
        int offset = 0;
        try (FileInputStream in = new FileInputStream(src); InputStreamReader reader = new InputStreamReader(in, StandardCharsets.UTF_8))
        {
            while ( true )
            {
                int read = reader.read(result, offset, maxChars - offset);
                if( read == -1 )
                    return new String(result, 0, offset);
                offset += read;
                if( offset >= maxChars )
                    return new String(result);
            }
        }
    }

    //TODO: copy, code copied from com.developmentontheedge.application.ApplicationUtils
    /**
     * Reads input stream into UTF-8 string
     * 
     * @param src
     * @return
     * @throws IOException
     */
    public static String readAsString(File file) throws IOException
    {
        return readAsString(new FileInputStream(file));
    }

    //TODO: copy, code copied from com.developmentontheedge.application.ApplicationUtils
    /**
     * Reads input stream into UTF-8 string
     * 
     * @param src
     * @return
     * @throws IOException
     */
    public static String readAsString(InputStream src) throws IOException
    {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ApplicationUtils.copyStream(baos, src);
        return baos.toString("UTF-8");
    }

    //TODO: copy, code copied from com.developmentontheedge.application.ApplicationUtils
    public static void copyStream(OutputStream dst, InputStream src) throws IOException
    {
        final int BUFFER_SIZE = 64 * 1024;
        BufferedInputStream bis = null;
        BufferedOutputStream bos = null;
        try
        {
            bis = src instanceof BufferedInputStream ? (BufferedInputStream) src : new BufferedInputStream(src);
            bos = dst instanceof BufferedOutputStream ? (BufferedOutputStream) dst : new BufferedOutputStream(dst);

            byte[] buffer = new byte[BUFFER_SIZE];
            int len;

            while ( (len = bis.read(buffer)) != -1 )
            {
                bos.write(buffer, 0, len);
            }
        }
        finally
        {
            if( bis != null )
            {
                bis.close();
            }
            if( bos != null )
            {
                bos.flush();
                bos.close();
            }
        }
    }

    //TODO: copy, code copied from com.developmentontheedge.application.ApplicationUtils
    /**
     * Returns Graphics object which can be used for measuring font sizes This
     * works even if there's no application frame
     */
    private static Graphics2D graphics;

    static public Graphics2D getGraphics()
    {
        if( graphics == null )
        {
            //TODO: commented, Application
            //if( Application.getApplicationFrame() == null )
            graphics = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB).createGraphics();
            //            else
            //                graphics = (Graphics2D) Application.getApplicationFrame().getGraphics();
        }
        return graphics;
    }

    //TODO: copy, code copied from com.developmentontheedge.application.ApplicationUtils
    public static void removeDir(File dir)
    {
        if( dir == null || !dir.isDirectory() )
            return;
        File[] files = dir.listFiles();
        if( files != null )
        {
            for ( File file : files )
            {
                if( file.isDirectory() )
                {
                    removeDir(file);
                }
                if( !file.delete() )
                {
                    file.deleteOnExit();
                }
            }
        }
        if( !dir.delete() )
        {
            dir.deleteOnExit();
        }
    }

    //TODO: copy, code copied from com.developmentontheedge.application.ApplicationUtils
    public static void writeString(OutputStream dst, String str) throws IOException
    {
        ByteArrayInputStream is = new ByteArrayInputStream(str.getBytes(StandardCharsets.UTF_8));
        ApplicationUtils.copyStream(dst, is);
    }

    //TODO: copy, code copied from com.developmentontheedge.application.ApplicationUtils
    public static void writeString(File dst, String str) throws IOException
    {
        writeString(new FileOutputStream(dst), str);
    }

    //TODO: copy, code copied from com.developmentontheedge.application.ApplicationUtils
    public static String getCommonParent(String name1, String name2)
    {
        String delim = "/\\";
        StringTokenizer strTok1 = new StringTokenizer(name1, delim);
        StringTokenizer strTok2 = new StringTokenizer(name2, delim);
        StringBuffer buffer = new StringBuffer();
        while ( strTok1.hasMoreTokens() && strTok2.hasMoreTokens() )
        {
            String token1 = strTok1.nextToken();
            String token2 = strTok2.nextToken();
            if( token1.equals(token2) )
            {
                if( buffer.length() != 0 )
                    buffer.append("/");
                buffer.append(token1);
            }
        }
        return buffer.toString();
    }
}
