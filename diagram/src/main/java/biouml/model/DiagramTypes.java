package biouml.model;

import one.util.streamex.StreamEx;
import ru.biosoft.util.Clazz;
import ru.biosoft.util.ObjectExtensionRegistry;

public class DiagramTypes
{

    private static final ObjectExtensionRegistry<DiagramType> diagramTypes = new ObjectExtensionRegistry<>( "biouml.workbench.diagramType", DiagramType.class );

    public static void addDiagramType(String typeClass)
    {
        diagramTypes.addElement( typeClass, typeClass );
    }

    public static StreamEx<Class<? extends DiagramType>> getGeneralPurposeTypes()
    {
        return diagramTypes.stream().filter( DiagramType::isGeneralPurpose ).map( DiagramType::getClass );
    }

    public static Class<? extends DiagramType>[] getDiagramTypes()
    {
        return getGeneralPurposeTypes().toArray( Class[]::new );
    }

    public static StreamEx<DiagramType> getDiagramTypeObjects()
    {
        return StreamEx.of( getDiagramTypes() ).map( Clazz.of( DiagramType.class )::createOrLog ).nonNull();
    }
}
