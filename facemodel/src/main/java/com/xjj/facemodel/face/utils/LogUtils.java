package com.xjj.facemodel.face.utils;

import android.text.TextUtils;
import android.util.Log;

import com.xjj.facemodel.face.model.SingleBaseConfig;

public class LogUtils {

    private static boolean DEBUG = SingleBaseConfig.getBaseConfig().isDebug();

    public static void setIsDebug(boolean isDebug) {
        LogUtils.DEBUG = isDebug;
    }


    public static void d(String tag, String message) {
        if (DEBUG) {
            if (TextUtils.isEmpty(message)) {
                return;
            }
            Log.d(tag, message);
        }
    }

    public static void d(String tag, String message, Throwable tr) {
        if (DEBUG) {
            if (TextUtils.isEmpty(message)) {
                return;
            }
            Log.d(tag, message, tr);
        }
    }

    public static void i(String tag, String message) {
        if (DEBUG) {
            if (TextUtils.isEmpty(message)) {
                return;
            }
            Log.i(tag, message);
        }
    }

    public static void i(String tag, String message, Throwable tr) {
        if (DEBUG) {
            if (TextUtils.isEmpty(message)) {
                return;
            }
            Log.d(tag, message, tr);
        }
    }

    public static void w(String tag, String message) {
        if (DEBUG) {
            if (TextUtils.isEmpty(message)) {
                return;
            }
            Log.w(tag, message);
        }
    }

    public static void w(String tag, String message, Throwable tr) {
        if (DEBUG) {
            if (TextUtils.isEmpty(message)) {
                return;
            }
            Log.w(tag, message, tr);
        }
    }

    public static void e(String tag, String message) {
        if (DEBUG) {
            if (TextUtils.isEmpty(message)) {
                return;
            }
            Log.e(tag, message);
        }
    }

    public static void e(String tag, String message, Throwable tr) {
        if (DEBUG) {
            if (TextUtils.isEmpty(message)) {
                return;
            }
            Log.d(tag, message, tr);
        }
    }

    /**
     * http log method
     */
    public static void http(String className, String message) {
        if (DEBUG) {
            if (TextUtils.isEmpty(message)) {
                return;
            }
            Log.d("httpMessage", className + " : " + message);
        }
    }



}
