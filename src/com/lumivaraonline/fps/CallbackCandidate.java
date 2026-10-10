package com.lumivaraonline.fps;

import java.net.URI;

public final class CallbackCandidate {
    private CallbackCandidate(){}
    public static boolean valid(String address){
        if(address==null||address.length()>8192)return false;
        try{URI uri=new URI(address);return "https".equalsIgnoreCase(uri.getScheme())&&"lumivaraonline.com".equalsIgnoreCase(uri.getHost())
            &&uri.getRawUserInfo()==null&&(uri.getPort()==-1||uri.getPort()==443)&&uri.getRawFragment()==null
            &&"/auth/google/callback".equals(uri.getRawPath())&&uri.getRawQuery()!=null;}
        catch(Exception invalid){return false;}
    }
}
