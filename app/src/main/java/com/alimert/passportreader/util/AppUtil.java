package com.alimert.passportreader.util;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;

public class AppUtil {

    public static void showAlertDialog(Activity activity, String title, String message, String buttonText, boolean isCancelable, Dialog.OnClickListener listener) {
        showAlertDialog(activity, title, message, buttonText, null, null, isCancelable, listener);
    }

    public static void showAlertDialog(Activity activity, String title, String message, String positiveButtonText, String negativeButtonText, String neutralButtonText, boolean isCancelable,Dialog.OnClickListener listener) {
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(activity)
                .setTitle(title)
                .setMessage(message)
                .setCancelable(isCancelable);

        if (positiveButtonText != null && !positiveButtonText.isEmpty())
            dialogBuilder.setPositiveButton(positiveButtonText, listener);

        if (negativeButtonText != null && !negativeButtonText.isEmpty())
            dialogBuilder.setNegativeButton(negativeButtonText, listener);

        if (neutralButtonText != null && !neutralButtonText.isEmpty())
            dialogBuilder.setNeutralButton(neutralButtonText, listener);

        dialogBuilder.show();
    }
}
