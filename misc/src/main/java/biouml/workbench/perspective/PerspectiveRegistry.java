package biouml.workbench.perspective;

import java.util.Collections;
import java.util.Comparator;

import one.util.streamex.StreamEx;

//import org.eclipse.core.runtime.IConfigurationElement;

import ru.biosoft.util.ObjectExtensionRegistry;
import ru.biosoft.util.ServerPreferences;

/**
 * @author lan,anna
 * Class refactored
 */
public class PerspectiveRegistry extends ObjectExtensionRegistry<Perspective>
{
    private static final PerspectiveRegistry instance = new PerspectiveRegistry();

    private PerspectiveRegistry()
    {
        super("biouml.workbench.perspective", "name", Perspective.class);
    }

    @Override
    protected void postInit()
    {
        Collections.sort( extensions, Comparator.comparing( Perspective::getPriority ).reversed().thenComparing( Perspective::getTitle ) );
    }

    public static Perspective getDefaultPerspective()
    {
        return instance.stream().findFirst().orElse( null );
    }
    
    public static StreamEx<Perspective> perspectives()
    {
        return instance.stream();
    }
    
    public static Perspective getPerspective(String name)
    {
        return instance.getExtension(name);
    }

    public static void registerPerspective(String elementName, String className, Object... args)
    {
        try
        {
            instance.registerElement(elementName, className, args[0]);
        }
        catch (Exception e)
        {
        }
    }

    public static final String PERSPECTIVE_PREFERENCE = "Perspective";

    public static Perspective getCurrentPerspective()
    {
        Object perspectiveObject = ServerPreferences.getPreferences().getValue(PERSPECTIVE_PREFERENCE);
        return perspectiveObject instanceof String ? PerspectiveRegistry.getPerspective((String) perspectiveObject) : PerspectiveRegistry.getDefaultPerspective();
    }

}
