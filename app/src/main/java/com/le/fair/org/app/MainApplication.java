package com.le.fair.org.app;

import android.app.Application;

import com.onesignal.OneSignal;
import com.yandex.metrica.YandexMetrica;
import com.yandex.metrica.YandexMetricaConfig;

public class MainApplication extends Application {
    private static final String
            ONESIGNAL_APP_ID = "c08e7b14-53d4-4834-98a7-5643c296d392",
            YANDEX_ID = "2d15e03b-2a75-4477-ab38-d2b7edb3deef";

    @Override
    public void onCreate() {
        super.onCreate();

        yandexMetric();
        startOneSignal();
    }

    void yandexMetric() {
        YandexMetricaConfig config = YandexMetricaConfig.newConfigBuilder(YANDEX_ID).build();
        YandexMetrica.activate(getApplicationContext(), config);
        YandexMetrica.enableActivityAutoTracking(this);
    }

    void startOneSignal() {
        OneSignal.setLogLevel(OneSignal.LOG_LEVEL.VERBOSE, OneSignal.LOG_LEVEL.NONE);
        OneSignal.initWithContext(this);
        OneSignal.setAppId(ONESIGNAL_APP_ID);
    }
}
