package ru.biosoft.access;

import ru.biosoft.access.generic.DataElementTypeRegistry;
import ru.biosoft.access.generic.TransformerRegistry;
import ru.biosoft.util.Initializer;

public class AccessInitializer extends Initializer
{
    private static AccessInitializer instance;
    public static AccessInitializer getInstance()
    {
        if( instance == null )
            instance = new AccessInitializer();
        return instance;
    }

    public static void initialize()
    {
        getInstance().init();
    }

    @Override protected void initDataElementTypeDrivers()
    {
        DataElementTypeRegistry.registerDataElementTypeDriver("ru.biosoft.access.generic.DataElementEntryTypeDriver");
        DataElementTypeRegistry.registerDataElementTypeDriver("ru.biosoft.access.generic.DataElementFileTypeDriver");
        DataElementTypeRegistry.registerDataElementTypeDriver("ru.biosoft.access.generic.DataElementGenericCollectionTypeDriver");
        DataElementTypeRegistry.registerDataElementTypeDriver("ru.biosoft.access.generic.DataElementSQLTypeDriver");
        DataElementTypeRegistry.registerDataElementTypeDriver("ru.biosoft.access.generic.RepositoryTypeDriver");
    }

    @Override protected void initTransformers()
    {
        //ACCESS
        //TransformerRegistry.addTransformer("Image", "ru.biosoft.access.support.FileImageTransformer", "ru.biosoft.access.file.FileDataElement", "ru.biosoft.access.ImageDataElement");
        //TransformerRegistry.addTransformer("HTML file", "ru.biosoft.access.support.FileHtmlTransformer", "ru.biosoft.access.file.FileDataElement", "ru.biosoft.access.HtmlDataElement");
        TransformerRegistry.addTransformer( "Plain text", "ru.biosoft.access.file.FileTextTransformer", "ru.biosoft.access.file.FileDataElement",
                "ru.biosoft.access.core.TextDataElement");
        //TransformerRegistry.addTransformer("ZIP-archive with HTML pages", "ru.biosoft.access.support.FileZipHtmlTransformer", "ru.biosoft.access.file.FileDataElement", "ru.biosoft.access.html.ZipHtmlDataCollection");
        //TransformerRegistry.addTransformer("Video", "ru.biosoft.access.support.FileZipHtmlTransformer", "ru.biosoft.access.file.FileDataElement", "ru.biosoft.access.VideoDataElement");
    }

    protected void initBeanProviders()
    {
        BeanRegistry.registerBeanProvider("properties/fdc", "ru.biosoft.access.FDCBeanProvider");
    }


}
