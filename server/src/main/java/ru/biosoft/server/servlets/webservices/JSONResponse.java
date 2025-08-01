package ru.biosoft.server.servlets.webservices;

import java.awt.Dimension;
import java.awt.Rectangle;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;

import com.developmentontheedge.beans.DynamicPropertySet;

import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.exception.LoggedException;
import ru.biosoft.exception.ExceptionRegistry;
import ru.biosoft.server.Response;

//import ru.biosoft.server.servlets.webservices.providers.WebScriptsProvider.WebJSEnvironment;
//TODO:  modified to use org.json instead of com.eclipsesource.json
/**
 * Response for JSON
 */
public class JSONResponse extends Response
{
    protected static final Logger log = Logger.getLogger(JSONResponse.class.getName());

    public static final int TYPE_OK = 0;
    public static final int TYPE_ERROR = 1;
    public static final int TYPE_ADDITIONAL = 2;
    public static final int TYPE_INVALID = 3;

    public static final String ATTR_TYPE = "type";
    public static final String ATTR_MESSAGE = "message";
    public static final String ATTR_ERROR_CODE = "code";
    public static final String ATTR_ERROR_PARAMETERS = "parameters";
    public static final String ATTR_VALUES = "values";
    public static final String ATTR_DICTIONARIES = "dictionaries";
    public static final String ATTR_ACTIONS = "actions";
    public static final String ATTR_LEFT = "left";
    public static final String ATTR_TOP = "top";
    public static final String ATTR_WIDTH = "width";
    public static final String ATTR_HEIGHT = "height";
    public static final String ATTR_SIZE = "size";
    public static final String ATTR_REFRESH_AREA = "refreshArea";
    public static final String ATTR_TABLES = "tables";
    public static final String ATTR_IMAGES = "images";
    public static final String ATTR_HTML = "html";
    public static final String ATTR_STATUS = "status";
    public static final String ATTR_PERCENT = "percent";
    public static final String ATTR_EXPORTERS = "exporters";
    public static final String ATTR_COLUMNS = "columns";
    public static final String ATTR_FROM = "from";
    public static final String ATTR_TO = "to";
    public static final String ATTR_RESULTS = "results";
    public static final String ATTR_ATTRIBUTES = "attributes";

    public JSONResponse(BiosoftWebResponse resp)
    {
        this(resp.getOutputStream());
        resp.setContentType("application/json");
    }

    public JSONResponse(OutputStream os)
    {
        super(os, null);
    }

    private boolean jsonSent = false;
    @Override
    public void error(String message) throws IOException
    {
        if( jsonSent )
        {
            log.log(Level.SEVERE, "Attempting to send JSON twice: second attempt is ignored; message was " + message);
            return;
        }
        OutputStreamWriter ow = new OutputStreamWriter(os, "UTF8");
        new JSONObject().put(ATTR_TYPE, TYPE_ERROR).put(ATTR_MESSAGE, message).write(ow);
        ow.flush();
        jsonSent = true;
    }

    @Override
    public void error(Throwable t) throws IOException
    {
        if( jsonSent )
        {
            log.log(Level.SEVERE, "Attempting to send JSON twice: second attempt is ignored; error was " + t);
            return;
        }
        OutputStreamWriter ow = new OutputStreamWriter(os, "UTF8");
        JSONObject obj = new JSONObject();
        obj.put(ATTR_TYPE, TYPE_ERROR);
        obj.put(ATTR_MESSAGE, t.getMessage() == null ? "Internal server error: " + t.getClass().getName() : t.getMessage());
        if(t instanceof WebException)
        {
            obj.put(ATTR_ERROR_CODE, ((WebException) t).getId());
            JSONArray arr = new JSONArray();
            for( Object parameter : ( (WebException)t ).getParameters() )
            {
                arr.put(parameter == null ? "" : parameter.toString());
            }
            obj.put(ATTR_ERROR_PARAMETERS, arr);
        } else if(t instanceof LoggedException)
        {
            obj.put(ATTR_ERROR_CODE, ((LoggedException) t).getDescriptor().getCode());
        } else
        {
            obj.put(ATTR_ERROR_CODE, t.getClass().getName());
        }
        obj.write(ow);
        ow.flush();
        jsonSent = true;
    }

    @Override
    public void send(byte[] message, int format) throws IOException
    {
        OutputStreamWriter ow = new OutputStreamWriter(os, "UTF8");
        JSONObject obj = new JSONObject();
        obj.put(ATTR_TYPE, TYPE_OK);
        if( message != null )
        {
            String value = null;
            if( message.length == 1 )
            {
                value = String.valueOf(message[0]);
            }
            else
            {
                value = new String(message, "UTF-16BE");
            }
            obj.put(ATTR_VALUES, value);
        }
        obj.write(ow);
        ow.flush();
    }
    //TODO: commented
    //    public void sendJSON(JsonValue value) throws IOException
    //    {
    //        OutputStreamWriter ow = new OutputStreamWriter(os, "UTF8");
    //        new JSONObject().put( ATTR_TYPE, TYPE_OK ).put( ATTR_VALUES, value ).write( ow );
    //        ow.flush();
    //    }

    public void sendJSON(Object values) throws IOException
    {
        OutputStreamWriter ow = new OutputStreamWriter(os, "UTF8");
        try
        {
            JSONWriter writer = new JSONWriter(ow);
            writer.object()
                .key(ATTR_TYPE)
                .value(TYPE_OK)
                .key(ATTR_VALUES);
            if(values instanceof ByteArrayOutputStream)
            {
                ow.flush();
                ( (ByteArrayOutputStream)values ).writeTo(os);
                os.write('}');
            }
            else
            {
                writer.value(values);
                writer.endObject();
            }
        }
        catch( JSONException e )
        {
            throw ExceptionRegistry.translateException( e );
        }
        ow.flush();
    }

    public void sendString(String values) throws IOException
    {
        OutputStreamWriter ow = new OutputStreamWriter(os, "UTF8");
        new JSONObject().put(ATTR_TYPE, TYPE_OK).put(ATTR_VALUES, values).write(ow);
        ow.flush();
    }
    
    public void sendStringArray(String ... values) throws IOException
    {
        OutputStreamWriter ow = new OutputStreamWriter(os, "UTF8");
        JSONArray arr = new JSONArray();
        for(String val : values )
            arr.put(val);
        new JSONObject().put(ATTR_TYPE, TYPE_OK).put(ATTR_VALUES, arr).write(ow);
        ow.flush();
    }

    @Override
    public void sendDPSArray(DynamicPropertySet[] dpsArray) throws IOException
    {
        throw new UnsupportedOperationException( "DPS array mode not supported in JSON response anymore: use /web/lucene provider instead" );
    }

    public void sendActions(JSONObject[] actions) throws IOException
    {
        JSONArray actionsArray = new JSONArray();
        for ( JSONObject action : actions )
        {
            actionsArray.put(action);
        }
        OutputStreamWriter ow = new OutputStreamWriter(os, "UTF8");
        new JSONObject().put(ATTR_TYPE, TYPE_OK).put(ATTR_VALUES, actionsArray).write(ow);
        ow.flush();
    }

    public void sendSizeParameters(Dimension size, Rectangle refreshArea) throws IOException
    {
        JSONObject root = new JSONObject();
        root.put(ATTR_TYPE, TYPE_OK);
        if( size != null )
        {
            root.put(ATTR_SIZE, new JSONObject().put(ATTR_WIDTH, size.width).put(ATTR_HEIGHT, size.height));
        }
        if( refreshArea != null )
        {
            JSONObject areaObj = new JSONObject().put(ATTR_LEFT, refreshArea.x).put(ATTR_TOP, refreshArea.y).put(ATTR_WIDTH, refreshArea.width).put(ATTR_HEIGHT,
                    refreshArea.height);
            root.put(ATTR_REFRESH_AREA, areaObj);
        }

        OutputStreamWriter ow = new OutputStreamWriter(os, "UTF8");
        ow.write(root.toString());
        ow.flush();
    }

    //TODO: commented, WebJSEnvironment 
    //    public void sendEnvironment(WebJSEnvironment environment) throws IOException
    //    {
    //        JSONArray tables = new JSONArray();
    //        for( String tName : environment.getTables() )
    //        {
    //            tables.put(tName);
    //        }
    //
    //        JSONArray images = new JSONArray();
    //        for( String iName : environment.getImages() )
    //        {
    //            images.put(iName);
    //        }
    //
    //        JSONArray htmls = new JSONArray();
    //        for( String html : environment.getHTML() )
    //        {
    //            htmls.put(html);
    //        }
    //
    //        JSONObject root = new JSONObject().put(ATTR_TYPE, TYPE_OK).put(ATTR_VALUES, environment.getBuffer()).put(ATTR_TABLES, tables).put(ATTR_IMAGES, images).put(ATTR_HTML,
    //                htmls);
    //
    //        OutputStreamWriter ow = new OutputStreamWriter(os, "UTF8");
    //        ow.write(root.toString());
    //        ow.flush();
    //    }

    /**
     * Send bean with additional attributes object
     * @param jsonArray - bean properties
     * @param attributes - bean attributes
     * @throws IOException
     */
    public void sendJSONBean(JSONArray jsonArray, JSONObject attributes) throws IOException
    {
        JSONObject root = new JSONObject();
        try
        {
            root.put(ATTR_TYPE, TYPE_OK);
            root.put(ATTR_VALUES, jsonArray);
            if(attributes != null)
                root.put(ATTR_ATTRIBUTES, attributes);
        }
        catch( JSONException e )
        {
            log.log(Level.SEVERE, "JSON exception", e);
        }

        OutputStreamWriter ow = new OutputStreamWriter(os, "UTF8");
        try
        {
            root.write(ow);
        }
        catch( JSONException e )
        {
            throw new IOException("Unable to write JSON", e);
        }
        ow.flush();
    }

    public void sendJSONBean(JSONArray jsonArray) throws IOException
    {
        sendJSONBean(jsonArray, null);
    }

    public void sendStatus(int status, int percent, String... values) throws IOException
    {
        sendStatus(status, percent, null, values);
    }

    public void sendStatus(int status, int percent,  ru.biosoft.access.core.DataElementPath[] resultPaths, String... values) throws IOException
    {
        JSONObject root = new JSONObject();
        root.put(ATTR_TYPE, TYPE_OK);
        root.put(ATTR_STATUS, status);
        if( percent >= 0 )
        {
            root.put(ATTR_PERCENT, percent);
        }
        if( values != null )
        {
            JSONArray array = new JSONArray();
            for( String val : values )
                array.put(val.toString());
            root.put(ATTR_VALUES, array);
        }
        if( resultPaths != null )
        {
            JSONArray array = new JSONArray();
            for( DataElementPath path : resultPaths )
                array.put(path.toString());
            root.put(ATTR_RESULTS, array);
        }

        OutputStreamWriter ow = new OutputStreamWriter(os, "UTF8");
        ow.write(root.toString());
        ow.flush();
    }

    public void sendTableExportInfo(String exporters, String columns, int from, int to) throws IOException
    {
        OutputStreamWriter ow = new OutputStreamWriter(os, "UTF8");
        new JSONObject().put(ATTR_TYPE, TYPE_OK).put(ATTR_EXPORTERS, exporters).put(ATTR_COLUMNS, columns).put(ATTR_FROM, from).put(ATTR_TO, to).write(ow);
        ow.flush();
    }

    public void sendAdditionalJSON(JSONObject values) throws IOException
    {
        JSONObject root = new JSONObject();
        try
        {
            root.put(ATTR_TYPE, TYPE_ADDITIONAL);
            if(values != null) root.put(ATTR_VALUES, values);
        }
        catch( JSONException e )
        {
            log.log(Level.SEVERE, "JSON exception", e);
        }

        OutputStreamWriter ow = new OutputStreamWriter(os, "UTF8");
        try
        {
            root.write(ow);
        }
        catch( JSONException e )
        {
            throw new IOException("Unable to write JSON", e);
        }
        ow.flush();
    }

    public void sendInvalidResponse(String message) throws IOException
    {
        OutputStreamWriter ow = new OutputStreamWriter(os, "UTF8");
        new JSONObject().put(ATTR_TYPE, TYPE_INVALID).put(ATTR_MESSAGE, message).write(ow);
        ow.flush();
    }
}
