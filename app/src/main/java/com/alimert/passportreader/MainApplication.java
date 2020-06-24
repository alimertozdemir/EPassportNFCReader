package com.alimert.passportreader;

import androidx.multidex.MultiDexApplication;

import org.spongycastle.jce.provider.BouncyCastleProvider;

import java.security.Security;

public class MainApplication extends MultiDexApplication {

    @Override
    public void onCreate() {
        super.onCreate();
        Security.insertProviderAt(new BouncyCastleProvider(), 1);
    }

}
