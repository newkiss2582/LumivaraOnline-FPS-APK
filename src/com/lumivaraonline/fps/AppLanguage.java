package com.lumivaraonline.fps;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import java.util.Locale;

/** Resource-only locale context. Never changes process Locale or the game WebView context. */
final class AppLanguage {
    static final String PREFS="app_settings",KEY="app_language";
    static final String CHANGED="com.lumivaraonline.fps.MENU_LANGUAGE_CHANGED";
    private static String remoteChoice;
    private AppLanguage(){}
    static SharedPreferences preferences(Context context){return context.getSharedPreferences(PREFS,Context.MODE_PRIVATE);}
    static String choice(Context context){
        if(ProfileApplication.slot()==0)return LanguageChoice.normalize(preferences(context).getString(KEY,LanguageChoice.DEFAULT));
        if(remoteChoice==null){
            try{android.os.Bundle reply=context.getContentResolver().call(android.net.Uri.parse("content://"+context.getPackageName()+".menu-language"),"language",null,null);remoteChoice=LanguageChoice.normalize(reply==null?null:reply.getString("language"));}
            catch(Exception unavailable){remoteChoice=LanguageChoice.DEFAULT;}
        }
        return remoteChoice;
    }
    static void acceptRemote(String choice){remoteChoice=LanguageChoice.normalize(choice);}
    static void refreshRemote(){remoteChoice=null;}
    static String effective(Context context){return LanguageChoice.resolve(choice(context),context.getResources().getConfiguration().getLocales().get(0).getLanguage());}
    static void set(Context context,String choice){String normalized=LanguageChoice.normalize(choice);preferences(context).edit().putString(KEY,normalized).apply();context.sendBroadcast(new android.content.Intent(CHANGED).setPackage(context.getPackageName()).putExtra(KEY,normalized));}
    static String text(Context context,int resource){
        Configuration config=new Configuration(context.getResources().getConfiguration());
        config.setLocale(Locale.forLanguageTag(effective(context)));
        return context.createConfigurationContext(config).getString(resource);
    }
    static String label(Context context){String mode=choice(context);return text(context,"en".equals(mode)?R.string.language_english:"system".equals(mode)?R.string.language_system:R.string.language_thai);}
}
