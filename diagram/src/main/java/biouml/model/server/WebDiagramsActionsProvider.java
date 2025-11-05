package biouml.model.server;

import java.awt.Point;
import java.beans.BeanDescriptor;
import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

import javax.swing.Icon;
import javax.swing.ImageIcon;

import org.apache.commons.text.StringEscapeUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import com.developmentontheedge.beans.model.ComponentFactory;
import com.developmentontheedge.beans.model.ComponentFactory.Policy;
import com.developmentontheedge.beans.model.ComponentModel;

import biouml.model.Diagram;
import biouml.model.DiagramType;
import biouml.model.Module;
import biouml.model.workbench.DiagramDynamicActionProperties;
import biouml.model.workbench.DiagramEditorHelper;
import biouml.standard.type.Stub;
import one.util.streamex.EntryStream;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.Environment;
import ru.biosoft.access.security.SecurityManager;
import ru.biosoft.access.subaction.BackgroundDynamicAction;
import ru.biosoft.access.subaction.DynamicAction;
import ru.biosoft.access.subaction.DynamicActionFactory;
import ru.biosoft.access.task.JobControlTask;
import ru.biosoft.access.task.TaskPool;
import ru.biosoft.exception.ExceptionRegistry;
import ru.biosoft.exception.LoggedClassCastException;
import ru.biosoft.exception.LoggedClassNotFoundException;
import ru.biosoft.exception.LoggedException;
import ru.biosoft.jobcontrol.JobControl;
import ru.biosoft.jobcontrol.JobControlEvent;
import ru.biosoft.jobcontrol.JobControlListenerAdapter;
import ru.biosoft.server.JSONUtils;
import ru.biosoft.server.servlets.webservices.BiosoftWebRequest;
import ru.biosoft.server.servlets.webservices.BiosoftWebResponse;
import ru.biosoft.server.servlets.webservices.JSONResponse;
import ru.biosoft.server.servlets.webservices.WebException;
import ru.biosoft.server.servlets.webservices.WebJob;
import ru.biosoft.server.servlets.webservices.providers.WebActionsProvider;
import ru.biosoft.util.TextUtil2;

public class WebDiagramsActionsProvider extends WebActionsProvider
{

    private static final Map<String, JSONObject[]> diagramTypesMap = new ConcurrentHashMap<>();

    @Override
    public void process(BiosoftWebRequest arguments, BiosoftWebResponse resp) throws Exception
    {
        String type = arguments.getString( "type" );

        JSONResponse response = new JSONResponse( resp );
        if( type.equals( "diagram" ) )
        {
            JSONObject[] actions = null;

            String action = arguments.optAction();
            if( "toolbar_icon".equals( action ) )
            {
                String name = arguments.getString( "name" ); // type of node/edge
                DiagramType diagramType = stringToType( arguments.getString( "diagramType" ) );
                Icon icon = diagramType.getDiagramViewBuilder().getIcon( name );
                sendIcon( resp, (ImageIcon) icon );
                return;
            }
            actions = loadDiagramActions( WebDiagramsProvider.getDiagramChecked( arguments.getDataElementPath() ) );
            if( actions != null )
            {
                response.sendActions( actions );
            }
            return;
        }
        else if( type.equals( "dynamic" ) )
        {
            String action = arguments.getAction();
            if( action.equals( "validate" ) || action.equals( "run" ) )
            {
                String actionName = arguments.getString( "name" );
                DataElement dc = arguments.getDataElement();
                if( action.equals( "validate" ) )
                {
                    validateDynamicAction( actionName, dc, getSelectedItems( arguments ), getActionProperties( arguments ), response );
                    return;
                }
                else if( action.equals( "run" ) )
                {
                    String result = runDynamicAction( actionName, dc, getSelectedItems( arguments ), getActionProperties( arguments ), arguments.get( "jobID" ) );
                    response.sendString( result );
                }
            }
            else
                super.process( arguments, resp );
        }
        else
            super.process( arguments, resp );
    }

    //TODO: this is copy from parent class, refactor
    public static void validateDynamicAction(String actionName, DataElement actionModel, List<DataElement> selectedItems, JSONArray properties, JSONResponse response)
            throws IOException, WebException
    {
        DynamicAction action = DynamicActionFactory.getDynamicAction( actionName );
        if( action != null )
        {
            action.validateParameters( actionModel, selectedItems );
            Object pd = getActionProperties( actionName, action, actionModel, selectedItems, properties == null );
            if( pd != null && !pd.getClass().equals( DiagramDynamicActionProperties.class ) )
            {
                try
                {
                    ComponentModel model = ComponentFactory.getModel( pd, Policy.DEFAULT, true );
                    if( properties != null )
                    {
                        JSONUtils.correctBeanOptions( model, properties );
                    }
                    JSONArray jsonProperties = JSONUtils.getModelAsJSON( model );
                    response.sendJSONBean( jsonProperties );
                }
                catch (Exception e)
                {
                    throw new WebException( e, "EX_INTERNAL_DURING_ACTION", actionName );
                }
            }
            else
            {
                String confirmation = action.getConfirmationMessage( actionModel, selectedItems );
                if( confirmation != null )
                {
                    response.sendJSON( new JSONObject().put( "confirm", confirmation ) );
                }
                else
                {
                    response.send( new byte[0], 0 );
                }
            }
        }
    }

    //TODO: this is copy from parent class, refactor
    public static String runDynamicAction(final String actionName, final ru.biosoft.access.core.DataElement actionModel, List<DataElement> selectedItems, JSONArray properties,
            String jobID) throws WebException
    {
        DynamicAction action = DynamicActionFactory.getDynamicAction( actionName );
        if( action == null )
            throw new WebException( "EX_QUERY_NO_ACTION", actionName );
        try
        {
            action.validateParameters( actionModel, selectedItems );
            Object pd = action.getProperties( actionModel, selectedItems );
            if( properties != null && pd != null )
            {
                ComponentModel model = ComponentFactory.getModel( pd, Policy.DEFAULT, true );
                JSONUtils.correctBeanOptions( model, properties );
            }
            if( action instanceof BackgroundDynamicAction && jobID != null )
            {
                if( actionModel instanceof Diagram && pd instanceof DiagramDynamicActionProperties )
                {
                    Diagram diagram = (Diagram) actionModel;
                    DiagramEditorHelper helper = new DiagramEditorHelper( diagram );
                    ((DiagramDynamicActionProperties) pd).setHelper( helper );
                }
                final JobControl jc = ((BackgroundDynamicAction) action).getJobControl( actionModel, selectedItems, pd );
                final WebJob webJob = WebJob.getWebJob( jobID );
                webJob.setJobControl( jc );
                jc.addListener( new JobControlListenerAdapter()
                {
                    @Override
                    public void jobTerminated(JobControlEvent event)
                    {
                        if( event.getStatus() == JobControl.TERMINATED_BY_ERROR && event.getException() != null )
                        {
                            webJob.addJobMessage( "ERROR - " + ExceptionRegistry.log( event.getException().getError() ) + "\n" );
                        }
                    }
                } );
                TaskPool.getInstance().submit( new JobControlTask( "Action: " + actionName + " (user: " + SecurityManager.getSessionUser() + ")", jc )
                {
                    @Override
                    public void doRun()
                    {
                        try
                        {
                            if( actionModel instanceof Diagram )
                            {
                                WebDiagramsProvider.performTransaction( (Diagram) actionModel, actionName, jc );
                            }
                            else
                            jc.run();
                        }
                        catch (Exception e)
                        {
                            log.log( Level.SEVERE, e.getMessage(), e );
                        }
                    }
                } );
                return "job started";
            }
            action.performAction( actionModel, selectedItems, pd );
            return "action finished";
        }
        catch (LoggedException e)
        {
            throw e;
        }
        catch (Exception e)
        {
            throw new WebException( e, "EX_INTERNAL_DURING_ACTION", actionName );
        }
    }

    private static String typeToString(DiagramType type)
    {
        //TODO: commented XmlDiagramType
        //            if(type instanceof XmlDiagramType)
        //            {
        //                return XML_TYPE_PREFIX+((XmlDiagramType)type).getCompletePath();
        //            }
        return type.getClass().getName();
    }

    private static DiagramType stringToType(String type)
    {
        //TODO: commented XmlDiagramType
        //            if(type.startsWith( XML_TYPE_PREFIX ))
        //            {
        //                return DataElementPath.create( type.substring( XML_TYPE_PREFIX.length() ) ).getDataElement( XmlDiagramType.class );
        //            }
        try
        {
            return Environment.loadClass( type, DiagramType.class ).newInstance();
        }
        catch (LoggedClassNotFoundException | LoggedClassCastException | InstantiationException | IllegalAccessException e)
        {
            throw ExceptionRegistry.translateException( e );
        }
    }

    public static JSONObject[] loadDiagramActions(Diagram diagram)
    {
        String key = null;
        //TODO: commented XmlDiagramType
        //          if( diagram.getType() instanceof XmlDiagramType )
        //          {
        //              key = ( (XmlDiagramType)diagram.getType() ).getName();
        //          }
        //          else
        //          {
        key = diagram.getType().getClass().getName();
        //          }
        DataElementPath modulePath = Module.optModulePath( diagram );
        if( modulePath != null )
            key += modulePath;

        return diagramTypesMap.computeIfAbsent( key,
                k -> EntryStream.of( diagram.getType().getNodeTypes(), true, diagram.getType().getEdgeTypes(), false ).nonNullKeys().flatMapKeys( Arrays::stream )
                        .filterKeys( type -> diagram.getType().getDiagramViewBuilder().getIcon( type ) != null ).prepend( null, true )
                        .mapKeyValue( (type, isEdge) -> getActionForDiagramElementType( diagram, type, isEdge ) ).toArray( JSONObject[]::new ) );
    }

    /**
     * Create JSON action for diagram element
     */
    protected static JSONObject getActionForDiagramElementType(Diagram diagram, Object type, boolean isNode)
    {
        String name;
        String icon;
        String callbackFunction;
        boolean instantAction = false;
        //TODO: isComposite!!!
        String compositeParameter = "";//(DiagramUtility.isComposite( diagram )) ? ", 'composite'" : "";
        if( type == null )
        {
            icon = name = "Select";
            callbackFunction = "function(event){this.setSelectMode();return true;}";
            instantAction = true;
        }
        else if( type instanceof Class )
        {
            Class typeClass = (Class) type;
            String typeName = typeClass.getName();
            //TODO: commented XmlDiagramType
            //              if( diagram.getType() instanceof XmlDiagramType )
            //              {
            //                  icon = name = ( (XmlDiagramType)diagram.getType() ).getKernelTypeName(type);
            //                  typeName = name;
            //              }
            //              else
            //              {
            icon = typeClass.getSimpleName();
            try
            {
                BeanInfo bi = Introspector.getBeanInfo( typeClass );
                BeanDescriptor bd = bi.getBeanDescriptor();
                name = bd.getDisplayName();
            }
            catch (IntrospectionException e)
            {
                name = typeClass.getName();
            }
            //              }

            //TODO: commented Reaction
            //            if( typeClass == Reaction.class )
            //            {
            //                callbackFunction = "function(event){this.createNewReaction(event);return false;}";
            //            }
            //            else
            //            {
                String dcCompleteName;
                //TODO: commented XmlDiagramType
                if( isModuleRequired( typeClass ) /*
                                                   * && !(diagram.getType()
                                                   * instanceof XmlDiagramType)
                                                   */
                        && diagram.getType().getSemanticController().getPropertiesByType( diagram, typeClass, new Point( 0, 0 ) ) == null )
                    dcCompleteName = "not available";
                else
                    dcCompleteName = "";
                try
                {
                    DataCollection category = Module.getModule( diagram ).getCategory( typeClass );
                    if( category != null )
                        dcCompleteName = category.getCompletePath().toString();
                }
                catch (Exception e)
                {
                }
                if( isNode )
                {
                    callbackFunction = "function(event){this.createNewNode(event, '" + StringEscapeUtils.escapeEcmaScript( dcCompleteName ) + "', '" + typeName
                            + "');return false;}";
                }
                else
                {
                    callbackFunction = "function(event){this.createNewEdge(event, '" + StringEscapeUtils.escapeEcmaScript( dcCompleteName ) + "', '" + typeName + "'"
                            + compositeParameter + ");return false;}";
                    instantAction = true;
                }
                //            }
        }
        else
        {
            icon = name = type.toString();
            if( isNode )
            {
                callbackFunction = "function(event){this.createNewNode(event, '', '" + type.toString() + "');return false;}";
            }
            else
            {
                callbackFunction = "function(event){this.createNewEdge(event, '', '" + type.toString() + "'" + compositeParameter + ");return false;}";
                instantAction = true;
            }
        }
        JSONObject json = new JSONObject();
        String id = name.toLowerCase().replaceAll( " ", "_" );
        json.put( "id", id );
        json.put( "label", name.toLowerCase() );
        json.put( "icon", icon.equals( "Select" ) ? "select.gif"
                : "/web/action?action=toolbar_icon&type=diagram&name=" + TextUtil2.encodeURL( icon ) + "&diagramType=" + TextUtil2.encodeURL( typeToString( diagram.getType() ) ) );
        json.put( "visible", "function(node, treeObj){return true;}" );
        json.put( "action",
                "function(node, treeObj){var activeDocument = opennedDocuments[activeDocumentId];if (activeDocument instanceof Diagram || activeDocument instanceof CompositeDiagram){"
                        + (instantAction ? "(" + callbackFunction + ").apply(activeDocument);" : "activeDocument.selectControl(" + callbackFunction + compositeParameter + ");")
                        + "}}" );
        return json;
    }

    private static boolean isModuleRequired(Class<?> type)
    {
        //TODO: commented EModelRoleSupport
        if( Stub.class.isAssignableFrom( type ) /*|| EModelRoleSupport.class.isAssignableFrom( type ) */)
            return false;
        return true;
    }
}
