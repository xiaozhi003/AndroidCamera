package com.android.xz.util;

import android.util.Log;

public class Logs {

    private static final String TAG_CONTENT_PRINT = "%s:%s.%s:%d";
    private static boolean sIsLogEnabled = true;// 是否打开LOG
    private static String sApplicationTag = "Logs";// LOG默认TAG

    private static StackTraceElement getCurrentStackTraceElement() {
        return Thread.currentThread().getStackTrace()[4];

    }

    // 打印LOG
    public static void trace() {
        if (sIsLogEnabled) {
            Log.d(sApplicationTag, getContent(getCurrentStackTraceElement()));
        }
    }

    // 获取LOG
    private static String getContent(StackTraceElement trace) {
        return String.format(TAG_CONTENT_PRINT, sApplicationTag,
                trace.getClassName(), trace.getMethodName(),
                trace.getLineNumber());
    }

    // 获取LOG
    private static String getContents(StackTraceElement trace) {
        return String.format("%s:%s:%d", sApplicationTag,
                trace.getMethodName(), trace.getLineNumber());
    }

    // 打印默认TAG的LOG
    public static void traceStack() {
        if (sIsLogEnabled) {
            traceStack(sApplicationTag, Log.ERROR);
        }
    }

    // 打印Log当前调用栈信息
    public static void traceStack(String tag, int priority) {

        if (sIsLogEnabled) {
            StackTraceElement[] stackTrace = Thread.currentThread()
                    .getStackTrace();
            Log.println(priority, tag, stackTrace[4].toString());
            StringBuilder str = new StringBuilder();
            String prevClass = null;
            for (int i = 5; i < stackTrace.length; i++) {
                String className = stackTrace[i].getFileName();
                int idx = className.indexOf(".java");
                if (idx >= 0) {
                    className = className.substring(0, idx);
                }
                if (prevClass == null || !prevClass.equals(className)) {

                    str.append(className.substring(0, idx));

                }
                prevClass = className;
                str.append(".").append(stackTrace[i].getMethodName())
                        .append(":").append(stackTrace[i].getLineNumber())
                        .append("->");
            }
            Log.println(priority, tag, str.toString());
        }
    }

    public static void v(String msg) {
        if (sIsLogEnabled) {
            Log.v(sApplicationTag, getContents(getCurrentStackTraceElement()) + ">" + msg);
        }
    }

    public static void v(String tag, String msg) {
        if (sIsLogEnabled) {
            Log.v(tag, getContents(getCurrentStackTraceElement()) + ">" + msg);
        }
    }

    // 指定TAG和指定内容的方法
    public static void d(String tag, String msg) {
        if (sIsLogEnabled) {
            Log.d(tag, getContent(getCurrentStackTraceElement()) + ">" + msg);
        }
    }

    // 默认TAG和制定内容的方法
    public static void ds(String msg) {
        if (sIsLogEnabled) {
            Log.d(sApplicationTag, getContent(getCurrentStackTraceElement())
                    + ">" + msg);
        }
    }

    public static void d(String msg) {
        if (sIsLogEnabled) {
            // String methodname=getCurrentStackTraceElement().getMethodName()
            // Log.d(sApplicationTag,
            // getContents(getCurrentStackTraceElement()).substring(getContent(getCurrentStackTraceElement()).getStringUnicodeLength()-12,getContent(getCurrentStackTraceElement()).getStringUnicodeLength())+">"+msg);
            Log.d(sApplicationTag, getContents(getCurrentStackTraceElement())
                    + ">" + msg);
        }
    }

    // 下面的定义和上面方法相同，可以定义不同等级的Debugger
    public static void i(String tag, String msg) {
        if (sIsLogEnabled) {
            Log.i(tag, getContent(getCurrentStackTraceElement()) + ">" + msg);
        }
    }

    public static void d(String message, Object... args) {
        if (sIsLogEnabled) {
            d(String.format(message, args));
        }
    }

    public static void w(String tag, String msg) {
        if (sIsLogEnabled) {
            Log.w(tag, getContent(getCurrentStackTraceElement()) + ">" + msg);
        }
    }

    public static void e(String tag, String msg) {
        if (sIsLogEnabled) {
            Log.e(tag, getContent(getCurrentStackTraceElement()) + ">" + msg);
        }
    }

    // 默认TAG和制定内容的方法
    public static void i(String msg) {
        if (sIsLogEnabled) {
            Log.i(sApplicationTag, getContent(getCurrentStackTraceElement())
                    + ">" + msg);
        }
    }

    // 默认TAG和制定内容的方法
    public static void w(String msg) {
        if (sIsLogEnabled) {
            Log.w(sApplicationTag, getContent(getCurrentStackTraceElement())
                    + ">" + msg);
        }
    }

    // 默认TAG和制定内容的方法
    public static void e(String msg) {
        if (sIsLogEnabled) {
            Log.e(sApplicationTag, getContent(getCurrentStackTraceElement())
                    + "\n>" + msg);
            // Log.e(sApplicationTag,
            // Thread.currentThread().getStackTrace()[2].getClassName());
            // Log.e(sApplicationTag,
            // Thread.currentThread().getStackTrace()[2].getMethodName());

        }
    }

    public static void e(Exception exception) {
        if (sIsLogEnabled) {
            Log.e(sApplicationTag, getContent(getCurrentStackTraceElement())
                    + "\n>" + exception.getMessage());
            // Log.e(sApplicationTag,
            // Thread.currentThread().getStackTrace()[2].getClassName());
            // Log.e(sApplicationTag,
            // Thread.currentThread().getStackTrace()[2].getMethodName());
            exception.printStackTrace();
        }
    }

    public static void e(Exception exception, String string) {
        if (sIsLogEnabled) {
            Log.e(sApplicationTag,
                    getContent(getCurrentStackTraceElement()) + "\n>"
                            + exception.getMessage() + "\n>"
                            + exception.getStackTrace() + "   " + string);
            // Log.e(sApplicationTag,
            // Thread.currentThread().getStackTrace()[2].getClassName());
            // Log.e(sApplicationTag,
            // Thread.currentThread().getStackTrace()[2].getMethodName());
            exception.printStackTrace();
        }
    }

    public static void e(String string, Exception exception) {
        if (sIsLogEnabled) {
            Log.e(sApplicationTag,
                    getContent(getCurrentStackTraceElement()) + "\n>"
                            + exception.getMessage() + "\n>"
                            + exception.getStackTrace() + "   " + string);
            // Log.e(sApplicationTag,
            // Thread.currentThread().getStackTrace()[2].getClassName());
            // Log.e(sApplicationTag,
            // Thread.currentThread().getStackTrace()[2].getMethodName());
            exception.printStackTrace();
        }
    }

    public static void e(String tag, String message, Exception exception) {
        if (sIsLogEnabled) {
            Log.e(tag,
                    getContent(getCurrentStackTraceElement()) + "\n>"
                            + exception.getMessage() + "\n>"
                            + exception.getStackTrace() + "   " + message);
            // Log.e(sApplicationTag,
            // Thread.currentThread().getStackTrace()[2].getClassName());
            // Log.e(sApplicationTag,
            // Thread.currentThread().getStackTrace()[2].getMethodName());
            exception.printStackTrace();
        }
    }

    public static boolean issIsLogEnabled() {
        return sIsLogEnabled;
    }

    public static void setsIsLogEnabled(boolean sIsLogEnabled) {
        Logs.sIsLogEnabled = sIsLogEnabled;
    }

    public static String getsApplicationTag() {
        return sApplicationTag;
    }

    public static void setsApplicationTag(String sApplicationTag) {
        Logs.sApplicationTag = sApplicationTag;
    }

    public static String getTagContentPrint() {
        return TAG_CONTENT_PRINT;
    }

/**
 * 0x格式的日至跟踪输出
 *
 * @param tag      前导提示
 * @param bBuf      待输出buf
 * @param nStartPos 起始坐标
 * @param nDatLen   输出长度
 */
    /**
     * 0x格式的日至跟踪输出
     *
     * @param tag       前导提示
     * @param bBuf      待输出buf
     * @param nStartPos 起始坐标
     * @param nDatLen   输出长度
     */
    public static void HexOut(String tag, byte[] bBuf, int nStartPos,
                              int nDatLen) {
        if (!sIsLogEnabled)
            return;
        String szTemp = "\n";
        int row = 0;
        for (int i = nStartPos; i < (nStartPos + nDatLen); i++) {
            String szOne;
            if (bBuf[i] < 0) {
                szOne = Integer.toHexString((int) (bBuf[i] + 256));
            } else {
                szOne = Integer.toHexString((int) bBuf[i]);
            }
            if (szOne.length() == 1) {
                szOne = "0" + szOne;
            }
            szTemp += szOne + " ";
            if (i % 4 == 3) {
                if (row % 8 == 7) {
                    szTemp += "\n";
                } else {
                    szTemp += " ";
                }
                row++;
            }
        }
        Logs.d(tag, szTemp);
    }
}
