package com.lumivaraonline.fps;

import android.app.*;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.*;
import java.util.List;

/** User-directed settings recovery. Never modifies another app or grants itself defaults. */
public class ReturnLinkActivity extends Activity {
    private LinearLayout page;
    private ScrollView scroll;
    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }
    private String text(int id) { return AppLanguage.text(this, id); }
    @Override public void onCreate(Bundle state) { super.onCreate(state); }
    @Override protected void onResume() { super.onResume(); render(); }
    private void render() {
        int position = scroll == null ? 0 : scroll.getScrollY();
        scroll = new ScrollView(this); scroll.setFillViewport(true);
        page = new LinearLayout(this); page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(dp(20), dp(24), dp(20), dp(24)); page.setBackgroundColor(Color.rgb(20,35,71));
        scroll.addView(page); setContentView(scroll);
        label(text(R.string.login_return_title), 23, true);
        ReturnLinkPolicy.Status status = ReturnLinkSettings.status(this, getPackageName());
        int statusText;
        switch (status) {
            case READY: statusText = R.string.link_status_ready; break;
            case DISABLED: statusText = R.string.link_status_disabled; break;
            case UNSELECTED: statusText = R.string.link_status_unselected; break;
            case LEGACY: statusText = R.string.link_status_legacy; break;
            default: statusText = R.string.link_status_unknown;
        }
        label(text(statusText), 17, true);
        label(text(R.string.link_conflict_help), 16, false);
        List<String> owners = ReturnLinkSettings.otherOwners(this);
        if (!owners.isEmpty()) {
            label(text(R.string.link_other_apps), 17, true);
            for (String packageName : owners) {
                String name = ReturnLinkSettings.label(this, packageName);
                button(text(R.string.link_open_other) + " · " + name, () -> new AlertDialog.Builder(this)
                    .setTitle(name).setMessage(text(R.string.link_other_instructions))
                    .setNegativeButton(text(R.string.cancel), null)
                    .setPositiveButton(text(R.string.open_settings), (dialog, which) -> ReturnLinkSettings.openSystem(this, packageName)).show());
            }
        } else if (status != ReturnLinkPolicy.Status.READY) {
            label(text(R.string.link_no_other_detected), 14, false);
        }
        button(text(R.string.link_open_this), () -> ReturnLinkSettings.openSystem(this, getPackageName()));
        label(text(R.string.link_this_instructions), 16, false);
        button(text(R.string.link_recheck), () -> render());
        label(text(R.string.link_still_disabled), 14, false);
        button(text(R.string.link_done), () -> finish());
        scroll.post(() -> scroll.scrollTo(0, position));
    }
    private void label(String value, int size, boolean heading) {
        TextView label = new TextView(this); label.setText(value); label.setTextSize(size);
        label.setTextColor(heading ? Color.rgb(243,211,139) : Color.rgb(248,241,224));
        label.setPadding(0, dp(10), 0, dp(10)); page.addView(label);
    }
    private void button(String title, Runnable action) {
        Button button = new Button(this); button.setText(title); button.setAllCaps(false);
        button.setMinHeight(dp(52)); page.addView(button, new LinearLayout.LayoutParams(-1, -2));
        button.setOnClickListener(view -> action.run());
    }
}
