package biouml.workbench.perspective;

import java.util.Map;
import java.util.regex.Pattern;

import org.json.JSONObject;

//import org.eclipse.core.runtime.IConfigurationElement;

//import com.eclipsesource.json.JsonObject;

import ru.biosoft.access.ClassLoading;
import ru.biosoft.exception.LoggedClassNotFoundException;
import ru.biosoft.util.TextUtil2;

public class Rule
{
    private final boolean allow;
    private final String template;
    private final Pattern regExp;
    
    public Rule(boolean allow, String template)
    {
        this.allow = allow;
        this.template = template;
        this.regExp = Pattern.compile(TextUtil2.wildcardToRegex(template));
    }
    
    protected Rule(Map<String, Object> element)
    {
        allow = checkAllowed( element );
        template = (String) element.get("id");
        regExp = Pattern.compile(TextUtil2.wildcardToRegex(template));
    }
    
    private boolean checkAllowed(Map<String, Object> element)
    {
        if( element.get("name").equals("allowWithClass") )
        {
            try
            {
                ClassLoading.loadClass((String) element.get("class"));
                return true;
            }
            catch( LoggedClassNotFoundException e )
            {
                return false;
            }
        }
        return element.get("name").equals("allow");
    }

    public boolean isAllow()
    {
        return allow;
    }
    
    public boolean isMatched(String id)
    {
        return regExp.matcher(id).matches();
    }

    public JSONObject toJSON()
    {
        return new JSONObject().put("type", allow ? "allow" : "deny").put("template", template);
    }
}