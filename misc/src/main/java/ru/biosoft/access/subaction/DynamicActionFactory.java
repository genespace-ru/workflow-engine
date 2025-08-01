package ru.biosoft.access.subaction;

import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TObjectIntHashMap;

import java.awt.Event;
import java.awt.event.KeyEvent;
import java.lang.reflect.Field;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import javax.swing.Action;
import javax.swing.KeyStroke;
import one.util.streamex.StreamEx;
import ru.biosoft.access.exception.InitializationException;

//import org.eclipse.core.runtime.IConfigurationElement;

import ru.biosoft.util.ExtensionRegistrySupport;
import ru.biosoft.util.LazyValue;

//import com.developmentontheedge.application.Application;
//import com.developmentontheedge.application.action.ActionManager;

import biouml.workbench.perspective.Perspective;
//import biouml.workbench.perspective.PerspectiveUI;

//TODO: class refactored
public class DynamicActionFactory extends ExtensionRegistrySupport<DynamicAction>
{
    public static final String NAME_ATTR = "name";
    public static final String CLASS_ATTR = "class";
    public static final String VALUE_ATTR = "value";
    public static final String PARAMETER_ELEMENT = "parameter";
    public static final String PROPERTY_ELEMENT = "property";
    public static final String PROPERTY_NAME_ATTR = "name";
    public static final String PROPERTY_VALUE_ATTR = "value";

    private static final DynamicActionFactory instance = new DynamicActionFactory();

    private DynamicActionFactory()
    {
        super("ru.biosoft.access.dynamicAction", NAME_ATTR);
    }

    /**
     * Returns dynamic action by name
     */
    public static DynamicAction getDynamicAction(String actionName)
    {
        return instance.getExtension(actionName);
    }

    @Override
    protected void postInit()
    {
        Collections.sort( extensions );
    }

    //    @Override
    //    protected DynamicAction loadElement(IConfigurationElement element, String title) throws Exception
    //    {
    //        DynamicAction action = getClassAttribute(element, CLASS_ATTR, DynamicAction.class).newInstance();
    //        action.setTitle( title );
    //        action.setEnabled(false);
    //        initAction(action, element, action.getClass());
    //        for( IConfigurationElement propElement : element.getChildren(PARAMETER_ELEMENT) )
    //        {
    //            String pName = getStringAttribute(propElement, NAME_ATTR);
    //            if( pName.equals("numSelected") )
    //                action.setNumSelected(getIntAttribute(propElement, VALUE_ATTR));
    //            else if( pName.equals("acceptReadOnly") )
    //                action.setAcceptReadOnly(getBooleanAttribute(propElement, VALUE_ATTR));
    //        }
    //        return action;
    //    }

    public static StreamEx<DynamicAction> dynamicActions()
    {
        return instance.stream();
    }

    private static boolean actionsRegistered;
    protected synchronized static void registerActions()
    {
        if(!actionsRegistered)
        {
            //            ActionManager actionManager = Application.getActionManager();
            //            instance.entries().forKeyValue( actionManager::addAction );
            actionsRegistered = true;
        }
    }

    public static List<Action> getEnabledActions(Object model)
    {
        registerActions();
        List<Action> actions = new ArrayList<>();
        //        Perspective perspective = PerspectiveUI.getCurrentPerspective();
        //        dynamicActions().filter( action -> perspective.isActionAvailable( action.getTitle() ) )
        //                .filter( action -> action.isApplicable( model ) ).forEach( action -> {
        //            action.setEnabled(true);
        //            if(!actions.isEmpty() && !((DynamicAction)actions.get( actions.size()-1 )).isSameGroup( action ))
        //                actions.add(null);
        //            actions.add(action);
        //        });
        return actions;
    }

    //        public static void initAction(Action action, IConfigurationElement extension, Class<?> loader)
    //        {
    //            IConfigurationElement[] actionProperties = extension.getChildren(PROPERTY_ELEMENT);
    //            if( actionProperties != null )
    //            {
    //                for( IConfigurationElement propElement : actionProperties )
    //                {
    //                    String pName = propElement.getAttribute(PROPERTY_NAME_ATTR);
    //                    String pValue = propElement.getAttribute(PROPERTY_VALUE_ATTR);
    //                    if( ( pName != null ) && ( pValue != null ) )
    //                    {
    //                        if( pName.equals(Action.SMALL_ICON) )
    //                        {
    //                            URL url = loader.getResource(pValue);
    //                            // try to find in resources subdirectory
    //                            if( url == null )
    //                                url = loader.getResource("resources/" + pValue);
    //                            if( url != null )
    //                                action.putValue(Action.SMALL_ICON, new javax.swing.ImageIcon(url));
    //                        }
    //                        else if( pName.equals(Action.MNEMONIC_KEY) )
    //                        {
    //                            action.putValue(Action.MNEMONIC_KEY, getKeyEvent(pValue));
    //                        }
    //                        else if( pName.equals(Action.ACCELERATOR_KEY) )
    //                        {
    //                            Pattern p = Pattern.compile("[\\s|]+");
    //                            String[] values = p.split(pValue);
    //                            if( values.length > 0 )
    //                            {
    //                                int event = getKeyEvent(values[0]);
    //                                if(event > 0)
    //                                {
    //                                    int mask = 0;
    //                                    for( int i = 1; i < values.length; i++ )
    //                                    {
    //                                        mask = mask | getEventMask(values[i]);
    //                                    }
    //                                    KeyStroke keyAccelerator = KeyStroke.getKeyStroke(event, mask);
    //                                    action.putValue(Action.ACCELERATOR_KEY, keyAccelerator);
    //                                }
    //                            }
    //                        }
    //                        else
    //                        {
    //                            action.putValue(pName, pValue);
    //                        }
    //                    }
    //                }
    //            }
    //        }

    private static int getEventMask(String maskStr)
    {
        if( "CTRL_MASK".equals(maskStr) )
            return Event.CTRL_MASK;
        else if( "SHIFT_MASK".equals(maskStr) )
            return Event.SHIFT_MASK;
        else if( "ALT_MASK".equals(maskStr) )
            return Event.ALT_MASK;
        else if( "META_MASK".equals(maskStr) )
            return Event.META_MASK;
        return 0;
    }

    private static LazyValue<TObjectIntMap<String>> keyEventName2Code = new LazyValue<>("Key names", () ->
    {
        TObjectIntMap<String> keyEventName2Code = new TObjectIntHashMap<>();
        for( Field field : KeyEvent.class.getFields() )
        {
            try
            {
                keyEventName2Code.put(field.getName(), field.getInt(null));
            }
            catch( IllegalArgumentException | IllegalAccessException e )
            {
            }
        }
        return keyEventName2Code;
    });

    private static int getKeyEvent(String keyStr)
    {
        return keyEventName2Code.get().get(keyStr);
    }

    //Register Action loaded by YAML config file
    @Override protected DynamicAction registerElement(String elementName, String className, Object... args) throws Exception
    {
        DynamicAction action = getClass(className, DynamicAction.class).getDeclaredConstructor().newInstance();
        action.setTitle(elementName);
        if( args == null || args.length == 0 )
            return action;
        if( args[0] instanceof Map )
        {
            Map<String, Object> props = ((Map<String, Object>) args[0]);
            if( props.containsKey(PROPERTY_ELEMENT) )
            {
                action.setActionProperties((Map<String, Object>) props.get(PROPERTY_ELEMENT));
            }
            if( props.containsKey(PARAMETER_ELEMENT) )
            {
                Map<String, Object> parameters = (Map<String, Object>) props.get(PARAMETER_ELEMENT);
                action.setNumSelected((Integer) parameters.getOrDefault("numSelected", DynamicAction.SELECTED_UNDEFINED));
                action.setAcceptReadOnly((Boolean) parameters.getOrDefault("acceptReadOnly", false));
            }
        }
        return action;
    }

    /*
     * <dynamicAction name="One category"
     * class="biouml.plugins.genomeenhancer.annotation.OneCategoryAction">
     * <parameter name="numSelected" value="-1"/> <property name="Name"
     * value="Create single category"/> <property name="ShortDescription"
     * value="Create single category."/> <property name="SmallIcon"
     * value="resources/simple category.gif" /> <property
     * name="ActionCommandKey" value="cmd-one-category"/> </dynamicAction>
     */

    @Override public void addElement(String elementName, String className)
    {
        throw new UnsupportedOperationException("Can not add action with this method");

    }
}
