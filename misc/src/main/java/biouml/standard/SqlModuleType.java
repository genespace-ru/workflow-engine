
package biouml.standard;

//import com.developmentontheedge.application.Application;

public class SqlModuleType extends StandardModuleType
{
    
    @Override
    public boolean canCreateEmptyModule ( )
    {
        return false;
    }

    public SqlModuleType ( )
    {
        //TODO: commented, Application
        //super ( Application.getGlobalValue("ApplicationName")+" standard (SQL)" );
        super("Application standard (SQL)");
    }

}
