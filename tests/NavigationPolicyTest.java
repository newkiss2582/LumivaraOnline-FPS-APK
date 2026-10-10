import com.lumivaraonline.fps.NavigationPolicy;
import com.lumivaraonline.fps.NavigationPolicy.Route;

public class NavigationPolicyTest {
    private static void expect(String url,Route expected) {
        Route actual=NavigationPolicy.route(url);
        if(actual!=expected)throw new AssertionError(url+" => "+actual+", expected "+expected);
    }
    public static void main(String[] args) {
        expect("https://lumivaraonline.com/",Route.GAME);
        expect("https://www.lumivaraonline.com/wiki/",Route.GAME);
        expect("https://lumivaraonline.com/auth/google?link=1",Route.GOOGLE_AUTH);
        expect("https://lumivaraonline.com/auth/google/callback?code=test",Route.GOOGLE_AUTH);
        expect("https://lumivaraonline.com/auth/%67oogle",Route.GOOGLE_AUTH);
        expect("https://accounts.google.com/o/oauth2/v2/auth",Route.GOOGLE_AUTH);
        expect("https://discord.gg/example",Route.IN_APP);
        expect("https://github.com/newkiss2582/Lumivara-Online-Client",Route.IN_APP);
        expect("https://accounts.google.com.evil.example/",Route.IN_APP);
        expect("http://lumivaraonline.com/",Route.BLOCKED);
        expect("intent://open/",Route.BLOCKED);
        expect("javascript:alert(1)",Route.BLOCKED);
        expect("https://user@lumivaraonline.com/",Route.BLOCKED);
        expect(null,Route.BLOCKED);
        expect("invalid URL",Route.BLOCKED);
        System.out.println("PASS: in-app HTTPS routing, game routing, Google OAuth guard, encoded paths, host boundaries, unsafe scheme and userinfo rejection.");
    }
}
