package ru.biosoft.util;

import java.io.IOException;
import java.util.logging.Logger;

import com.developmentontheedge.beans.Preferences;


//TODO: new class to hold and manipulate Preferences, replacement for com.developmentontheedge.application.Application 
public class ServerPreferences
{
    private static Logger log = Logger.getLogger(ServerPreferences.class.getName());
    private static Preferences preferences;
    private static String preferencesFilePath;

    static public Preferences getPreferences()
    {
        return preferences;
    }

    static public void setPreferences(Preferences preferences)
    {
        ServerPreferences.preferences = preferences;
    }

    /** @return application constant by name. */
    static public String getGlobalValue(String constantName)
    {
        return getGlobalValue(constantName, constantName);
    }

    /**
     * @return application constant by name or defaultValue if property not
     * found
     */
    static public String getGlobalValue(String constantName, String defaultValue)
    {
        Preferences preferences = getPreferences();
        if( preferences != null )
        {
            Preferences globalPreferences = (Preferences) preferences.getValue("Global");
            if( globalPreferences != null )
            {
                Object value = globalPreferences.getValue(constantName);
                if( value != null )
                {
                    return value.toString();
                }
            }
        }
        return defaultValue;
    }

    public static void loadPreferences(String prefFilePath)
    {
        Preferences preferences = new Preferences();
        if( prefFilePath != null )
        {
            preferences.load(prefFilePath);
            setPreferences(preferences);
            preferencesFilePath = prefFilePath;
        }
        else
        {
            log.warning("Preferences file not found");
        }
    }

    public static void savePreferences()
    {
        if( preferences != null && preferencesFilePath != null )
            try
            {
                preferences.save(preferencesFilePath);
            }
            catch (IOException e)
            {
                // TODO Auto-generated catch block
            }

    }

    public static void savePreferences(Preferences preferences2)
    {
        preferences = preferences2;
        savePreferences();

    }
}
