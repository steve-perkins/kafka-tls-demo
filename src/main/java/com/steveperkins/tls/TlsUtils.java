package com.steveperkins.tls;

import com.bettercloud.vault.Vault;
import com.bettercloud.vault.VaultConfig;
import com.bettercloud.vault.VaultException;
import com.bettercloud.vault.api.pki.Credential;
import com.bettercloud.vault.api.pki.CredentialFormat;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.StringReader;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;

public class TlsUtils {

    public static Credential credentials(final String application) throws VaultException {
        final VaultConfig authConfig = new VaultConfig("http://127.0.0.1:8200");
        final Vault authVault = new Vault(authConfig);
        final String token = authVault.auth().loginByUserPass("vault_user", "vault_pass").getAuthClientToken();

        VaultConfig config = new VaultConfig("http://127.0.0.1:8200", token);
        Vault vault = new Vault(config);
        return vault.pki().issue(application, application + ".example.com", null, null, null, CredentialFormat.PEM).getCredential();
    }

    public static void setupStores(final String certificate, final String privateKey, final String issuingCa, final String prefix) throws Exception {
        final PEMParser certParser = new PEMParser(new StringReader(certificate));
        final PEMParser keyParser = new PEMParser(new StringReader(privateKey));
        final PEMParser caParser = new PEMParser(new StringReader(issuingCa));
        final X509Certificate cert = new JcaX509CertificateConverter().setProvider(new BouncyCastleProvider()).getCertificate((X509CertificateHolder) certParser.readObject());
        final PrivateKey key = new JcaPEMKeyConverter().setProvider(new BouncyCastleProvider()).getKeyPair((PEMKeyPair) keyParser.readObject()).getPrivate();
        final X509Certificate ca = new JcaX509CertificateConverter().setProvider(new BouncyCastleProvider()).getCertificate((X509CertificateHolder) caParser.readObject());

        final KeyStore keyStore = KeyStore.getInstance("jks");
        keyStore.load(null);
        final Certificate[] chain = { cert };
        keyStore.setKeyEntry("privateKey", key, "password123".toCharArray(), chain);
        keyStore.store(new FileOutputStream(prefix + "-keystore.jks"), "password123".toCharArray());

        final KeyStore trustStore = KeyStore.getInstance("jks");
        trustStore.load(null);
        trustStore.setCertificateEntry("issuingCA", ca);
        trustStore.store(new FileOutputStream(prefix + "-truststore.jks"), "password123".toCharArray());
    }

    private TlsUtils() {
    }

}
