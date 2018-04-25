package com.lody.virtual.client.stub;

import java.util.Locale;

/**
 * @author Lody
 */

public class VASettings {

    public static boolean CHECK_UPDATE_NOT_COPY_APK = true;

    public static boolean PROVIDER_ONLY_FILE = true;

    /**
     * userId=0
     */
    public static boolean KEEP_ADMIN_PHONE_INFO = true;

    public static boolean ENABLE_GMS = false;

    public static boolean NEW_INTENTSENDER = false;

    /**
     * disable startForeground
     *
     * @see android.app.Service#startForeground
     */
    public static boolean DISABLE_FOREGROUND_SERVICE = false;

    /**
     * @deprecated
     * @see com.lody.virtual.client.core.VirtualCore#setSettingHandler
     * @see com.lody.virtual.client.core.SettingHandler#isUseRealDataDir(String)
     * /data/data/va/virtual/data/xxx/->/data/data/xxxx/
     */
    public static boolean USE_REAL_DATA_DIR = false;

    public static final String ACTION_BADGER_CHANGE = "com.lody.virtual.BADGER_CHANGE";
    public static String STUB_ACTIVITY = ShadowActivity.class.getName();
    public static String STUB_DIALOG = ShadowDialogActivity.class.getName();
    public static String STUB_CP = ShadowContentProvider.class.getName();
    public static String STUB_JOB = ShadowJobService.class.getName();
    public static String STUB_SERVICE = ShadowService.class.getName();

    public static String RESOLVER_ACTIVITY = ResolverActivity.class.getName();
    public static String STUB_CP_AUTHORITY = "virtual_stub_";
    public static int STUB_COUNT = 100;
    public static String[] PRIVILEGE_APPS = new String[]{
            "com.google.android.gms"
    };

    /**
     * If enable,
     * App run in VA will allowed to create shortcut on your Desktop.
     */
    public static boolean ENABLE_INNER_SHORTCUT = true;

    /**
     * If enable,
     * For example:
     * when app access '/data/data/{Package Name}' or '/data/user/0/{Package Name}',
     * we redirect it to '/data/data/{Your Host Package Name}/virtual/user/0/{Package Name}'.
     */
    public static boolean ENABLE_IO_REDIRECT = true;

    public static String getStubActivityName(int index) {
        return String.format(Locale.ENGLISH, "%s$P%d", STUB_ACTIVITY, index);
    }

    public static String getStubDialogName(int index) {
        return String.format(Locale.ENGLISH, "%s$P%d", STUB_DIALOG, index);
    }

    public static String getStubContentProviderName(int index) {
        return String.format(Locale.ENGLISH, "%s$P%d", STUB_CP, index);
    }

    public static String getStubServiceName(int index) {
        return String.format(Locale.ENGLISH, "%s$P%d", STUB_SERVICE, index);
    }

    public static String getStubAuthority(int index) {
        return String.format(Locale.ENGLISH, "%s%d", STUB_CP_AUTHORITY, index);
    }

    public static class Wifi {
        public static boolean FAKE_WIFI_STATE = false;
        public static String DEFAULT_BSSID = "66:55:44:33:22:11";
        public static String DEFAULT_MAC = "11:22:33:44:55:66";
        public static String DEFAULT_SSID = "VA_SSID";

        public static String BSSID = DEFAULT_BSSID;
        public static String MAC = DEFAULT_MAC;
        public static String SSID = DEFAULT_SSID;
    }

}
