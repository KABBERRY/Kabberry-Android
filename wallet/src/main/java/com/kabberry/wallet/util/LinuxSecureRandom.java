package com.kabberry.wallet.util;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.Provider;
import java.security.SecureRandomSpi;
import java.security.Security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LinuxSecureRandom extends SecureRandomSpi {
    private static final Logger log = LoggerFactory.getLogger(LinuxSecureRandom.class);
    private static final FileInputStream urandom;
    private final DataInputStream dis = new DataInputStream(urandom);

    private static class LinuxSecureRandomProvider extends Provider {
        public LinuxSecureRandomProvider() {
            super("LinuxSecureRandom", 1.0d, "A Linux specific random number provider that uses /dev/urandom");
            put("SecureRandom.LinuxSecureRandom", LinuxSecureRandom.class.getName());
        }
    }

    static {
        try {
            File file = new File("/dev/urandom");
            urandom = new FileInputStream(file);
            if (urandom.read() == -1) {
                throw new RuntimeException("/dev/urandom not readable?");
            } else if (Security.insertProviderAt(new LinuxSecureRandomProvider(), 1) != -1) {
                log.info("Secure randomness will be read from {} only.", file);
            } else {
                log.info("Randomness is already secure.");
            }
        } catch (FileNotFoundException e) {
            log.error("/dev/urandom does not appear to exist or is not openable");
            throw new RuntimeException(e);
        } catch (IOException e2) {
            log.error("/dev/urandom does not appear to be readable");
            throw new RuntimeException(e2);
        }
    }

    protected void engineSetSeed(byte[] bytes) {
    }

    protected void engineNextBytes(byte[] bytes) {
        try {
            this.dis.readFully(bytes);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    protected byte[] engineGenerateSeed(int i) {
        byte[] bits = new byte[i];
        engineNextBytes(bits);
        return bits;
    }
}
