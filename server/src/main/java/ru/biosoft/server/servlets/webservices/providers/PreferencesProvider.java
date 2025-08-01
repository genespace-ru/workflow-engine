package ru.biosoft.server.servlets.webservices.providers;

import ru.biosoft.server.servlets.webservices.BiosoftWebRequest;
import ru.biosoft.server.servlets.webservices.JSONResponse;
import ru.biosoft.util.ServerPreferences;

import com.developmentontheedge.beans.Preferences;

/**
 * @author lan, anna 
 * class refactored to use ServerPreferences
 *
 */
public class PreferencesProvider extends WebJSONProviderSupport
{
    @Override
    public void process(BiosoftWebRequest arguments, JSONResponse response) throws Exception
    {
        Preferences preferences = ServerPreferences.getPreferences();
        if( "add".equals(arguments.optAction()) )
        {
            String propName = arguments.getString("name");
            String propValue = arguments.getString("value");
            preferences.addValue(propName, propValue);
            ServerPreferences.savePreferences(preferences);
        }
        WebBeanProvider.sendBeanStructure("preferences", preferences, response);
    }
}
