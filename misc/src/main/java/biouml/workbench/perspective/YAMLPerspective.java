package biouml.workbench.perspective;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Default implementation of Perspective in Genome Browser loaded by YAML
 * configuration
 * 
 * @author anna
 */
public class YAMLPerspective implements Perspective
{
    private static Logger log = Logger.getLogger( YAMLPerspective.class.getName() );

    private static final String IMPORTERS_ATTR = "importers";
    private static final String EXPORTERS_ATTR = "exporters";
    private static final String VIEWPARTS_ATTR = "viewparts";
    private static final String TAB_ATTR = "tab";
    private static final String REPOSITORY_ATTR = "repository";
    private static final String MESSAGEBUNDLE_ATTR = "messageBundle";
    private static final String PRIORITY_ATTR = "priority";
    private static final String NAME_ATTR = "name";
    private static final String INTRO_ATTR = "intro";
    private static final String ACTIONS_ATTR = "actions";
    private static final String PROJECTSELECTOR_ATTR = "projectSelector";
    private static final String TEMPLATE_ATTR = "template";
    private static final String HIDEDIAGRAMPANEL_ATTR = "hideDiagramPanel";
    private static final String CLOSEONLYONSESSIONEXPIRE_ATTR = "closeOnlyOnSessionExpire";

    private final String title;
    private final List<RepositoryTabInfo> tabInfo = new ArrayList<>();
    private final List<Rule> viewPartRules = new ArrayList<>();
    private final List<Rule> actionRules = new ArrayList<>();
    private final List<Rule> importerRules = new ArrayList<>();
    private final List<Rule> exporterRules = new ArrayList<>();

    private final Map<String,String> messageBundle = new HashMap<>();

    private int priority;
    private String introPage;
    private boolean showProjectSelector = true;
    private String defaultTemplate = null;
    private boolean hideDiagramPanel = false;
    private boolean closeOnlyOnSessionExpire = false;

    /*
     * <extension point="biouml.workbench.perspective"> <perspective
     * name="Genome Enhancer" priority="30" intro="intro-genomeenhancer"
     * projectSelector="false"> <repository> <tab title="GenomeEnhancer"
     * path="GenomeEnhancer" virtual="true"/> </repository> <viewparts> <deny
     * id="search.graph"/> <deny id="common.clipboard"/> <deny id="script.*"/>
     * <deny id="common.log"/> </viewparts> <actions> <deny
     * id="Apply antimony"/> <deny id="Change port"/> <deny id="Clone node"/>
     * <deny id="Merge clone"/> <deny id="Fix in compartment"/> <deny
     * id="Adjust reactions nodes"/> <deny id="Change Subdiagram"/> <deny
     * id="combine_tracks"/> <deny id="rename_project"/> </actions> <importers>
     * <deny id="*"/> <allow id="biouml.plugins.genomeenhancer.importer.*" />
     * <allow id="ru.biosoft.access.ZipFileImporter" /> <allow
     * id="ru.biosoft.access.TextFileImporter" /> <allow
     * id="ru.biosoft.bsa.SequenceImporter" /> <allow
     * id="ru.biosoft.bsa.SequenceImporter" /> <allow
     * id="ru.biosoft.access.FileImporter" /> <allow
     * id="ru.biosoft.bsa.SequenceImporter" /> <allow
     * id="com.genexplain.analyses.CELFilesImporter" /> <allow
     * id="com.genexplain.analyses.AgilentFilesImporter" /> <allow
     * id="com.genexplain.analyses.IlluminaFilesImporter" /> <allow
     * id="ru.biosoft.access.html.HtmlToPDFExporter" /> </importers>
     * </perspective> </extension>
     */

    public YAMLPerspective(LinkedHashMap<String, Object> element)
    {
        title = (String) element.get(NAME_ATTR);
        priority = (Integer) element.getOrDefault(PRIORITY_ATTR, 0);
        showProjectSelector = (Boolean) element.getOrDefault(PROJECTSELECTOR_ATTR, true);

        List<Object> repository = (List<Object>) element.get(REPOSITORY_ATTR);
        if( repository != null && repository.size() > 0 )
        {
            for ( Object repo : repository )
            {
                if( repo instanceof Map )
                {
                    Map<String, Object> repoProps = (Map<String, Object>) ((Map<String, Object>) repo).get("tab");
                    tabInfo.add(new RepositoryTabInfo((Map<String, Object>) repoProps));
                }
            }
        }
        //TODO:
        //        //        IConfigurationElement[] messagesArray = element.getChildren(MESSAGEBUNDLE_ATTR);
        //        //        if( messagesArray != null && messagesArray.length > 0)
        //        //        {
        //        //            IConfigurationElement[] messages = messagesArray[0].getChildren();
        //        //            if( messages != null )
        //        //            {
        //        //                for( IConfigurationElement message : messages )
        //        //                {
        //        //                    messageBundle.put( message.getAttribute( "key" ), message.getAttribute( "value" ) );
        //        //                }
        //        //            }
        //        //        }
        //
        List<Object> viewParts = (List<Object>) element.get(VIEWPARTS_ATTR);
        if( viewParts != null && viewParts.size() > 0 )
        {
            for ( Object rule : viewParts )
            {
                if( rule instanceof Map )
                {
                    Map<String, Object> ruleProps = (Map<String, Object>) ((Map<String, Object>) rule).get("rule");
                    viewPartRules.add(new Rule((Map<String, Object>) ruleProps));
                }
            }
        }

        List<Object> actions = (List<Object>) element.get(ACTIONS_ATTR);
        if( actions != null && actions.size() > 0 )
        {
            for ( Object rule : actions )
            {
                if( rule instanceof Map )
                {
                    Map<String, Object> ruleProps = (Map<String, Object>) ((Map<String, Object>) rule).get("rule");
                    actionRules.add(new Rule((Map<String, Object>) ruleProps));
                }
            }
        }

        //TODO:
        //        IConfigurationElement[] importers = element.getChildren( IMPORTERS_ATTR );
        //        if( importers != null && importers.length > 0 )
        //        {
        //            IConfigurationElement[] rules = importers[0].getChildren();
        //            if( rules != null )
        //            {
        //                for( IConfigurationElement rule : rules )
        //                {
        //                    importerRules.add( new Rule( rule ) );
        //                }
        //            }
        //        }
        //        IConfigurationElement[] exporters = element.getChildren( EXPORTERS_ATTR );
        //        if( exporters != null && exporters.length > 0 )
        //        {
        //            IConfigurationElement[] rules = exporters[0].getChildren();
        //            if( rules != null )
        //            {
        //                for( IConfigurationElement rule : rules )
        //                {
        //                    exporterRules.add( new Rule( rule ) );
        //                }
        //            }
        //        }
        try
        {
            introPage = (String)element.get(INTRO_ATTR);
            defaultTemplate = (String)element.get( TEMPLATE_ATTR );
            hideDiagramPanel = (Boolean) element.get(HIDEDIAGRAMPANEL_ATTR);
            closeOnlyOnSessionExpire = (Boolean) element.get(CLOSEONLYONSESSIONEXPIRE_ATTR);
        }
        catch( Exception e )
        {
            log.fine( "Can not load all perspective attributes for " + title );
        }
    }

    @Override
    public String getTitle()
    {
        return title;
    }

    @Override
    public List<RepositoryTabInfo> getRepositoryTabs()
    {
        return Collections.unmodifiableList(tabInfo);
    }

    @Override
    public boolean isViewPartAvailable(String viewPartId)
    {
        return matchRules( viewPartRules, viewPartId );
    }

    @Override
    public String getIntroPage()
    {
        return introPage;
    }

    @Override
    public int getPriority()
    {
        return priority;
    }

    @Override
    public JSONObject toJSON()
    {
        JSONObject result = new JSONObject();
        result.put(NAME_ATTR, title);
        result.put(PRIORITY_ATTR, priority);

        if( introPage != null )
            result.put(INTRO_ATTR, introPage);
        if( showProjectSelector )
            result.put(PROJECTSELECTOR_ATTR, showProjectSelector);
        if( defaultTemplate != null )
            result.put(TEMPLATE_ATTR, defaultTemplate);
        if( hideDiagramPanel )
            result.put(HIDEDIAGRAMPANEL_ATTR, hideDiagramPanel);
        if( closeOnlyOnSessionExpire )
            result.put(CLOSEONLYONSESSIONEXPIRE_ATTR, closeOnlyOnSessionExpire);

        if( messageBundle.size() > 0 )
        {
            JSONObject bundle = new JSONObject();
            messageBundle.forEach((k, v) -> bundle.put(k, v));
            result.put(MESSAGEBUNDLE_ATTR, bundle);
        } 

        JSONArray repository = new JSONArray();
        for( RepositoryTabInfo tab : tabInfo )
        {
            repository.put(tab.toJSON());
        }
        result.put(REPOSITORY_ATTR, repository);
        JSONArray viewParts = new JSONArray();
        for( Rule rule : viewPartRules )
        {
            viewParts.put(rule.toJSON());
        }
        result.put(VIEWPARTS_ATTR, viewParts);
        JSONArray actions = new JSONArray();
        for( Rule rule : actionRules )
        {
            actions.put(rule.toJSON());
        }
        result.put(ACTIONS_ATTR, actions);
        return result;
    }

    @Override
    public String toString()
    {
        return getTitle();
    }

    @Override
    public boolean isActionAvailable(String actionId)
    {
        return matchRules( actionRules, actionId );
    }

    @Override
    public boolean isImporterAvailable(String importerId)
    {
        return matchRules( importerRules, importerId );
    }

    @Override
    public boolean isExporterAvailable(String exporterId)
    {
        return matchRules( exporterRules, exporterId );
    }

    private static boolean matchRules(List<Rule> rules, String id)
    {
        boolean result = true;
        for( Rule rule : rules )
        {
            if( rule.isMatched( id ) )
                result = rule.isAllow();
        }
        return result;
    }

    @Override
    public String getDefaultTemplate()
    {
        return defaultTemplate;
    }
}
