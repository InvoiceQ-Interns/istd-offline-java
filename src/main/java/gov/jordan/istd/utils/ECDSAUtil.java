package gov.jordan.istd.utils;

import org.bouncycastle.jce.ECNamedCurveTable;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.jce.spec.ECNamedCurveParameterSpec;

import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;

public class ECDSAUtil {

    public static KeyPair getKeyPair() throws NoSuchAlgorithmException, NoSuchProviderException, InvalidAlgorithmParameterException {
        BouncyCastleProvider bouncyCastleProvider = new BouncyCastleProvider();
        Security.insertProviderAt(bouncyCastleProvider, 1);
        BouncyCastleProvider prov = new BouncyCastleProvider();
        Security.addProvider(prov);
        ECNamedCurveParameterSpec eCNamedCurveParameterSpec = ECNamedCurveTable.getParameterSpec("secp256k1");
        KeyPairGenerator generator = KeyPairGenerator.getInstance("ECDSA", prov.getName());
        generator.initialize(eCNamedCurveParameterSpec, new SecureRandom());
        return generator.generateKeyPair();
    }

    public static PrivateKey getPrivateKey(String key) throws NoSuchAlgorithmException, InvalidKeySpecException {
        Security.addProvider(new BouncyCastleProvider());
        KeyFactory kf = KeyFactory.getInstance("EC");
        byte[] privateKeyDecrypted = Base64.getDecoder().decode(key);
        PrivateKey privateKey = kf.generatePrivate(new PKCS8EncodedKeySpec(privateKeyDecrypted));
        Security.removeProvider("BC");
        return privateKey;
    }
}
