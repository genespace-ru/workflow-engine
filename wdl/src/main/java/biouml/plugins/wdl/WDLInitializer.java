package biouml.plugins.wdl;

import biouml.plugins.wdl.web.WDLWebProvider;
import ru.biosoft.access.BeanRegistry;
import ru.biosoft.access.DataElementImporterRegistry;
import ru.biosoft.server.servlets.webservices.providers.WebProviderFactory;
import ru.biosoft.util.Initializer;

public class WDLInitializer extends Initializer
{

    private static WDLInitializer instance;

    public static WDLInitializer getInstance()
    {
        if( instance == null )
            instance = new WDLInitializer();
        return instance;
    }

    public static void initialize()
    {
        getInstance().init();
    }

    @Override
    protected void initProviders()
    {
        WebProviderFactory.registerProvider( "wdl", new WDLWebProvider() );
    }

    @Override
    protected void initBeanProviders()
    {
        BeanRegistry.registerBeanProvider( "wdlsettings", "biouml.plugins.wdl.web.WDLSettingsBeanProvider" );
    }

    @Override
    protected void initImporters()
    {
        DataElementImporterRegistry.registerImporter( "Import WDL", "biouml.plugins.wdl.diagram.WDLImporter", "WDL file", null );
    }

}
