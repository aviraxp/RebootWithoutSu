package me.piebridge.rebootwithoutsu;

import android.content.Intent;
import android.os.SystemProperties;
import android.util.Log;

public class RebootUtils {

    public static final String TAG = "RebootWoSu";

    public static final String ACTION = XposedMod.class.getPackage() + ".action.REBOOT";

    private static final String EXTRA_TITLE_SOFT_REBOOT = "SOFT_REBOOT";

    private static final String EXTRA_TITLE_REBOOT = "REBOOT";

    private static final String EXTRA_TITLE_REBOOT_RECOVERY = "REBOOT_RECOVERY";

    private RebootUtils() {

    }

    public static void hookIntent(Intent intent) {
        String title = intent.getStringExtra(Intent.EXTRA_TITLE);
        String text = intent.getStringExtra(Intent.EXTRA_TEXT);
        Log.d(TAG, "command: " + text);
        if (EXTRA_TITLE_SOFT_REBOOT.equals(title)) {
            SystemProperties.set("ctl.restart", "surfaceflinger");
            SystemProperties.set("ctl.restart", "zygote");
        } else if (EXTRA_TITLE_REBOOT.equals(title)) {
            SystemProperties.set("sys.powerctl", "reboot");
        }else if ((EXTRA_TITLE_REBOOT_RECOVERY).equals(title)) {
            SystemProperties.set("sys.powerctl", "reboot,recovery");
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

    public static Intent newRebootRecoveryIntent(String command) {
        return newIntent(EXTRA_TITLE_REBOOT_RECOVERY, command);
    }

}
