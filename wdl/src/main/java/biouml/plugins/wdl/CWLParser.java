package biouml.plugins.wdl;

import java.util.Map;

import org.yaml.snakeyaml.Yaml;

public class CWLParser
{
    public Map<String, Object> parseYaml(String text)
    {
        Yaml parser = new Yaml();

        Object root;
        try
        {
            root = parser.load( text );
        }
        catch( Exception e )
        {
            return null;
        }
        if( root == null )
            return null;
        if( ! ( root instanceof Map ) )
            return null;

        Map<?, ?> rootMap = (Map<?, ?>)root;
        return (Map<String, Object>)rootMap;
    }
}
