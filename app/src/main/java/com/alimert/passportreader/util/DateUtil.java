package com.alimert.passportreader.util;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class DateUtil {

    public static Date stringToDate(String dateStr, DateFormat dateFormat) {
        Date date = null;
        try {
            date = dateFormat.parse(dateStr);
        } catch (ParseException e) {
            e.printStackTrace();
        }

        return date;
    }

    public static String dateToString(Date date, DateFormat dateFormat) {
        return dateFormat.format(date);
    }

    public static String convertFromMrzDate(String mrzDate) {
        Date date = stringToDate(mrzDate, new SimpleDateFormat("yyMMdd"));
        return dateToString(date, new SimpleDateFormat("dd.MM.yyyy"));
    }
}
