package com.lumivaraonline.fps;

import android.app.Activity;
import android.content.*;
import android.content.pm.*;
import android.content.pm.verify.domain.*;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.widget.Toast;
import java.util.*;

final class ReturnLinkSettings {
    static final String HOST = "lumivaraonline.com";
    private ReturnLinkSettings() {}
    static ReturnLinkPolicy.Status status(Context context, String packageName) {
        if (Build.VERSION.SDK_INT < 31) return ReturnLinkPolicy.Status.LEGACY;
        try {
            DomainVerificationManager manager = context.getSystemService(DomainVerificationManager.class);
            DomainVerificationUserState state = manager == null ? null : manager.getDomainVerificationUserState(packageName);
            return ReturnLinkPolicy.status(Build.VERSION.SDK_INT, state != null,
                state != null && state.isLinkHandlingAllowed(), state == null ? null : state.getHostToStateMap().get(HOST));
        } catch (Exception unavailable) { return ReturnLinkPolicy.Status.UNKNOWN; }
    }
    static boolean ready(Context context) {
        return ReturnLinkPolicy.canAttempt(status(context, context.getPackageName()));
    }
    static List<String> otherOwners(Context context) {
        Set<String> candidates = new LinkedHashSet<>();
        // Inspect only handlers of the fixed callback, never the installed-app inventory.
        Intent probe = new Intent(Intent.ACTION_VIEW, Uri.parse(OAuthHandshake.CALLBACK));
        probe.addCategory(Intent.CATEGORY_BROWSABLE);
        try {
            for (ResolveInfo match : context.getPackageManager().queryIntentActivities(probe, PackageManager.MATCH_ALL)) {
                if (match.activityInfo != null) candidates.add(match.activityInfo.packageName);
            }
            ResolveInfo resolved = context.getPackageManager().resolveActivity(probe, PackageManager.MATCH_DEFAULT_ONLY);
            if (resolved != null && resolved.activityInfo != null) candidates.add(resolved.activityInfo.packageName);
        } catch (Exception unavailable) { }
        List<String> owners = new ArrayList<>();
        for (String packageName : candidates) {
            if (!packageName.equals(context.getPackageName()) && status(context, packageName) == ReturnLinkPolicy.Status.READY)
                owners.add(packageName);
        }
        return owners;
    }
    static String label(Context context, String packageName) {
        try { return context.getPackageManager().getApplicationLabel(context.getPackageManager().getApplicationInfo(packageName, 0)).toString(); }
        catch (Exception unavailable) { return packageName; }
    }
    static void openSystem(Activity activity, String packageName) {
        Uri target = Uri.parse("package:" + packageName);
        if (Build.VERSION.SDK_INT >= 31) {
            try { activity.startActivity(new Intent(Settings.ACTION_APP_OPEN_BY_DEFAULT_SETTINGS, target)); return; }
            catch (Exception unsupported) { }
        }
        // Keep the same target on OEM fallback, including when configuring the other app.
        try { activity.startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, target)); }
        catch (Exception unsupported) { Toast.makeText(activity, AppLanguage.text(activity, R.string.open_settings_manually), Toast.LENGTH_LONG).show(); }
    }
    static void openGuide(Activity activity) {
        if (FloatingGameService.active != null) FloatingGameService.active.minimizeForPage();
        activity.startActivity(new Intent(activity, ReturnLinkActivity.class));
    }
}
