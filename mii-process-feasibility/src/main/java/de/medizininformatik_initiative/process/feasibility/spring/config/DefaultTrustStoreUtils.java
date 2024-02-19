package de.medizininformatik_initiative.process.feasibility.spring.config;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.Security;
import java.security.cert.CertificateException;

// TODO: doc
public final class DefaultTrustStoreUtils {
    private DefaultTrustStoreUtils() {
    }

    public static KeyStore loadDefaultTrustStore() {
        Path location = null;
        String type = null;
        String password = null;

        var locationProperty = System.getProperty("javax.net.ssl.trustStore");
        if ((null != locationProperty) && (locationProperty.length() > 0)) {
            var p = Paths.get(locationProperty);
            var f = p.toFile();
            if (f.exists() && f.isFile() && f.canRead()) {
                location = p;
            }
        } else {
            var javaHome = System.getProperty("java.home");
            location = Paths.get(javaHome, "lib", "security", "jssecacerts");
            if (!location.toFile().exists()) {
                location = Paths.get(javaHome, "lib", "security", "cacerts");
            }
        }

        var passwordProperty = System.getProperty("javax.net.ssl.trustStorePassword");
        if ((null != passwordProperty) && (passwordProperty.length() > 0)) {
            password = passwordProperty;
        } else {
            password = "changeit";
        }

        var typeProperty = System.getProperty("javax.net.ssl.trustStoreType");
        if ((null != typeProperty) && (typeProperty.length() > 0)) {
            type = passwordProperty;
        } else {
            type = KeyStore.getDefaultType();
        }

        KeyStore trustStore = null;
        try {
            trustStore = KeyStore.getInstance(type, Security.getProvider("SUN"));
        } catch (KeyStoreException e) {
            throw new RuntimeException(e);
        }

        try (var is = Files.newInputStream(location)) {
            trustStore.load(is, password.toCharArray());
        } catch (IOException
                 | CertificateException
                 | NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }

        return trustStore;
    }
}
