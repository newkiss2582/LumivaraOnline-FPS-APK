package com.lumivaraonline.fps;

import android.app.ActivityManager;
import android.content.*;
import android.os.Build;
import java.util.*;

public final class ProfileComponents {
    private ProfileComponents(){}
    static Class<?> service(int slot){ProfileIdentity.checked(slot);return FloatingGameService.class;}
    static Class<?> fullscreen(int slot){ProfileIdentity.checked(slot);return GameFullscreenActivity.class;}
    static Class<?> login(int slot){ProfileIdentity.checked(slot);return GoogleLoginActivity.class;}
    static Intent serviceIntent(Context context,int slot,String action){return new Intent(context,service(slot)).setAction(action);}
    static String label(Context context,int slot){ProfileIdentity.checked(slot);return "Lumivara Online FPS";}
    static boolean isRunning(Context context,int slot){
        // Service-management UI only. Android still exposes our own services here.
        try{ActivityManager manager=(ActivityManager)context.getSystemService(Context.ACTIVITY_SERVICE);
            for(ActivityManager.RunningServiceInfo info:manager.getRunningServices(20))
                if(info.service.getPackageName().equals(context.getPackageName())&&info.service.getClassName().equals(service(slot).getName()))return true;
        }catch(Exception unavailable){}
        return false;
    }
    static void minimizeOthers(Context context,int ownSlot){
        for(int slot=0;slot<ProfileIdentity.supportedCount(Build.VERSION.SDK_INT);slot++)if(slot!=ownSlot)
            ProfileCommands.send(context,slot,ProfileCommands.MINIMIZE,null,accepted->{});
    }
}
