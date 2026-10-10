package com.lumivaraonline.fps;

import java.net.URI;

/** A single URL policy shared by the game view and the transient in-app browser. */
public final class NavigationPolicy {
    public enum Route { GAME, IN_APP, GOOGLE_AUTH, BLOCKED }
    private NavigationPolicy() {}
    public static Route route(String address) {
        try {
            URI url = new URI(address);
            if (!"https".equalsIgnoreCase(url.getScheme()) || url.getHost()==null || url.getRawUserInfo()!=null)
                return Route.BLOCKED;
            String host = url.getHost();
            String path = url.getPath()==null ? "/" : url.getPath();
            if ("accounts.google.com".equalsIgnoreCase(host)) return Route.GOOGLE_AUTH;
            if ("lumivaraonline.com".equalsIgnoreCase(host) || "www.lumivaraonline.com".equalsIgnoreCase(host)) {
                if (path.equals("/auth/google") || path.startsWith("/auth/google/")) return Route.GOOGLE_AUTH;
                return Route.GAME;
            }
            return Route.IN_APP;
        } catch (Exception ignored) { return Route.BLOCKED; }
    }
}
