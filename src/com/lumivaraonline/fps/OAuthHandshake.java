package com.lumivaraonline.fps;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Map;

/** Pure-Java validation; no passwords, auth codes or cookies are logged. */
public final class OAuthHandshake {
    public static final String CALLBACK="https://lumivaraonline.com/auth/google/callback";
    public static final long TIMEOUT_MS=10*60*1000;
    private final String state;
    private final long createdAt;
    private boolean consumed;
    public OAuthHandshake(String googleUrl,long now) {
        try {
            URI uri=new URI(googleUrl);Map<String,String> query=query(uri);
            if(!httpsHost(uri,"accounts.google.com")||!"/o/oauth2/v2/auth".equals(uri.getPath())
                ||!CALLBACK.equals(query.get("redirect_uri"))||!"code".equals(query.get("response_type")))
                throw new IllegalArgumentException("Unexpected OAuth provider or return address");
            state=query.get("state");if(state==null||state.length()<16)throw new IllegalArgumentException("Missing OAuth state");
            createdAt=now;
        }catch(Exception error){throw new IllegalArgumentException("Invalid authorization response");}
    }
    public static boolean isStart(String address) {
        try {URI u=new URI(address);return httpsHost(u,"lumivaraonline.com")&&"/auth/google".equals(u.getPath());}
        catch(Exception error){return false;}
    }
    public synchronized boolean accept(String address,long now) {
        if(consumed||now<createdAt||now-createdAt>TIMEOUT_MS)return false;
        try {
            URI uri=new URI(address);Map<String,String> query=query(uri);
            if(!httpsHost(uri,"lumivaraonline.com")||!"/auth/google/callback".equals(uri.getPath()))return false;
            String returnedState=query.get("state");
            if(returnedState==null||!MessageDigest.isEqual(state.getBytes(StandardCharsets.UTF_8),returnedState.getBytes(StandardCharsets.UTF_8)))return false;
            if(!nonempty(query.get("code"))&&!nonempty(query.get("error")))return false;
            consumed=true;return true;
        }catch(Exception error){return false;}
    }
    private static boolean nonempty(String value){return value!=null&&!value.isEmpty();}
    private static boolean httpsHost(URI uri,String host){return "https".equalsIgnoreCase(uri.getScheme())&&host.equalsIgnoreCase(uri.getHost())&&uri.getRawUserInfo()==null&&(uri.getPort()==-1||uri.getPort()==443)&&uri.getFragment()==null;}
    private static Map<String,String> query(URI uri)throws Exception {
        Map<String,String> values=new HashMap<>();String raw=uri.getRawQuery();if(raw==null)return values;
        for(String item:raw.split("&")){String[] parts=item.split("=",2);String key=URLDecoder.decode(parts[0],"UTF-8"),value=URLDecoder.decode(parts.length==2?parts[1]:"","UTF-8");if(values.put(key,value)!=null)throw new IllegalArgumentException("Duplicate query key");}
        return values;
    }
}
