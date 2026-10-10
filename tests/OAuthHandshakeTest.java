import com.lumivaraonline.fps.OAuthHandshake;
import java.net.URLEncoder;

public class OAuthHandshakeTest {
    private static final String STATE="example-state-with-32-characters";
    private static String authorization()throws Exception{return "https://accounts.google.com/o/oauth2/v2/auth?response_type=code&redirect_uri="+URLEncoder.encode(OAuthHandshake.CALLBACK,"UTF-8")+"&state="+STATE;}
    private static String callback(){return OAuthHandshake.CALLBACK+"?state="+STATE+"&code=mock-authorization-code";}
    private static void check(boolean ok,String label){if(!ok)throw new AssertionError(label);}
    public static void main(String[] args)throws Exception{
        check(OAuthHandshake.isStart("https://lumivaraonline.com/auth/google?link=1"),"start");
        check(!OAuthHandshake.isStart(OAuthHandshake.CALLBACK),"callback cannot start auth");
        OAuthHandshake handshake=new OAuthHandshake(authorization(),1000);
        check(!handshake.accept(callback().replace(STATE,"wrong-state"),2000),"reject wrong state");
        check(!handshake.accept(callback().replace("lumivaraonline.com","evil.example"),2000),"reject wrong host");
        check(!handshake.accept(callback()+"&state="+STATE,2000),"reject duplicate state");
        check(!handshake.accept(callback().replace("https:","http:"),2000),"reject insecure callback");
        check(!handshake.accept(callback().replace("lumivaraonline.com","user@lumivaraonline.com"),2000),"reject userinfo");
        check(!handshake.accept(callback().replace("/auth/google/callback","/other"),2000),"reject wrong path");
        check(handshake.accept(callback(),2000),"valid state-bound callback accepted");
        check(!handshake.accept(callback(),2100),"reject replay");
        check(!new OAuthHandshake(authorization(),1000).accept(callback(),OAuthHandshake.TIMEOUT_MS+1001),"reject expired callback");
        check(!new OAuthHandshake(authorization(),1000).accept(callback(),999),"reject clock reversal");
        check(new OAuthHandshake(authorization(),1000).accept(OAuthHandshake.CALLBACK+"?state="+STATE+"&error=access_denied",2000),"handle user cancellation via server");
        boolean invalid=false;try{new OAuthHandshake(authorization().replace("accounts.google.com","accounts.google.com.evil.example"),1000);}catch(IllegalArgumentException e){invalid=true;}check(invalid,"reject fake provider");
        System.out.println("PASS: OAuth state binding, fixed provider/redirect, callback host/path/HTTPS validation, expiry, single-use, duplicates, user cancellation.");
    }
}
