package ru.biosoft.server;

import biouml.plugins.server.access.AccessService;
import ru.biosoft.access.BeanRegistry;
import ru.biosoft.server.servlets.webservices.providers.DocumentProvider;
import ru.biosoft.server.servlets.webservices.providers.HtmlTemplateProvider;
import ru.biosoft.server.servlets.webservices.providers.ImageProvider;
import ru.biosoft.server.servlets.webservices.providers.PerspectivesProvider;
import ru.biosoft.server.servlets.webservices.providers.PreferencesProvider;
import ru.biosoft.server.servlets.webservices.providers.ServiceProvider;
import ru.biosoft.server.servlets.webservices.providers.WebActionsProvider;
import ru.biosoft.server.servlets.webservices.providers.WebBeanProvider;
import ru.biosoft.server.servlets.webservices.providers.WebProviderFactory;
import ru.biosoft.server.servlets.webservices.providers.WebTablesProvider;
import ru.biosoft.util.Initializer;

public class ServerInitializer extends Initializer
{

    private static ServerInitializer instance;
    public static ServerInitializer getInstance()
    {
        if( instance == null )
            instance = new ServerInitializer();
        return instance;
    }

    @Override protected void initServices()
    {
        ServiceRegistry.registerService("access.service", new AccessService());
    }

    @Override protected void initProviders()
    {
        WebProviderFactory.registerProvider("data", new ServiceProvider());
        WebProviderFactory.registerProvider("bean", new WebBeanProvider());
        WebProviderFactory.registerProvider("html", new HtmlTemplateProvider());
        WebProviderFactory.registerProvider("img", new ImageProvider());
        WebProviderFactory.registerProvider("table", new WebTablesProvider());
        WebProviderFactory.registerProvider("preferences", new PreferencesProvider());
        WebProviderFactory.registerProvider("perspective", new PerspectivesProvider());

        WebProviderFactory.registerProvider("action", new WebActionsProvider());
        WebProviderFactory.registerProvider("doc", new DocumentProvider());

        /*
         * <extension point="ru.biosoft.server.servlets.webProvider"> <provider
         * prefix="diagram" class=
         * "ru.biosoft.server.servlets.webservices.providers.WebDiagramsProvider"
         * /> <provider prefix="content" class=
         * "ru.biosoft.server.servlets.webservices.providers.ContentProvider"/>
         * <provider prefix="html_page" class=
         * "ru.biosoft.server.servlets.webservices.providers.HtmlPageTemplateProvider"
         * /> <provider prefix="revert" class=
         * "ru.biosoft.server.servlets.webservices.providers.RevertRequestProvider"
         * /> <provider prefix="script" class=
         * "ru.biosoft.server.servlets.webservices.providers.WebScriptsProvider"
         * /> <provider prefix="jobcontrol" class=
         * "ru.biosoft.server.servlets.webservices.providers.JobControlProvider"
         * /> <provider prefix="analysis" class=
         * "ru.biosoft.server.servlets.webservices.providers.AnalysisProvider"/>
         * <provider prefix="tasks"
         * class="ru.biosoft.server.servlets.webservices.providers.TaskProvider"
         * /> <provider prefix="work" class=
         * "ru.biosoft.server.servlets.webservices.providers.WorksProvider"/> />
         * <provider prefix="export" class=
         * "ru.biosoft.server.servlets.webservices.providers.ExportProvider"/>
         * <provider prefix="import" class=
         * "ru.biosoft.server.servlets.webservices.providers.ImportProvider"/>
         * <provider prefix="import2"
         * class="ru.biosoft.server.servlets.webservices.imports.ImportProvider"
         * /> <provider prefix="treetable" class=
         * "ru.biosoft.server.servlets.webservices.providers.WebTreeTablesProvider"
         * /> <provider prefix="newElement" class=
         * "ru.biosoft.server.servlets.webservices.providers.NewElementProvider"
         * /> <provider prefix="wikihelp" class=
         * "ru.biosoft.server.servlets.webservices.providers.WikiHelpProvider"/>
         * <provider prefix="serverMessages" class=
         * "ru.biosoft.server.servlets.webservices.messages.ServerMessagesProvider"
         * /> <provider prefix="folder" class=
         * "ru.biosoft.server.servlets.webservices.providers.CopyFolderProvider"
         * <provider prefix="git" class=
         * "ru.biosoft.server.servlets.webservices.providers.GitWebProvider"/>
         * <provider prefix="oasys" class=
         * "ru.biosoft.server.servlets.webservices.providers.OASYSWebProvider"/>
         * <provider prefix="log" class=
         * "ru.biosoft.server.servlets.webservices.providers.WebLogProvider"/>
         * <provider prefix="video" class=
         * "ru.biosoft.server.servlets.webservices.providers.VideoProvider"/>
         * </extension>
         */
    }

    public static void initialize()
    {
        getInstance().init();
    }

    @Override protected void initBeans()
    {
        //BeanRegistry.registerBeanProvider("NAME", "BEAN CLASS");
    }



    protected void initCommonClasses()
    {
        AccessService.addCommonClass("ru.biosoft.access.core.DataCollection");
        AccessService.addCommonClass("ru.biosoft.access.file.FileDataElement");
        AccessService.addCommonClass("ru.biosoft.access.FileCollection");
        AccessService.addCommonClass("ru.biosoft.access.core.TransformedDataCollection");
        AccessService.addCommonClass("ru.biosoft.access.LocalRepository");
        AccessService.addCommonClass("ru.biosoft.access.security.NetworkRepository");
        AccessService.addCommonClass("ru.biosoft.access.SqlDataCollection");
        AccessService.addCommonClass("ru.biosoft.access.security.NetworkDataCollection");
        AccessService.addCommonClass("ru.biosoft.access.core.TextDataElement");

        /*
         * class="ru.biosoft.access.HtmlDataElement"/> <class
         * class="ru.biosoft.access.ImageDataElement"/> </extension>
         * 
         */
    }

}
