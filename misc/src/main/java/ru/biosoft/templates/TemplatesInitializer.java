package ru.biosoft.templates;

import ru.biosoft.util.Initializer;

public class TemplatesInitializer extends Initializer
{
    private static TemplatesInitializer instance;

    public static TemplatesInitializer getInstance()
    {
        if( instance == null )
            instance = new TemplatesInitializer();
        return instance;
    }

    @Override
    protected void initTemplates()
    {
        //TemplateRegistry.registerTemplate("TEMPLATE NAME", "ANY CLASS WITH RESOURCE FOLDER INSTEAD OF PLUGIN NAME", "OTHER ARGUMENTS FOR TEMPLATE: FILE PATH, DESCRIPTION, IS BREAF, ORDER, FILTER");
        TemplateRegistry.registerTemplate("Default", "ru.biosoft.templates.TemplateInfo", "ru/biosoft/templates/beaninfotemplate.vm", "Universal bean template", false, 1);
        //TemplateFilter filter = new TemplateFilter("ru.biosoft.table.TableDataCollection", true, Collections.EMPTY_LIST, null);
        //TemplateRegistry.registerTemplate("Table info", "ru.biosoft.templates.TemplateInfo", "ru/biosoft/templates/tabletemplate.vm", "Table info templat", false, 2, filter);
        TemplateRegistry.addContextItem( "resolver", "biouml.standard.type.LinkResolver" );
    }

    public static void initialize()
    {
        getInstance().init();
    }
}
