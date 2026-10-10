package com.lumivaraonline.fps;

/** App UI locales only. This class does not change the game's language or storage. */
public final class LanguageChoice {
    public static final String DEFAULT="th";
    public static final String[] MODES={"th","en","system"};
    private LanguageChoice(){}
    public static String normalize(String choice){return "en".equals(choice)||"system".equals(choice)?choice:DEFAULT;}
    public static String resolve(String choice,String deviceLanguage){
        String mode=normalize(choice);
        if(!"system".equals(mode))return mode;
        return deviceLanguage!=null&&(deviceLanguage.equalsIgnoreCase("th")||deviceLanguage.toLowerCase(java.util.Locale.ROOT).startsWith("th-")||deviceLanguage.toLowerCase(java.util.Locale.ROOT).startsWith("th_"))?"th":"en";
    }
    public static int index(String choice){String mode=normalize(choice);for(int i=0;i<MODES.length;i++)if(MODES[i].equals(mode))return i;return 0;}
}
