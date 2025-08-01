package biouml.workbench.perspective;

import java.util.Map;

//import org.eclipse.core.runtime.IConfigurationElement;
import org.json.JSONObject;

//import com.eclipsesource.json.JsonObject;

import ru.biosoft.access.core.DataElementPath;

/**
 * Stores information about single tab in repository tabs
 * @author lan
 */
public class RepositoryTabInfo
{
    private static final String DATABASES_ATTR = "databases";
    private static final String PATH_ATTR = "path";
    private static final String TITLE_ATTR = "title";
    private static final String HELP_ID_ATTR = "helpId";
    private static final String VIRTUAL_ATTR = "virtual";
    
    private final String title;
    private final String helpId;
    private final DataElementPath rootPath;
    private final boolean databasesTab;
    private final boolean virtualTab;
    
    protected RepositoryTabInfo()
    {
        this.title = "";
        this.helpId = "";
        this.rootPath = null;
        this.databasesTab = false;
        this.virtualTab = false;

    }

    protected RepositoryTabInfo(Map<String, Object> tab)
    {
        this.title = (String) tab.get(TITLE_ATTR);
        this.helpId = (String) tab.get(HELP_ID_ATTR);
        this.rootPath = DataElementPath.create((String) tab.get(PATH_ATTR));
        this.databasesTab = (Boolean) tab.getOrDefault(DATABASES_ATTR, false);
        this.virtualTab = (Boolean) tab.getOrDefault(VIRTUAL_ATTR, false);
    }

    //    protected RepositoryTabInfo(IConfigurationElement tab)
    //    {
    //        this.title = tab.getAttribute(TITLE_ATTR);
    //        this.rootPath = DataElementPath.create(tab.getAttribute(PATH_ATTR));
    //        String databasesTabStr = tab.getAttribute(DATABASES_ATTR);
    //        this.databasesTab = databasesTabStr != null && databasesTabStr.equalsIgnoreCase("true");
    //        String virtualTabStr = tab.getAttribute( VIRTUAL_ATTR );
    //        this.virtualTab = virtualTabStr != null && virtualTabStr.equalsIgnoreCase( "true" );
    //        this.helpId = tab.getAttribute(HELP_ID_ATTR);
    //    }
    
    /**
     * @return the human-readable title of the tab
     */
    public String getTitle()
    {
        return title;
    }
    
    /**
     * @return the root path
     */
    public DataElementPath getRootPath()
    {
        return rootPath;
    }
    
    /**
     * @return identifier to bind help
     */
    public String getHelpId()
    {
        return helpId;
    }

    /**
     * @return true if this tab is databases tab
     */
    public boolean isDatabasesTab()
    {
        return databasesTab;
    }
    
    public JSONObject toJSON()
    {
        JSONObject result = new JSONObject().put(TITLE_ATTR, title).put(PATH_ATTR, rootPath.toString()).put(DATABASES_ATTR, this.databasesTab).put(VIRTUAL_ATTR, this.virtualTab);
        if(this.helpId != null)
            result.put(HELP_ID_ATTR, this.helpId);
        return result;
    }
}
