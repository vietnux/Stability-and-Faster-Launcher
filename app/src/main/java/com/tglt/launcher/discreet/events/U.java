/* Copyright 2016 GTSStar
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tglt.launcher.discreet.events;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityOptions;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.graphics.Point;
import android.graphics.Rect;
import android.hardware.display.DisplayManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.View;

import androidx.annotation.VisibleForTesting;

import com.tglt.launcher.discreet.BuildConfig;
import com.tglt.launcher.discreet.ActivityMain;
import com.tglt.launcher.discreet.R;
import com.tglt.launcher.discreet.Constants;
import com.tglt.launcher.discreet.core.ApplicationType;
import com.tglt.launcher.discreet.helper.FreeformHackHelper;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class U {

    private U() {}

    private static final int MAXIMIZED = 0;
    private static final int LEFT = -1;
    private static final int RIGHT = 1;

    public static final int HIDDEN = 0;
    public static final int TOP_APPS = 1;

    // From android.app.ActivityManager.StackId
    private static final int FULLSCREEN_WORKSPACE_STACK_ID = 1;
    private static final int FREEFORM_WORKSPACE_STACK_ID = 2;

    // From android.app.WindowConfiguration
    private static final int WINDOWING_MODE_FULLSCREEN = 1;
    private static final int WINDOWING_MODE_FREEFORM = 5;

    public static SharedPreferences getSharedPreferences(Context context) {
        return context.getSharedPreferences(BuildConfig.APPLICATION_ID + "_preferences", Context.MODE_PRIVATE);
    }


    public static void startFreeformHack(Context context) {
        startFreeformHack(context, false);
    }

    @TargetApi(Build.VERSION_CODES.N)
    public static void startFreeformHack(Context context, boolean checkMultiWindow) {
//        Intent freeformHackIntent = new Intent(context, InvisibleActivityFreeform.class);
//        Intent freeformHackIntent = new Intent(context, InvisibleActivityFreeform.class);
//        freeformHackIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
//                | Intent.FLAG_ACTIVITY_LAUNCH_ADJACENT
//                | Intent.FLAG_ACTIVITY_NO_ANIMATION);
//
//        if(checkMultiWindow)
//            freeformHackIntent.putExtra("check_multiwindow", true);
//
//        startActivityLowerRight(context, freeformHackIntent);
    }


    public static Point getScreenDimensions(Activity activity) {
        // Get the screen size
        Display display = activity.getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        return size;
    }

    public static void startActivityLowerCenter(Context context, Intent intent) {
        DisplayInfo display = getDisplayInfo(context);

        SharedPreferences pref = getSharedPreferences(ActivityMain.getContext());
        String column = pref.getString("list_app_column", "4-5");
        String[] colums = column.split("-");

        int factor = Integer.parseInt(colums[0]);
        int width1 = display.width / factor;
        int width2 = display.width - width1;
        int height1 = display.height / 2;
        int height2 = display.height - height1;
//        Log.e("Pagging", width1+" == "+height1+" = width2 =" +width2 +" = height2 = "+height2+" : "+factor );
        try {
            context.startActivity(intent,
                    getActivityOptionsBundle(context, ApplicationType.FREEFORM_HACK, null,
                            width1,
                            height1,
                            width2,
                            height2
                    ));
        } catch (IllegalArgumentException | SecurityException ignored) {}
    }

    public static void startActivityLowerRight(Context context, Intent intent) {
        DisplayInfo display = getDisplayInfo(context);
        try {
            context.startActivity(intent,
                    getActivityOptionsBundle(context, ApplicationType.FREEFORM_HACK, null,
                            display.width,
                            display.height,
                            display.width + 1,
                            display.height + 1
                    ));
        } catch (IllegalArgumentException | SecurityException ignored) {}
    }


    public static boolean launcherIsDefault(Context context) {
        Intent homeIntent = new Intent(Intent.ACTION_MAIN);
        homeIntent.addCategory(Intent.CATEGORY_HOME);
        ResolveInfo defaultLauncher = context.getPackageManager().resolveActivity(homeIntent, PackageManager.MATCH_DEFAULT_ONLY);

        try {
            return defaultLauncher.activityInfo.packageName.equals(context.getPackageName());
        } catch (NullPointerException e) {
            return false;
        }
    }

//
//    public static int getNavbarHeight(Context context) {
//        SharedPreferences pref = getSharedPreferences(context);
//        boolean isNavbarHidden = isShowHideNavbarSupported()
//                && LauncherHelper.getInstance().isOnSecondaryHomeScreen(context)
//                && pref.getBoolean(PREF_AUTO_HIDE_NAVBAR_DESKTOP_MODE, false);
//
//        return isNavbarHidden ? 0 : getSystemDimen(context, "navigation_bar_height");
//    }

    private static int getSystemDimen(Context context, String id) {
        context = getDisplayContext(context);

        int value = 0;
        int resourceId = context.getResources().getIdentifier(id, "dimen", "android");
        if(resourceId > 0)
            value = context.getResources().getDimensionPixelSize(resourceId);

        return value;
    }

    public static boolean canEnableFreeform() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.N;
    }

    @TargetApi(Build.VERSION_CODES.N)
    public static boolean hasFreeformSupport(Context context) {
        return canEnableFreeform()
                && (context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_FREEFORM_WINDOW_MANAGEMENT)
                || Settings.Global.getInt(context.getContentResolver(), "enable_freeform_support", 0) != 0
                || (Build.VERSION.SDK_INT <= Build.VERSION_CODES.N_MR1
                && Settings.Global.getInt(context.getContentResolver(), "force_resizable_activities", 0) != 0));
    }


    public static boolean canBootToFreeform(Context context) {
        return hasFreeformSupport(context) && isOverridingFreeformHack(context);
    }

    public static boolean isSamsungDevice() {
        return Build.MANUFACTURER.equalsIgnoreCase("Samsung");
    }

    private static boolean isNvidiaDevice() {
        return Build.MANUFACTURER.equalsIgnoreCase("NVIDIA");
    }


    private static boolean isServiceRunning(Context context, String className) {
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        for(ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if(className.equals(service.service.getClassName()))
                return true;
        }

        return false;
    }




    private static int getFullscreenWindowModeId() {
        if(getCurrentApiVersion() >= 28.0f)
            return WINDOWING_MODE_FULLSCREEN;
        else
            return FULLSCREEN_WORKSPACE_STACK_ID;
    }

    private static int getFreeformWindowModeId() {
        if(getCurrentApiVersion() >= 28.0f)
            return WINDOWING_MODE_FREEFORM;
        else
            return FREEFORM_WORKSPACE_STACK_ID;
    }

    private static String getWindowingModeMethodName() {
        if(getCurrentApiVersion() >= 28.0f)
            return "setLaunchWindowingMode";
        else
            return "setLaunchStackId";
    }

    private static ActivityOptions getActivityOptions(View view) {
        return getActivityOptions(null, null, view);
    }

    public static ActivityOptions getActivityOptions(Context context, ApplicationType applicationType, View view) {
        ActivityOptions options;
        if(view != null) {
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                options = ActivityOptions.makeClipRevealAnimation(view, 0, 0, view.getWidth(), view.getHeight());
            else
                options = ActivityOptions.makeScaleUpAnimation(view, 0, 0, view.getWidth(), view.getHeight());
        } else if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            options = ActivityOptions.makeBasic();
        else {
            try {
                Constructor<ActivityOptions> constructor = ActivityOptions.class.getDeclaredConstructor();
                constructor.setAccessible(true);
                options = constructor.newInstance();
            } catch (Exception e) {
                return null;
            }
        }

        if(applicationType == null)
            return options;

        int stackId = -1;

        switch(applicationType) {
            case APP_PORTRAIT:
            case APP_LANDSCAPE:
                if(FreeformHackHelper.getInstance().isFreeformHackActive())
                    stackId = getFreeformWindowModeId();
                else
                    stackId = getFullscreenWindowModeId();
                break;
            case APP_FULLSCREEN:
                stackId = getFullscreenWindowModeId();
                break;
            case FREEFORM_HACK:
                stackId = getFreeformWindowModeId();
                break;
            case CONTEXT_MENU:
                if(hasBrokenSetLaunchBoundsApi()
                        || (!isChromeOs(context) && getCurrentApiVersion() >= 28.0f))
                    stackId = getFullscreenWindowModeId();
                break;
        }

        try {
            Method method = ActivityOptions.class.getMethod(getWindowingModeMethodName(), int.class);
            method.invoke(options, stackId);
        } catch (Exception ignored) {}


        return options;
    }

    private static Bundle getActivityOptionsBundle(Context context,
                                                   ApplicationType applicationType,
                                                   View view,
                                                   int left,
                                                   int top,
                                                   int right,
                                                   int bottom) {
        ActivityOptions options = getActivityOptions(context, applicationType, view);
        if(options == null) return null;

        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.N)
            return options.toBundle();

        return options.setLaunchBounds(new Rect(left, top, right, bottom)).toBundle();
    }


    public static boolean isSystemApp(Context context) {
        try {
            ApplicationInfo info = context.getPackageManager().getApplicationInfo(context.getPackageName(), 0);
            int mask = ApplicationInfo.FLAG_SYSTEM | ApplicationInfo.FLAG_UPDATED_SYSTEM_APP;
            return (info.flags & mask) != 0;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    public static boolean isChromeOs(Context context) {
        return context.getPackageManager().hasSystemFeature("org.chromium.arc");
    }

    @TargetApi(Build.VERSION_CODES.O)
    public static boolean isAndroidTV(Context context) {
        return context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_LEANBACK_ONLY);
    }


    @VisibleForTesting
    public static float getBaseTaskbarSize(Context context) {
        return getBaseTaskbarSizeStart(context) ;//+ getBaseTaskbarSizeEnd(context, null);
    }

    public static float getBaseTaskbarSizeStart(Context context) {
        SharedPreferences pref = getSharedPreferences(context);
        float baseTaskbarSize = context.getResources().getDimension(R.dimen.quickbar_height);

        return baseTaskbarSize;
    }




    public static DisplayInfo getDisplayInfo(Context context) {
        return getDisplayInfo(context, false);
    }

    public static DisplayInfo getDisplayInfo(Context context, boolean fromTaskbar) {
        context = getDisplayContext(context);

        DisplayManager dm = (DisplayManager) context.getSystemService(Context.DISPLAY_SERVICE);
        Display currentDisplay = null;

        for(Display display : dm.getDisplays()) {
            currentDisplay = display;
            break;
        }

        if(currentDisplay == null)
            return new DisplayInfo(0, 0, 0, 0, 0.0);

        DisplayMetrics metrics = new DisplayMetrics();
        currentDisplay.getMetrics(metrics);

        DisplayMetrics realMetrics = new DisplayMetrics();
        currentDisplay.getRealMetrics(realMetrics);

        DisplayInfo info = new DisplayInfo(metrics.widthPixels, metrics.heightPixels, metrics.densityDpi, 0, metrics.density);


        // Workaround for incorrect display size on devices with notches in landscape mode
        if(fromTaskbar && getDisplayOrientation(context) == Configuration.ORIENTATION_LANDSCAPE)
            return info;

        boolean sameWidth = metrics.widthPixels == realMetrics.widthPixels;
        boolean sameHeight = metrics.heightPixels == realMetrics.heightPixels;

        if(sameWidth && !sameHeight) {
            info.width = realMetrics.widthPixels;
            info.height = realMetrics.heightPixels;
        }

        if(!sameWidth && sameHeight) {
            info.width = realMetrics.widthPixels;
            info.height = realMetrics.heightPixels;
        }

//        info.density = realMetrics.density;
        return info;
    }



    public static boolean isOverridingFreeformHack(Context context) {
//        Log.e("Log U", !isChromeOs(context) +" && "+ getCurrentApiVersion() +" :: "+isSamsungDevice() );
        return isFreeformModeEnabled(context);// && (!isChromeOs(context) && getCurrentApiVersion() >= 28.0f) ;
    }

    public static boolean isPlayStoreInstalled(Context context) {
        try {
            context.getPackageManager().getPackageInfo("com.android.vending", 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    public static float getCurrentApiVersion() {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            return Float.parseFloat(Build.VERSION.SDK_INT + "." + Build.VERSION.PREVIEW_SDK_INT);
        else
            return (float) Build.VERSION.SDK_INT;
    }

    public static boolean hasBrokenSetLaunchBoundsApi() {
        return getCurrentApiVersion() >= 26.0f
                && getCurrentApiVersion() < 28.0f
                && !isSamsungDevice()
                && !isNvidiaDevice();
    }

    // Returns the name of an installed package from a list of package names, in order of preference
    private static String getInstalledPackage(Context context, String... packageNames) {
        return getInstalledPackage(context, Arrays.asList(packageNames));
    }

    private static String getInstalledPackage(Context context, List<String> packageNames) {
        if(packageNames == null || packageNames.isEmpty())
            return null;

        List<String> packages = packageNames instanceof ArrayList ? packageNames : new ArrayList<>(packageNames);
        String packageName = packages.get(0);

        try {
            context.getPackageManager().getPackageInfo(packageName, 0);
            return packageName;
        } catch (PackageManager.NameNotFoundException e) {
            packages.remove(0);
            return getInstalledPackage(context, packages);
        }
    }


    @SuppressLint("PrivateApi")
    public static String getSystemProperty(String key) {
        try {
            Class<?> cls = Class.forName("android.os.SystemProperties");
            return cls.getMethod("get", String.class).invoke(null, key).toString();
        } catch (Exception e) {
            return null;
        }
    }


    public static boolean isDesktopModeSupported(Context context) {
        return Build.VERSION.SDK_INT > Build.VERSION_CODES.P
                && context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_ACTIVITIES_ON_SECONDARY_DISPLAYS);
    }

    public static boolean isDesktopModePrefEnabled(Context context) {
        if(!isDesktopModeSupported(context) || !hasFreeformSupport(context))
            return false;

        boolean desktopModePrefEnabled;

        try {
            desktopModePrefEnabled = Settings.Global.getInt(context.getContentResolver(), "force_desktop_mode_on_external_displays") == 1;
        } catch (Settings.SettingNotFoundException e) {
            desktopModePrefEnabled = false;
        }

        return desktopModePrefEnabled;
    }

    public static boolean isDesktopModeActive(Context context) {
        return isDesktopModePrefEnabled(context) && getExternalDisplayID(context) != Display.DEFAULT_DISPLAY;
    }

    private static Display getExternalDisplay(Context context) {
        DisplayManager dm = (DisplayManager) context.getSystemService(Context.DISPLAY_SERVICE);
        Display[] displays = dm.getDisplays();

        return displays[displays.length - 1];
    }

    public static int getExternalDisplayID(Context context) {
        return getExternalDisplay(context).getDisplayId();
    }




    public static boolean isFreeformModeEnabled(Context context) {
        /*
        SharedPreferences.Editor editor = sharedPref.edit();
editor.putInt(getString(R.string.saved_high_score_key), newHighScore);
editor.apply();
         */
        SharedPreferences pref = getSharedPreferences(context);
        return pref.getBoolean("freeform_hack", false) ;
    }

    public static Context getDisplayContext(Context context) {
        if(isDesktopModeActive(context))
            return context.createDisplayContext(getExternalDisplay(context));
        else
            return context.getApplicationContext();
    }

    public static int getDisplayOrientation(Context context) {
        return getDisplayContext(context).getResources().getConfiguration().orientation;
    }

    @TargetApi(Build.VERSION_CODES.N)
    public static void applyOpenInNewWindow(Context context, Intent intent) {
//        if(!isFreeformModeEnabled(context)) return;

        intent.addFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK);

        ActivityInfo activityInfo = intent.resolveActivityInfo(context.getPackageManager(), 0);
        if(activityInfo != null) {
            switch(activityInfo.launchMode) {
                case ActivityInfo.LAUNCH_SINGLE_TASK:
                case ActivityInfo.LAUNCH_SINGLE_INSTANCE:
                    intent.addFlags(Intent.FLAG_ACTIVITY_LAUNCH_ADJACENT);
                    break;
            }
        }
    }

    public static Handler newHandler() {
        return new Handler(Looper.getMainLooper());
    }

    public static CharSequence getAppName(Context context) {
        return context.getApplicationInfo().loadLabel(context.getPackageManager());
    }
}

class DisplayInfo {
    public int width;
    public int height;
    public int currentDensity;
    public int defaultDensity;
    public double density;

    DisplayInfo(int width, int height, int currentDensity, int defaultDensity, double density) {
        this.width = width;
        this.height = height;
        this.currentDensity = currentDensity;
        this.defaultDensity = defaultDensity;
        this.density = density;
    }
}

