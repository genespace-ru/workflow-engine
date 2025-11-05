package biouml.model;

import biouml.model.server.DiagramService;
import biouml.model.server.WebDiagramsActionsProvider;
import biouml.model.server.WebDiagramsProvider;
import biouml.plugins.server.access.AccessService;
import ru.biosoft.access.file.FileType;
import ru.biosoft.access.file.FileTypePriority;
import ru.biosoft.access.file.FileTypeRegistry;
import ru.biosoft.access.generic.TransformerRegistry;
import ru.biosoft.server.ServiceRegistry;
import ru.biosoft.server.servlets.webservices.providers.WebProviderFactory;
import ru.biosoft.util.Initializer;

public class DiagramInitializer extends Initializer
{

    private static DiagramInitializer instance;

    public static DiagramInitializer getInstance()
    {
        if( instance == null )
            instance = new DiagramInitializer();
        return instance;
    }

    public static void initialize()
    {
        getInstance().init();
    }

    @Override protected void initDataTypes()
    {
        //DataType.addDataType("ru.biosoft.bsa.access.GenomeBrowserDataType");

    }

    @Override protected void initServices()
    {
        ServiceRegistry.registerService( "diagram.service", new DiagramService() );
    }

    @Override protected void initTransformers()
    {
        TransformerRegistry.addTransformer( "Diagram", "biouml.model.util.DiagramXmlTransformer", "ru.biosoft.access.file.FileDataElement", "biouml.model.Diagram" );
    }

    @Override protected void initTemplates()
    {
        //TemplateRegistry.registerTemplate("TEMPLATE NAME", "ANY CLASS WITH RESOURCE FOLDER INSTEAD OF PLUGIN NAME", "OTHER ARGUMENTS FOR TEMPLATE: FILE PATH, DESCRIPTION, IS BREAF, ORDER, FILTER");
    }

    @Override protected void initProviders()
    {
        WebProviderFactory.registerProvider( "diagram", new WebDiagramsProvider() );
        WebProviderFactory.registerProvider( "action", new WebDiagramsActionsProvider() );
    }

    @Override protected void initBeanProviders()
    {
        //BeanRegistry.registerBeanProvider("trackFinder/parameters", "ru.biosoft.bsa.finder.TrackFinderBeanProvider");
    }

    @Override protected void initTableResolvers()
    {
        //WebTablesProvider.addTableResolver("sites", "ru.biosoft.bsa.server.SitesTableResolver");
    }

    @Override
    protected void initFileTypes()
    {

        //String[] extensions, String transformerClassName, FileTypePriority priority, String description

        FileTypeRegistry
                .register( new FileType( "BioUML diagram", new String[] { "dml" }, "biouml.model.util.DiagramXmlTransformer", FileTypePriority.HIGH_PRIORITY,
                        "BioUML diagram file" ) );
    }

    @Override
    protected void initCommonClasses()
    {
        AccessService.addCommonClass( "biouml.model.Diagram" );
    }
}
