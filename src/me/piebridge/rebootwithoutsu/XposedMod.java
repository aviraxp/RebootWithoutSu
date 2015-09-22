package me.piebridge.rebootwithoutsu;

import android.app.ActivityThread;
import android.app.AndroidAppHelper;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.util.Log;

import java.util.List;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class XposedMod implements IXposedHookZygoteInit, IXposedHookLoadPackage {

    @Override
    public void initZygote(IXposedHookZygoteInit.StartupParam startupParam) throws Throwable {
        XposedBridge.hookAllMethods(ActivityThread.class, "systemMain", new SystemMainHook());
    }

    @Override
    public void handleLoadPackage(final LoadPackageParam param) throws Throwable {
        if ("de.robv.android.xposed.installer".equals(param.packageName)) {
            XC_MethodHook rebootMethodHook = new RebootMethodHook();
            Class<?> rootUtil = XposedHelpers.findClass("de.robv.android.xposed.installer.util.RootUtil", param.classLoader);
            XposedHelpers.findAndHookMethod(rootUtil, "startShell", XC_MethodReplacement.returnConstant(true));
            XposedHelpers.findAndHookMethod(rootUtil, "execute", String.class, List.class, rebootMethodHook);
            XposedHelpers.findAndHookMethod(rootUtil, "executeWithBusybox", String.class, List.class, rebootMethodHook);
        }
    }

    public static class SystemMainHook extends XC_MethodHook {

        private static boolean systemHooked;

        @Override
        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
            if (!systemHooked) {
                ClassLoader loader = Thread.currentThread().getContextClassLoader();
                Class<?> activityManagerService = Class.forName("com.android.server.am.ActivityManagerService", false, loader);
                hookAllMethods(activityManagerService, "broadcastIntent", new BroadcastHook());
                systemHooked = true;
            }
        }

        public static class BroadcastHook extends XC_MethodHook {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                Intent intent = (Intent) param.args[0x1];
                if (RebootUtils.ACTION.equals(intent.getAction())) {
                    Object caller = param.args[0];
                    Object callerApp = XposedHelpers.callMethod(param.thisObject, "getRecordForAppLocked", caller);
                    ApplicationInfo info = (ApplicationInfo) XposedHelpers.getObjectField(callerApp, "info");
                    String sender = info == null ? "" : info.packageName;
                    if ("de.robv.android.xposed.installer".equals(sender)) {
                        RebootUtils.hookIntent(intent);
                    }
                }
            }
        }

        private static void hookAllMethods(Class<?> hookClass, String methodName, XC_MethodHook callback) {
            int size = XposedBridge.hookAllMethods(hookClass, methodName, callback).size();
            if (size == 0) {
                String log = "cannot hook " + hookClass.getSimpleName() + "." + methodName;
                Log.e(RebootUtils.TAG, log);
                XposedBridge.log(log);
            } else {
                Log.d(RebootUtils.TAG, "hook " + size + " " + hookClass.getSimpleName() + "." + methodName);
            }
        }
    }

    public static class RebootMethodHook extends XC_MethodHook {

        @Override
        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
            String command = (String) param.args[0];
            Application application = AndroidAppHelper.currentApplication();
            if (command != null && application != null) {
                Context context = application.getApplicationContext();
                if (command.contains("ctl.restart")) {
                    Log.d(RebootUtils.TAG, "soft reboot, command: " + command);
                    context.sendBroadcast(RebootUtils.newSoftRebootIntent(command));
                    param.setResult(0);
                } else if ("reboot".equals(command)) {
                    Log.d(RebootUtils.TAG, "reboot, command: " + command);
                    context.sendBroadcast(RebootUtils.newRebootIntent(command));
                    param.setResult(0);
                }
            }
        }

    }


}
