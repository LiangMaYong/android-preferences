package com.liangmayong.preferences;

import android.annotation.SuppressLint;
import android.app.Application;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;

import com.liangmayong.preferences.annotations.PreferenceValue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
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
     * bind
     *
     * @param obj obj
     */
    public static void bind(Object obj) {
        if (null == obj)
            return;
        Class<?> cl = obj.getClass();
        initFields(cl.getDeclaredFields(), obj);
    }

    /**
     * initFields
     *
     * @param allField allField
     * @param object   object
     */
    private static void initFields(Field[] allField, Object object) {
        for (final Field field : allField) {
            // preference
            if (isPreferenceValue(field)) {
                PreferenceValue xkPreference = field.getAnnotation(PreferenceValue.class);
                try {
                    String key = xkPreference.value();
                    String name = xkPreference.name();
                    String initValue = xkPreference.initValue();
                    Object value = null;
                    Preferences preferences = null;
                    if ("defualt".equals(name)) {
                        preferences = Preferences.getDefaultPreferences();
                    } else {
                        preferences = Preferences.getPreferences(name);
                    }
                    if (!preferences.contains(key)) {
                        preferences.setString(key, initValue);
                    }
                    if (preferences != null) {
                        if (field.getType() == String.class) {
                            value = preferences.getString(key, "");
                        } else if (field.getType() == int.class || field.getType() == Integer.class) {
                            value = preferences.getInt(key, 0);
                        } else if (field.getType() == boolean.class || field.getType() == Boolean.class) {
                            value = preferences.getBoolean(key, false);
                        } else if (field.getType() == long.class || field.getType() == Long.class) {
                            value = preferences.getLong(key, 0);
                        } else if (field.getType() == float.class || field.getType() == Float.class) {
                            value = preferences.getFloat(key, 0);
                        }
                    }
                    field.setAccessible(true);
                    field.set(object, value);
                } catch (Exception e) {
                }
            }
        }
    }

    /**
     * isPreferenceValue
     *
     * @param field field
     * @return true or false
     */
    private static boolean isPreferenceValue(Field field) {
        return field.isAnnotationPresent(PreferenceValue.class);
    }

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

    private Preferences(String sharedPreferencesName) {
        this.sharedPreferencesName = "preferences_" + sharedPreferencesName;
    }

    // sharedPreferencesName
    private String sharedPreferencesName = "";

    // preferencesValueMap
    private Map<String, String> preferencesValueMap = new HashMap<String, String>();
    // sharedPreferences
    private SharedPreferences sharedPreferences;
    // preferenceChangeListeners
    private List<OnPreferenceChangeListener> preferenceChangeListeners = new ArrayList<OnPreferenceChangeListener>();

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
     * getSharedPreferences
     *
     * @return sharedPreferences
     */
    private SharedPreferences getSharedPreferences() {
        if (sharedPreferences == null) {
            synchronized (this) {
                sharedPreferences = getApplication().getSharedPreferences(sharedPreferencesName, 0);
                sharedPreferences.registerOnSharedPreferenceChangeListener(new OnSharedPreferenceChangeListener() {
                    @Override
                    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
                        for (int i = 0; i < preferenceChangeListeners.size(); i++) {
                            if (preferenceChangeListeners.get(i) != null) {
                                preferenceChangeListeners.get(i).onChange(Preferences.this, key);
                            }
                        }
                    }
                });
            }
        }
        return sharedPreferences;
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
        String mString = "";
        try {
            mString = new String(Des.decrypt(getSharedPreferences().getString(key, defValue), key));
        } catch (Exception e) {
        }
        return mString;
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
        int mInt = 0;
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
        boolean retu = false;
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
        float retu = 0;
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
        long retu = 0;
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
        preferencesValueMap.put(key, value);
        try {
            SharedPreferences.Editor editor = getSharedPreferences().edit();
            editor.putString(key, Des.encrypt(value.getBytes(), key));
            editor.commit();
        } catch (Exception e) {
        }
        return this;
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