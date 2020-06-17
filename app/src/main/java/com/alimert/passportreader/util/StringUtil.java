package com.alimert.passportreader.util;

public class StringUtil {

    public static final String TYPE_PASSPORT = "P<";

    public static final String TYPE_ID_CARD = "I<";

    public static String exceptionStack(Throwable exception) {
        StringBuilder s = new StringBuilder();
        String exceptionMsg = exception.getMessage();
        if (exceptionMsg != null) {
            s.append(exceptionMsg);
            s.append(" - ");
        }
        s.append(exception.getClass().getSimpleName());
        StackTraceElement[] stack = exception.getStackTrace();

        if (stack.length > 0) {
            int count = 3;
            boolean first = true;
            boolean skip = false;
            String file = "";
            s.append(" (");
            for (StackTraceElement element : stack) {
                if (count > 0 && element.getClassName().startsWith("com.tananaev")) {
                    if (!first) {
                        s.append(" < ");
                    } else {
                        first = false;
                    }

                    if (skip) {
                        s.append("... < ");
                        skip = false;
                    }

                    if (file.equals(element.getFileName())) {
                        s.append("*");
                    } else {
                        file = element.getFileName();
                        s.append(file.substring(0, file.length() - 5)); // remove ".java"
                        count -= 1;
                    }
                    s.append(":").append(element.getLineNumber());
                } else {
                    skip = true;
                }
            }
            if (skip) {
                if (!first) {
                    s.append(" < ");
                }
                s.append("...");
            }
            s.append(")");
        }
        return s.toString();
    }
}
