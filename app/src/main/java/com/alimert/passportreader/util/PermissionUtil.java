package com.alimert.passportreader.util;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;

import androidx.core.app.ActivityCompat;

public class PermissionUtil {

    public static final int REQUEST_CODE_MULTIPLE_PERMISSIONS = 100;

    public static boolean hasPermissions(Context context, String... permissions) {
        if (context != null && permissions != null && permissions.length > 0) {
            for (String permission : permissions) {
                if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
        }
        return true;
    }

    public static boolean showRationale(Activity activity, String permission) {
        if (activity != null && permission != null) {
            return ActivityCompat.shouldShowRequestPermissionRationale(activity, permission);
        }
        return true;
    }
}
