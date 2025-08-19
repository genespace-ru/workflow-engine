package biouml.model.workbench;

import java.util.ListResourceBundle;

public class MessageBundle extends ListResourceBundle
{
    @Override
    protected Object[][] getContents()
    {
        return new Object[][] {
                { "CANNOT_MOVE_NODE_INTO_PARENT", "Node \"{0}\" cannot be moved into \"{1}\"" },
                { "MESSAGE_NODE_ALREADY_EXIST", "Node \"{0}\" already exists in compartment \"{1}\"" },
        };
    }

    /**
     * Returns string from the resource bundle for the specified key.
     * If the string is absent the key string is returned instead and
     * the message is printed in <code>java.util.logging.Logger</code> for the component.
     */
    public String getResourceString(String key)
    {
        try
        {
            return getString(key);
        }
        catch( Throwable t )
        {
            System.out.println("Missing resource <" + key + "> in " + this.getClass());
        }
        return key;
    }
}