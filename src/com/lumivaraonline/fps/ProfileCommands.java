package com.lumivaraonline.fps;

import android.content.*;
import android.os.*;

/** Binds only to an already running, non-exported service. It cannot start a profile. */
final class ProfileCommands {
    static final int CALLBACK=101, MINIMIZE=102, PING=103, TILE=104, DIAGNOSTICS=105, SAVER=106;
    interface Result { void complete(boolean accepted); }
    static void send(Context context,int slot,int command,String address,Result result){
        new Request(context.getApplicationContext(),slot,command,address,result).start();
    }
    static void sendData(Context context,int slot,int command,Bundle data,Result result){
        Request request=new Request(context.getApplicationContext(),slot,command,null,result);request.details=new Bundle(data);request.start();
    }
    private static final class Request implements ServiceConnection {
        final Context context; final int slot,command; final Result result;
        final Handler handler=new Handler(Looper.getMainLooper());
        String address; boolean bound,done;
        Bundle details=new Bundle();
        final Runnable timeout=()->finish(false);
        Request(Context context,int slot,int command,String address,Result result){this.context=context;this.slot=ProfileIdentity.checked(slot);this.command=command;this.address=address;this.result=result;}
        void start(){
            try{bound=context.bindService(ProfileComponents.serviceIntent(context,slot,"profile_command"),this,0);}
            catch(Exception unavailable){bound=false;}
            if(!bound)finish(false);else handler.postDelayed(timeout,2500);
        }
        @Override public void onServiceConnected(ComponentName name,IBinder binder){
            if(done)return;
            try{
                Message message=Message.obtain(null,command);Bundle payload=new Bundle(details);payload.putString("address",address);message.setData(payload);
                message.replyTo=new Messenger(new Handler(Looper.getMainLooper(),reply->{if(reply.what==command)finish(reply.arg1==1);return true;}));
                new Messenger(binder).send(message);address=null;
            }catch(Exception unavailable){finish(false);}
        }
        @Override public void onServiceDisconnected(ComponentName name){finish(false);}
        @Override public void onBindingDied(ComponentName name){finish(false);}
        @Override public void onNullBinding(ComponentName name){finish(false);}
        void finish(boolean accepted){
            if(done)return;done=true;address=null;handler.removeCallbacks(timeout);
            if(bound){bound=false;try{context.unbindService(this);}catch(Exception ignored){}}
            result.complete(accepted);
        }
    }
}
