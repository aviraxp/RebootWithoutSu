package me.piebridge.rebootwithoutsu;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.SystemProperties;
import android.util.Log;

public class RebootHookReceiver extends BroadcastReceiver {

    public static final String ACTION = XposedMod.class.getPackage() + ".action.REBOOT";

    private static final String EXTRA_TITLE_SOFT_REBOOT = "SOFT_REBOOT";

    private static final String EXTRA_TITLE_REBOOT = "REBOOT";

    @Override
    public void onReceive(Context context, Intent intent) {
        String title = intent.getStringExtra(Intent.EXTRA_TITLE);
        String text = intent.getStringExtra(Intent.EXTRA_TEXT);
        Log.d("RWS", "command: " + text);
        if (EXTRA_TITLE_SOFT_REBOOT.equals(title)) {
            SystemProperties.set("ctl.restart", "surfaceflinger");
            SystemProperties.set("ctl.restart", "zygote");
        } else if (EXTRA_TITLE_REBOOT.equals(title)) {
            SystemProperties.set("sys.powerctl", "reboot");
        }
    }

    private static Intent newIntent(String action, String command) {
        Intent intent = new Intent(ACTION);
        intent.putExtra(Intent.EXTRA_TITLE, action);
        intent.putExtra(Intent.EXTRA_TEXT, command);
        intent.setFlags(Intent.FLAG_RECEIVER_REGISTERED_ONLY);
        return intent;
    }

    public static Intent newSoftRebootIntent(String command) {
        return newIntent(EXTRA_TITLE_SOFT_REBOOT, command);
    }

    public static Intent newRebootIntent(String command) {
        return newIntent(EXTRA_TITLE_REBOOT, command);
    }

}
