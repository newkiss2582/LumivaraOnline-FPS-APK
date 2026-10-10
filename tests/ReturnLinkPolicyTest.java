import com.lumivaraonline.fps.ReturnLinkPolicy;
import static com.lumivaraonline.fps.ReturnLinkPolicy.Status.*;

public class ReturnLinkPolicyTest {
    private static void check(boolean result) { if (!result) throw new AssertionError(); }
    public static void main(String[] args) {
        check(ReturnLinkPolicy.status(36, true, true, 1) == READY);
        check(ReturnLinkPolicy.status(31, true, true, 2) == READY);
        check(ReturnLinkPolicy.status(36, true, false, 1) == DISABLED);
        check(ReturnLinkPolicy.status(36, true, false, 2) == DISABLED);
        check(ReturnLinkPolicy.status(36, true, true, 0) == UNSELECTED);
        check(ReturnLinkPolicy.status(36, true, true, 1024) == UNSELECTED);
        check(ReturnLinkPolicy.status(36, false, true, 1) == UNKNOWN);
        check(ReturnLinkPolicy.status(36, true, true, null) == UNKNOWN);
        check(ReturnLinkPolicy.status(26, false, false, null) == LEGACY);
        for (ReturnLinkPolicy.Status status : ReturnLinkPolicy.Status.values())
            check(ReturnLinkPolicy.canAttempt(status) == (status == READY || status == LEGACY));
        System.out.println("PASS: disabled, unselected and unavailable host states do not masquerade as ready; legacy Android remains distinct.");
    }
}
