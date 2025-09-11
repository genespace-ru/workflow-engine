package servlets;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.json.JSONArray;
import org.json.JSONObject;
import org.yaml.snakeyaml.Yaml;

import biouml.plugins.server.RepositoryManager;
import biouml.workbench.perspective.PerspectiveRegistry;
import biouml.workbench.perspective.YAMLPerspective;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebInitParam;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import ru.biosoft.access.AccessCoreInit;
import ru.biosoft.access.AccessInitializer;
import ru.biosoft.access.core.Environment;
import ru.biosoft.graphics.View;
import ru.biosoft.graphics.View.ModelResolver;
import ru.biosoft.graphics.access.DataElementModelResolver;
import ru.biosoft.server.ServerInitializer;
import ru.biosoft.server.servlets.webservices.WebServletHandler;
import ru.biosoft.templates.TemplatesInitializer;
import ru.biosoft.util.ServerPreferences;
import biouml.model.DiagramInitializer;
import biouml.plugins.wdl.WDLInitializer;
import biouml.model.DiagramTypes;

@WebServlet(urlPatterns = { "/diagrams/*" }, initParams = { @WebInitParam(name = "configPath", value = "config.yml") })
public class StartingServlet extends HttpServlet
{
    private static final long serialVersionUID = 1L;
    private String configPath;
    private Map<String, Object> yaml;

    @Override public void init(ServletConfig config) throws ServletException
    {
        super.init(config);
        AccessCoreInit.init();

        AccessInitializer.initialize();
        ServerInitializer.initialize();
        DiagramInitializer.initialize();
        DiagramTypes.addDiagramType( "biouml.plugins.wdl.diagram.WDLDiagramType" );
        WDLInitializer.initialize();
        TemplatesInitializer.initialize();

        configPath = config.getInitParameter("configPath");
        if( configPath == null )
            configPath = "config.yml";
        readConfigFile();
        if(yaml.get("repositories") != null)
        {
            Object repoObj = yaml.get("repositories");
            List<String> repos = (List<String>) yaml.get("repositories");
            try
            {
                RepositoryManager.initRepository(repos);
            }
            catch (Exception e)
            {
            }
        }

        if( yaml.get("preferences") != null )
        {
            ServerPreferences.loadPreferences((String) yaml.get("preferences"));
        }

        if( yaml.get("environment") != null )
        {
            Map<String, Object> environment = (Map<String, Object>) yaml.get("environment");
            for ( String propName : environment.keySet() )
            {
                Environment.setValue(propName, environment.get(propName));
            }
        }

        ModelResolver viewModelResolver = new DataElementModelResolver();
        View.setModelResolver(viewModelResolver);

        if( yaml.get("perspectives") != null )
        {
            try
            {
                List<Object> perspectives = (List<Object>) yaml.get("perspectives");
                for ( Object perspective : perspectives )
                {
                    if( perspective instanceof Map )
                    {
                        Map<String, Object> perspProps = (Map<String, Object>) ((Map) perspective).get("perspective");
                        String name = (String) perspProps.getOrDefault("name", "Unknown");
                        PerspectiveRegistry.registerPerspective(name, YAMLPerspective.class.getName(), perspProps);
                    }
                }
            }
            catch (Exception e)
            {
                // TODO: do nothing if can not init
            }
        }
    }


    @Override protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException
    {
        processRequest(request, response);
        //        response.setContentType("text/plain");
        //        response.getWriter().printf("Get query%n");
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException
    {
        processRequest(request, response);
    }

    protected void processRequest(HttpServletRequest request, HttpServletResponse response) throws IOException
    {
        String target = request.getRequestURI();
        if( target.startsWith( "/diagrams/web/check" ) )
        {
            PrintWriter ow = response.getWriter();
            response.setContentType("text/plain");
            response.setStatus(HttpServletResponse.SC_OK);
            JSONObject obj = new JSONObject();
            obj.put("type", "ok");
            obj.write(ow);
            ow.flush();
        }
        else if( target.startsWith( "/diagrams/web/parameter" ) )
        {
            String pName = request.getParameter("parameter_name");

            JSONArray vals = new JSONArray();
            if( yaml.get(pName) != null )
            {
                Object pValue = yaml.get(pName);
                if( pValue instanceof List )
                {
                    for ( String val : (List<String>) pValue )
                    {
                        vals.put(val);
                    }
                }
                else
                {
                    vals.put(pValue.toString());
                }

            }
            JSONObject responseObject = new JSONObject();
            responseObject.put(pName, vals);
            responseObject.put("type", "ok");
            PrintWriter ow = response.getWriter();
            response.setContentType("text/plain");
            response.setStatus(HttpServletResponse.SC_OK);
            responseObject.write(ow);
            ow.flush();
        }
        else if( target.startsWith( "/diagrams/web/login" ) )
        {
            Map<String, Object> extraParameters = null;
            if( yaml.get("roots") != null )
            {
                Object roots = yaml.get("roots");
                if( roots instanceof List )
                {
                    extraParameters = new HashMap<>();
                    extraParameters.put("repository", new String[] { ((List<String>) roots).stream().collect(Collectors.joining(";")) });
                }
            }
            WebServletHandler.handle(request, response, "POST", extraParameters);
        }
        else
            WebServletHandler.handle(request, response, "POST");
    }

    private void readConfigFile()
    {
        yaml = Collections.emptyMap();
        try
        {

            URL cfg = getClass().getClassLoader().getResource(configPath);
            if( cfg != null )
            {
                Path path = Paths.get(cfg.toURI());
                Yaml parser = new Yaml();

                Object root = parser.load(Files.readString(path));
                if( !(root instanceof Map) )
                    throw new IllegalArgumentException("Yaml should be a map of key-values, but get " + root);

                yaml = (Map<String, Object>) root;
            }
        }
        catch (IOException ioe)
        {

        }
        catch (URISyntaxException use)
        {

        }
    }

}
