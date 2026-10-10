package com.lumivaraonline.fps;

/** Screen-off is background even when a game window was left expanded. */
public final class BackgroundFramePolicy {
    public static final int PULSE_INTERVAL_MS=17;
    private BackgroundFramePolicy(){}
    public static boolean needsFallback(boolean screenInteractive,boolean expanded,boolean fullscreen){
        return !screenInteractive||(!expanded&&!fullscreen);
    }
}
