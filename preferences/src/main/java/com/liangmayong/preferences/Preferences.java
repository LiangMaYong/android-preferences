package com.liangmayong.preferences;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.ActivityManager;
import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Process;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * Preferences
 *
 * @author LiangMaYong
 * @version 1.0
 */
public class Preferences {

    /**
     * OnPreferenceChangeListener
     */
    public static interface OnPreferenceChangeListener {
        void onChange(Preferences preference, String key);
    }

    // application
    private static Application application;

    /**
     * getApplication
     *
     * @return application
     */
    private static Application getApplication() {
        if (application == null) {
            synchronized (Preferences.class) {
                if (application == null) {
                    try {
                        Class<?> clazz = Class.forName("android.app.ActivityThread");
                        Method currentActivityThread = clazz.getDeclaredMethod("currentActivityThread");
                        if (currentActivityThread != null) {
                            Object object = currentActivityThread.invoke(null);
                            if (object != null) {
                                Method getApplication = object.getClass().getDeclaredMethod("getApplication");
                                if (getApplication != null) {
                                    application = (Application) getApplication.invoke(object);
                                }
                            }
                        }
                    } catch (Exception e) {
                    }
                }
            }
        }
        return application;
    }

    // perferencesMap
    private static final Map<String, Preferences> perferencesMap = new HashMap<String, Preferences>();
    // DEFAULT_PREFERENCES_NAME
    private static final String DEFAULT_PREFERENCES_NAME = "default";
    private static final String ANDROID_PREFERENCES_REFRESH_ACTION = ".android_preferences_refresh_action";

    /**
     * getDefault
     *
     * @return preferences
     */
    public static Preferences getDefaultPreferences() {
        return getPreferences(DEFAULT_PREFERENCES_NAME);
    }

    /**
     * get
     *
     * @param name name
     * @return preferences
     */
    @SuppressLint("DefaultLocale")
    public static Preferences getPreferences(String name) {
        if (name == null || "".equals(name)) {
            name = DEFAULT_PREFERENCES_NAME;
        }
        if (perferencesMap.containsKey(name)) {
            return perferencesMap.get(name);
        } else {
            Preferences preferences = new Preferences(name);
            perferencesMap.put(name, preferences);
            return preferences;
        }
    }

    /**
     * getCurrentProcessName
     *
     * @param context
     * @return process name
     */
    @TargetApi(Build.VERSION_CODES.CUPCAKE)
    private static String getCurrentProcessName(Context context) {
        int pid = Process.myPid();
        ActivityManager mActivityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        @SuppressWarnings("rawtypes")
        Iterator i$ = mActivityManager.getRunningAppProcesses().iterator();
        ActivityManager.RunningAppProcessInfo appProcess;
        do {
            if (!i$.hasNext()) {
                return null;
            }
            appProcess = (ActivityManager.RunningAppProcessInfo) i$.next();
        } while (appProcess.pid != pid);
        return appProcess.processName;
    }

    private Preferences(String sharedPreferencesName) {
        this.sharedPreferencesName = "preferences_" + sharedPreferencesName;
        IntentFilter filter = new IntentFilter();
        filter.addAction(getApplication().getPackageName() + ANDROID_PREFERENCES_REFRESH_ACTION + "." + this.sharedPreferencesName);
        getApplication().registerReceiver(new PreferencesReceiver(), filter);
    }

    // sharedPreferencesName
    private String sharedPreferencesName = "";

    // preferencesValueMap
    private Map<String, String> preferencesValueMap = new HashMap<String, String>();
    // preferenceChangeListeners
    private List<OnPreferenceChangeListener> preferenceChangeListeners = new ArrayList<OnPreferenceChangeListener>();

    /**
     * change
     *
     * @param key key
     */
    private void onChange(String key) {
        for (int i = 0; i < preferenceChangeListeners.size(); i++) {
            preferenceChangeListeners.get(i).onChange(this, key);
        }
    }

    /**
     * registerOnPreferenceChangeListener
     *
     * @param preferenceChangeListener preferenceChangeListener
     */
    public void registerOnPreferenceChangeListener(OnPreferenceChangeListener preferenceChangeListener) {
        if (preferenceChangeListener == null) {
            return;
        }
        if (preferenceChangeListeners.contains(preferenceChangeListener)) {
            return;
        }
        preferenceChangeListeners.add(preferenceChangeListener);
    }

    /**
     * unregisterOnPreferenceChangeListener
     *
     * @param preferenceChangeListener preferenceChangeListener
     */
    public void unregisterOnPreferenceChangeListener(OnPreferenceChangeListener preferenceChangeListener) {
        if (preferenceChangeListener == null) {
            return;
        }
        if (preferenceChangeListeners.contains(preferenceChangeListener)) {
            preferenceChangeListeners.remove(preferenceChangeListener);
        }
    }

    /**
     * getSharePreferences
     *
     * @return preferences
     */
    private SharedPreferences getSharedPreferences() {
        Context context = null;
        try {
            context = getApplication().createPackageContext(getApplication().getPackageName(),
                    Context.CONTEXT_IGNORE_SECURITY);
        } catch (Exception e) {
            context = getApplication();
        }
        return context.getSharedPreferences(sharedPreferencesName, 0 | 2 | 4);
    }

    /**
     * get string
     *
     * @param key      key
     * @param defValue defValue
     * @return string
     */
    public String getString(String key, String defValue) {
        if (preferencesValueMap.containsKey(key)) {
            try {
                return (String) preferencesValueMap.get(key);
            } catch (Exception e) {
            }
        }
        if (contains(key)) {
            String mString = "";
            try {
                mString = new String(Des.decrypt(getSharedPreferences().getString(key, defValue), key));
            } catch (Exception e) {
            }
            return mString;
        }
        return defValue;
    }

    /**
     * getPreferencesObject
     *
     * @param key
     * @param clazz
     * @return
     */
    public <T extends PreferencesObject> T getPreferencesObject(String key, Class<T> clazz) {
        if (contains(key)) {
            try {
                Constructor<T> classConstructor = clazz.getDeclaredConstructor();
                classConstructor.setAccessible(true);
                Object[] objects = new Object[classConstructor.getParameterTypes().length];
                for (int i = 0; i < objects.length; i++) {
                    objects[i] = null;
                }
                T t = (T) classConstructor.newInstance(objects);
                t.writeToObject(getString(key));
                return t;
            } catch (Exception e) {
            }
        }
        return null;
    }

    /**
     * setPreferencesObject
     *
     * @param key    key
     * @param object object
     */
    public void setPreferencesObject(String key, PreferencesObject object) {
        setString(key, object.toValue());
    }

    /**
     * contains
     *
     * @param key key
     * @return contains
     */
    public boolean contains(String key) {
        return getSharedPreferences().contains(key);
    }

    /**
     * getString
     *
     * @param key key
     * @return string
     */
    public String getString(String key) {
        return getString(key, "");
    }

    /**
     * get int
     *
     * @param key      key
     * @param defValue defValue
     * @return int
     */
    public int getInt(String key, int defValue) {
        int mInt = defValue;
        try {
            String string = getString(key, defValue + "");
            mInt = Integer.parseInt(string);
        } catch (Exception e) {
        }
        return mInt;
    }

    /**
     * get boolean
     *
     * @param key      key
     * @param defValue defValue
     * @return boolean
     */
    @SuppressLint("DefaultLocale")
    public boolean getBoolean(String key, boolean defValue) {
        boolean retu = defValue;
        try {
            retu = "Yes".equals(getString(key, defValue ? "Yes" : "No"))
                    || "TRUE".toUpperCase().equals(getString(key, defValue ? "Yes" : "No"));
        } catch (Exception e) {
        }
        return retu;
    }

    /**
     * get float
     *
     * @param key      key
     * @param defValue defValue
     * @return float
     */
    public float getFloat(String key, float defValue) {
        float retu = defValue;
        try {
            String string = getString(key, defValue + "");
            retu = Float.parseFloat(string);
        } catch (Exception e) {
        }
        return retu;
    }

    /**
     * get long
     *
     * @param key      key
     * @param defValue defValue
     * @return long
     */
    public long getLong(String key, long defValue) {
        long retu = defValue;
        try {
            String string = getString(key, defValue + "");
            retu = Long.parseLong(string);
        } catch (Exception e) {
        }
        return retu;
    }

    /**
     * set string
     *
     * @param key   key
     * @param value value
     * @return preference
     */
    public Preferences setString(String key, String value) {
        setString(key, value, true);
        return this;
    }

    /**
     * setString
     *
     * @param key     key
     * @param value   value
     * @param process process
     * @return v
     */
    private Preferences setString(String key, String value, boolean process) {
        if (!preferencesValueMap.containsKey(key) || !preferencesValueMap.get(key).equals(value)) {
            onChange(key);
        }
        preferencesValueMap.put(key, value);
        try {
            SharedPreferences.Editor editor = getSharedPreferences().edit();
            editor.putString(key, Des.encrypt(value.getBytes(), key));
            editor.commit();
        } catch (Exception e) {
        }
        if (process) {
            Intent intent = new Intent(getApplication().getPackageName() + ANDROID_PREFERENCES_REFRESH_ACTION + "." + this.sharedPreferencesName);
            intent.putExtra("process", getCurrentProcessName(getApplication()));
            intent.putExtra("key", key);
            intent.putExtra("value", value);
            getApplication().sendBroadcast(intent);
        }
        return this;
    }

    /**
     * PreferencesReceiver
     */
    private class PreferencesReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String process = intent.getStringExtra("process");
            if (process != null && !process.equals(getCurrentProcessName(context))) {
                String key = intent.getStringExtra("key");
                if (key != null && !"".equals(key)) {
                    String value = intent.getStringExtra("value");
                    setString(key, value, false);
                }
            }
        }
    }

    /**
     * set long
     *
     * @param key   key
     * @param value value
     * @return preference
     */
    public Preferences setLong(String key, long value) {
        setString(key, value + "");
        return this;
    }

    /**
     * set float
     *
     * @param key   key
     * @param value value
     * @return preference
     */
    public Preferences setFloat(String key, float value) {
        setString(key, value + "");
        return this;
    }

    /**
     * set boolean
     *
     * @param key   key
     * @param value value
     * @return preference
     */
    public Preferences setBoolean(String key, boolean value) {
        setString(key, value ? "Yes" : "No");
        return this;
    }

    /**
     * set int
     *
     * @param key   key
     * @param value value
     * @return preference
     */
    public Preferences setInt(String key, int value) {
        setString(key, value + "");
        return this;
    }

    /**
     * remove
     *
     * @param key key
     * @return preference
     */
    public Preferences remove(String key) {
        try {
            SharedPreferences.Editor editor = getSharedPreferences().edit();
            editor.remove(key);
            editor.commit();
        } catch (Exception e) {
        }
        return this;
    }

    /**
     * Des
     *
     * @author LiangMaYong
     * @version 1.0
     */
    private static final class Des {

        private String iv = "national";
        private static Des des = null;

        private Des() {
        }

        private static Des getDes() {
            if (des == null) {
                des = new Des();
            }
            return des;
        }

        /**
         * encrypt
         *
         * @param encryptByte encryptByte
         * @param encryptKey  encryptKey
         * @return encrypt string
         */
        public static String encrypt(byte[] encryptByte, String encryptKey) {
            return getDes()._encrypt(encryptByte, getKey(encryptKey));
        }

        @SuppressLint("TrulyRandom")
        private String _encrypt(byte[] encryptByte, String encryptKey) {
            try {
                IvParameterSpec zeroIv = new IvParameterSpec(iv.getBytes());
                SecretKeySpec key = new SecretKeySpec(encryptKey.getBytes(), "DES");
                Cipher cipher = Cipher.getInstance("DES/CBC/PKCS5Padding");
                cipher.init(Cipher.ENCRYPT_MODE, key, zeroIv);
                byte[] encryptedData = cipher.doFinal(encryptByte);
                return Base64.encode(encryptedData);
            } catch (Exception e) {
            }
            return null;
        }

        /**
         * decrypt
         *
         * @param encryptString encryptString
         * @param encryptKey    encryptKey
         * @return byte[]
         */
        public static byte[] decrypt(String encryptString, String encryptKey) {
            return getDes()._decrypt(encryptString, getKey(encryptKey));
        }

        private byte[] _decrypt(String encryptString, String encryptKey) {
            try {
                byte[] encryptByte = Base64.decode(encryptString);
                IvParameterSpec zeroIv = new IvParameterSpec(iv.getBytes());
                SecretKeySpec key = new SecretKeySpec(encryptKey.getBytes(), "DES");
                Cipher cipher = Cipher.getInstance("DES/CBC/PKCS5Padding");
                cipher.init(Cipher.DECRYPT_MODE, key, zeroIv);
                return cipher.doFinal(encryptByte);
            } catch (Exception e) {
            }
            return null;
        }

        /**
         * md5 encode
         *
         * @param plain plain
         * @return string
         */
        private final static String md5(String plain) {
            String re_md5 = new String();
            try {
                MessageDigest md = MessageDigest.getInstance("MD5");
                md.update(plain.getBytes());
                byte b[] = md.digest();

                int i;

                StringBuffer buf = new StringBuffer("");
                for (int offset = 0; offset < b.length; offset++) {
                    i = b[offset];
                    if (i < 0)
                        i += 256;
                    if (i < 16)
                        buf.append("0");
                    buf.append(Integer.toHexString(i));
                }
                re_md5 = buf.toString();
            } catch (NoSuchAlgorithmException e) {
            }
            return re_md5;
        }

        /**
         * The encryptKey to 8 characters
         *
         * @param encryptKey encryptKey
         * @return string
         */
        private static String getKey(String encryptKey) {
            return md5(encryptKey).substring(4, 12);
        }

        private static class Base64 {

            private static final char[] legalChars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/"
                    .toCharArray();

            public static String encode(byte[] data) {
                int start = 0;
                int len = data.length;
                StringBuffer buf = new StringBuffer(data.length * 3 / 2);

                int end = len - 3;
                int i = start;

                while (i <= end) {
                    int d = ((((int) data[i]) & 0x0ff) << 16) | ((((int) data[i + 1]) & 0x0ff) << 8)
                            | (((int) data[i + 2]) & 0x0ff);

                    buf.append(legalChars[(d >> 18) & 63]);
                    buf.append(legalChars[(d >> 12) & 63]);
                    buf.append(legalChars[(d >> 6) & 63]);
                    buf.append(legalChars[d & 63]);

                    i += 3;
                }

                if (i == start + len - 2) {
                    int d = ((((int) data[i]) & 0x0ff) << 16) | ((((int) data[i + 1]) & 255) << 8);

                    buf.append(legalChars[(d >> 18) & 63]);
                    buf.append(legalChars[(d >> 12) & 63]);
                    buf.append(legalChars[(d >> 6) & 63]);
                    buf.append("=");
                } else if (i == start + len - 1) {
                    int d = (((int) data[i]) & 0x0ff) << 16;

                    buf.append(legalChars[(d >> 18) & 63]);
                    buf.append(legalChars[(d >> 12) & 63]);
                    buf.append("==");
                }

                return buf.toString();
            }

            public static byte[] decode(String s) {

                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                try {
                    decode(s, bos);
                } catch (IOException e) {
                    throw new RuntimeException();
                }
                byte[] decodedBytes = bos.toByteArray();
                try {
                    bos.close();
                    bos = null;
                } catch (IOException ex) {
                    System.err.println("Error while decoding BASE64: " + ex.toString());
                }
                return decodedBytes;
            }

            private static void decode(String s, OutputStream os) throws IOException {
                int i = 0;

                int len = s.length();

                while (true) {
                    while (i < len && s.charAt(i) <= ' ')
                        i++;

                    if (i == len)
                        break;

                    int tri = (decode(s.charAt(i)) << 18) + (decode(s.charAt(i + 1)) << 12)
                            + (decode(s.charAt(i + 2)) << 6) + (decode(s.charAt(i + 3)));

                    os.write((tri >> 16) & 255);
                    if (s.charAt(i + 2) == '=')
                        break;
                    os.write((tri >> 8) & 255);
                    if (s.charAt(i + 3) == '=')
                        break;
                    os.write(tri & 255);

                    i += 4;
                }
            }

            private static int decode(char c) {
                if (c >= 'A' && c <= 'Z')
                    return ((int) c) - 65;
                else if (c >= 'a' && c <= 'z')
                    return ((int) c) - 97 + 26;
                else if (c >= '0' && c <= '9')
                    return ((int) c) - 48 + 26 + 26;
                else
                    switch (c) {
                        case '+':
                            return 62;
                        case '/':
                            return 63;
                        case '=':
                            return 0;
                        default:
                            throw new RuntimeException("unexpected code: " + c);
                    }
            }
        }
    }
}