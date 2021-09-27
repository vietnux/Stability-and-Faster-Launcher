package com.tglt.launcher.discreet.core;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;

import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class DefaultApps {

    //QuickBar
    public static boolean checkDefaultApps(final Context context, Application appdefault, List<String> onlyTypes) {
        Map<String, List<String>> defactivities = getDefaultActivities(context);
       // boolean addeddefault = false;
        int max = 1;
        for (List<String> tests : defactivities.values()) {
            if (tests.size() > max) max = tests.size();
        }
//        ApplicationInfo app = appdefault.activityInfo.applicationInfo;
//        String activitiesname = appdefault.name;//resolveInfos.activityInfo.name;
        for (int i = 0; i < max; i++) { // try the tests in order.
            //try the app for each one of the activities
            for (Iterator<Map.Entry<String, List<String>>> defactit = defactivities.entrySet().iterator(); defactit.hasNext(); ) {
                Map.Entry<String, List<String>> defactent = defactit.next();

                if (onlyTypes!=null && !onlyTypes.contains(defactent.getKey())) continue;

                //if we have a test, search the app name
                if (defactent.getValue().size() > i) {
                    String test = defactent.getValue().get(i);
//                            Log.e("Using: ", " " + app.packageName + " for " + activitiesname);
//                    if ( activitiesname.toLowerCase().contains(test.toLowerCase()) || app.packageName.toLowerCase().contains(test.toLowerCase()) ) {
                    if ( appdefault.name.toLowerCase().contains(test.toLowerCase()) || appdefault.apk.toLowerCase().contains(test.toLowerCase()) ) {

//                                Log.e("Using: ", info.activityInfo.icon+" " + app.packageName + " for " + activitiesname);
                        defactit.remove(); // remove this group
                        return true;
//                                break;  //we're using this app for something
                    }
                }
            }
        }

        return false;
    }

//    private static boolean contains(AppLauncher app, String test) {
//        return app.getActivityName().toLowerCase().contains(test.toLowerCase())
//                || app.getPackageName().toLowerCase().contains(test.toLowerCase());
//    }


    private static Map<String, List<String>> getDefaultActivities(Context context) {

        Map<String, List<String>> activities = new LinkedHashMap<>();
        try {
            ComponentName camapp = getpkg(context, MediaStore.ACTION_IMAGE_CAPTURE, null, null);
            //Log.d("DefApps", camapp.getPackageName() + " " + camapp.getClassName());
            activities.put("camera", Arrays.asList(camapp.getClassName(),
                    "cameraApp", "CameraActivity", "camera.Camera", ".camera2", ".camera", "kamera", "camera", "photo", "foto", "kam", "cam"));

            ComponentName phoneapp = getpkg(context, Intent.ACTION_DIAL, "tel:411", null);
            activities.put("phone", Arrays.asList(phoneapp.getClassName(), phoneapp.getPackageName(),
                    "DialtactsActivity", "dialer", "dial", "phone", "fone", "contacts"));

            ComponentName msgapp = getpkg(context, Intent.ACTION_MAIN, null, Intent.CATEGORY_APP_MESSAGING);
            activities.put("msg", Arrays.asList(msgapp.getClassName(), msgapp.getPackageName(),
                    "messag", "msg", "sms", "mms", "messen", "chat", "irc" ));


            ComponentName emailapp = getpkg(context, Intent.ACTION_SENDTO, "mailto:", null);
            activities.put("email", Arrays.asList(emailapp.getPackageName(),
                    "k9", "inbox", "outlook", ".email", ".mail", "com.google.android.gm", "mail"));

            ComponentName musicapp = getpkg(context, Intent.ACTION_VIEW, "file:", "audio/*");
            activities.put("music", Arrays.asList(musicapp.getPackageName(),
                    "com.spotify.music",
                    "com.pandora.android",
                    "com.google.android.music",
                    "org.gateshipone.odyssey",
                    "org.fitchfamily.android.symphony",
                    "ch.blinkenlights.android.vanilla",
                    "com.rhapsody",
                    "musicplayer",  "music.player",
                    "mp3", "music", "tunein", "radio", "song"));

            ComponentName browseapp = getpkg(context, Intent.ACTION_VIEW, "http://www", null);
            activities.put("browser", Arrays.asList(browseapp.getPackageName(),
                    "duckduckgo", "web.browser", "webbrowser", "opera.", ".firefox", "mozilla.fennec", "mozilla.focus", "chromium",
                    "uc.browser", "UCMobile", "brave.browser", "TunnyBrowser", "emmx", ".bing.", "chrome",
                    ".browser", "browser"));
//            ComponentName calendar = getpkg(context, Intent.ACTION_VIEW, null, Intent.CATEGORY_APP_CALENDAR);
//            activities.put("calendar", Arrays.asList(calendar.getClassName(),
//                    "com.android.calendar", "com.truyenxuatichcu.amlich", "calendar", "lunar"));
//            ComponentName gallery = getpkg(context, Intent.ACTION_PICK, null, Intent.CATEGORY_APP_GALLERY); // "content://"
//            activities.put("gallery", Arrays.asList(gallery.getPackageName(),
//                    "albums", "gallery"));

        } catch (Exception e) {
            Log.e("Sidebar Launcher", e.getMessage(), e);
        }

        return activities;
    }

    private static ComponentName getpkg(Context context, String intentaction, String intenturi, String intentcategory) {
        return getpkg(context, intentaction, intenturi, intentcategory, null);
    }

    private static ComponentName getpkg(Context context, String intentaction, String intenturi, String intentcategory, String intenttype) {

        ComponentName cn = new ComponentName("_fakename", "_fakename");

        try {
            Intent intent;
            if (intenturi == null) {
                intent = new Intent(intentaction);
            } else {
                intent = new Intent(intentaction, Uri.parse(intenturi));
            }
            if (intentcategory != null) {
                intent.addCategory(intentcategory);
            }
            if (intenttype!=null) {
                intent.setType(intenttype);
            }

            ResolveInfo resolveInfo = context.getPackageManager().resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY);

            if (resolveInfo != null) {
//                Log.d("sh", resolveInfo.activityInfo.name + " " + resolveInfo.activityInfo.packageName);
                if (resolveInfo.activityInfo.name != null && !resolveInfo.activityInfo.name.equals("com.android.internal.app.ResolverActivity")) {
                    cn = new ComponentName(resolveInfo.activityInfo.packageName, resolveInfo.activityInfo.name);
                }
            }
        } catch (Throwable t) {
            Log.e("DefApps", t.getMessage(), t);
        }
        return cn;

    }

}
