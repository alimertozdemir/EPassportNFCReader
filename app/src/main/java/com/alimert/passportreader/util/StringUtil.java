package com.alimert.passportreader.util;

/**
 * @author AliMertOzdemir
 * @class StringUtil
 * @created 26.11.2020
 */
public class StringUtil {

    public static String byteArrayToHex(byte[] a) {
        StringBuilder sb = new StringBuilder(a.length * 2);
        for(byte b: a)
            sb.append(String.format("%02x", b));
        return sb.toString().toUpperCase();
    }

    public static String fixPersonalNumberMrzData(String mrzData, String personalNumber) {

        if (personalNumber == null || personalNumber.isEmpty()) {
            return mrzData;
        }

        String firstPart = mrzData.split(personalNumber)[0];
        String restPart = mrzData.split(personalNumber)[1];

        if (firstPart.lastIndexOf("<") < 10) {
            firstPart += "<";
        }

        if (restPart.indexOf("<<<<") == 0) {
            restPart = restPart.substring(1);
        }

        return firstPart + personalNumber + restPart;
    }
}
