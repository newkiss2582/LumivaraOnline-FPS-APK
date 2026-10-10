import com.lumivaraonline.fps.OAuthHandshake;
import java.net.*;
import java.util.*;

/** Probes public auth initiation only. No user account login or credentials. */
public class AuthStartProbe {
    public static void main(String[] args)throws Exception{
        HttpURLConnection connection=(HttpURLConnection)new URL("https://lumivaraonline.com/auth/google").openConnection();
        connection.setInstanceFollowRedirects(false);connection.setConnectTimeout(15000);connection.setReadTimeout(15000);
        int status=connection.getResponseCode();if(status!=302&&status!=303)throw new AssertionError("Unexpected start status");
        new OAuthHandshake(connection.getHeaderField("Location"),1000);
        boolean cookie=false;for(Map.Entry<String,List<String>> entry:connection.getHeaderFields().entrySet())if(entry.getKey()!=null&&entry.getKey().equalsIgnoreCase("Set-Cookie"))cookie|=entry.getValue().size()>0;
        if(!cookie)throw new AssertionError("Missing auth-state cookie");
        connection.disconnect();System.out.println("PASS: live public auth-start uses supported Google endpoint, expected game callback, and state cookie. Google account login was not performed.");
    }
}
