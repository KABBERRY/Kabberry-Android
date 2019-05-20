package com.primestone.wallet.util;

import android.content.Context;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.HashSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PasswordQualityChecker {
    private static final Logger log = LoggerFactory.getLogger(PasswordQualityChecker.class);
    private final int minPasswordLength;
    private final HashSet<String> passwordList;

    public PasswordQualityChecker(Context context) {
        this(context, 10);
    }

    public PasswordQualityChecker(Context context, int minPassLength) {
        this.minPasswordLength = minPassLength;
        this.passwordList = new HashSet(10000);
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(context.getAssets().open("common_passwords.txt"), "UTF-8"));
            while (true) {
                String word = br.readLine();
                if (word != null) {
                    this.passwordList.add(word);
                } else {
                    br.close();
                    return;
                }
            }
        } catch (Throwable e) {
            log.error("Could not open common passwords file.", e);
        }
    }
}
