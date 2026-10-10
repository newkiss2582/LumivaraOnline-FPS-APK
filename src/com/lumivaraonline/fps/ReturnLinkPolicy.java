package com.lumivaraonline.fps;

/** Pure interpretation of Android domain states; unavailable data never means ready. */
public final class ReturnLinkPolicy {
    public enum Status { READY, DISABLED, UNSELECTED, UNKNOWN, LEGACY }
    private ReturnLinkPolicy() {}
    public static Status status(int sdk, boolean known, boolean allowed, Integer hostState) {
        if (sdk < 31) return Status.LEGACY;
        if (!known || hostState == null) return Status.UNKNOWN;
        if (!allowed) return Status.DISABLED;
        return hostState == 1 || hostState == 2 ? Status.READY : Status.UNSELECTED;
    }
    public static boolean canAttempt(Status status) {
        return status == Status.READY || status == Status.LEGACY;
    }
}
