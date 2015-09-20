package me.piebridge.rebootwithoutsu;

import android.app.ActivityThread;
import android.app.AndroidAppHelper;
import android.app.Application;
import android.content.Context;
import android.content.IntentFilter;

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
        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
            if (!systemHooked) {
                ClassLoader loader = Thread.currentThread().getContextClassLoader();
                Class<?> packageManagerService = Class.forName("com.android.server.pm.PackageManagerService", false, loader);
                XposedHelpers.findAndHookMethod(packageManagerService, "systemReady", new SystemReadyHook());
                systemHooked = true;
            }
        }

        public static class SystemReadyHook extends XC_MethodHook {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                Context mContext = (Context) XposedHelpers.getObjectField(param.thisObject, "mContext");
                mContext.registerReceiver(new RebootHookReceiver(), new IntentFilter(RebootHookReceiver.ACTION));
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
                    context.sendBroadcast(RebootHookReceiver.newSoftRebootIntent(command));
                    param.setResult(0);
                } else if ("reboot".equals(command)) {
                    context.sendBroadcast(RebootHookReceiver.newRebootIntent(command));
                    param.setResult(0);
                }
            }
        }

    }


}
