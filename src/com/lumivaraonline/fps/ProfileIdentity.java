package com.lumivaraonline.fps;

/** Fixed slots cannot be reassigned while a live WebView owns their data directory. */
public final class ProfileIdentity {
    public static final int COUNT=1;
    private ProfileIdentity(){}
    public static int checked(int slot){if(slot<0||slot>=COUNT)throw new IllegalArgumentException("Invalid profile");return slot;}
    public static int fromProcess(String packageName,String process){
        if(packageName.equals(process))return 0;
        for(int slot=1;slot<COUNT;slot++)if((packageName+":profile"+slot).equals(process))return slot;
        throw new IllegalArgumentException("Unexpected application process");
    }
    public static String suffix(int slot){checked(slot);return slot==0?null:"profile"+slot;}
    public static String windowPrefs(int slot){checked(slot);return slot==0?"floating":"floating_profile"+slot;}
    public static int notificationId(int slot){return 100+checked(slot);}
    public static int requestId(int slot,int action){if(action<1||action>3)throw new IllegalArgumentException();return checked(slot)*10+action;}
    public static int supportedCount(int sdk){return sdk>=28?COUNT:1;}
}
