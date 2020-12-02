package com.alimert.passportreader.util;

import android.util.Log;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.util.encoders.Base64;
import org.jmrtd.Util;
import org.spongycastle.asn1.ASN1Encodable;
import org.spongycastle.asn1.ASN1Integer;
import org.spongycastle.asn1.DERSequence;

import java.io.IOException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.Provider;
import java.security.PublicKey;
import java.security.Signature;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.security.interfaces.ECPublicKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.MGF1ParameterSpec;
import java.security.spec.PSSParameterSpec;

import javax.crypto.Cipher;

/**
 * @author AliMertOzdemir
 * @class SecurityUtil
 * @created 01.12.2020
 */
public class SecurityUtil {

    private static final String TAG = SecurityUtil.class.getSimpleName();

    private static final Provider BC_PROVIDER = new BouncyCastleProvider();

    public static boolean verifyAA(PublicKey publicKey, String digestAlgorithm, String signatureAlgorithm, byte[] challenge, byte[] response) {

        boolean result = false;

        String publicKeyAlgorithm = publicKey.getAlgorithm();

        if ("RSA".equals(publicKeyAlgorithm)) {
            Log.d(TAG, "Unexpected algorithms for RSA AA: " + "digest algorithm = " + digestAlgorithm + ", signature algorithm = " + signatureAlgorithm);
            MessageDigest rsaAADigest;
            Signature rsaAASignature;
            Cipher rsaAACipher;
            PublicKey rsaPublicKey;
            try {
                rsaAADigest = MessageDigest.getInstance(digestAlgorithm);
                rsaAASignature = Signature.getInstance(signatureAlgorithm, BC_PROVIDER);
                rsaAACipher = Cipher.getInstance("RSA/NONE/NoPadding");
                rsaPublicKey = (RSAPublicKey) publicKey;

                rsaAACipher.init(Cipher.DECRYPT_MODE, rsaPublicKey);
                rsaAASignature.initVerify(rsaPublicKey);

                int digestLength = rsaAADigest.getDigestLength();

                byte[] decryptedResponse = rsaAACipher.doFinal(response);
                byte[] m1 = Util.recoverMessage(digestLength, decryptedResponse);
                rsaAASignature.update(m1);
                rsaAASignature.update(challenge);
                result = rsaAASignature.verify(response);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else if ("EC".equals(publicKeyAlgorithm) || "ECDSA".equals(publicKeyAlgorithm)) {

            MessageDigest ecdsaAADigest;
            Signature ecdsaAASignature;
            PublicKey ecdsaPublicKey;

            try {
                ecdsaAADigest = MessageDigest.getInstance("SHA-256");
                ecdsaAASignature = Signature.getInstance("SHA256withECDSA", BC_PROVIDER);
                ecdsaPublicKey = (ECPublicKey) publicKey;

                if (ecdsaAASignature == null || signatureAlgorithm != null && !signatureAlgorithm.equals(ecdsaAASignature.getAlgorithm())) {
                    Log.d(TAG, "Re-initializing ecdsaAASignature with signature algorithm " + signatureAlgorithm);
                    ecdsaAASignature = Signature.getInstance(signatureAlgorithm);
                }

                if (ecdsaAADigest == null || digestAlgorithm != null && !digestAlgorithm.equals(ecdsaAADigest.getAlgorithm())) {
                    Log.d(TAG, "Re-initializing ecdsaAADigest with digest algorithm " + digestAlgorithm);
                    ecdsaAADigest = MessageDigest.getInstance(digestAlgorithm);
                }

                ecdsaAASignature.initVerify(ecdsaPublicKey);

                if (response.length % 2 != 0) {
                    Log.d(TAG, "Active Authentication response is not of even length");
                }

                int length = response.length / 2;
                BigInteger r = Util.os2i(response, 0, length);
                BigInteger s = Util.os2i(response, length, length);

                ecdsaAASignature.update(challenge);

                try {
                    ASN1Integer asn1R = new ASN1Integer(r);
                    ASN1Integer asn1S = new ASN1Integer(s);
                    ASN1Encodable[] asn1Encodables = {asn1R, asn1S};
                    DERSequence asn1Sequence = new DERSequence(asn1Encodables);
                    result = ecdsaAASignature.verify(asn1Sequence.getEncoded());
                } catch (IOException exp) {
                    Log.e(TAG, "Unexpected exception during AA signature verification with ECDSA");
                    exp.printStackTrace();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

        }

        return result;
    }

    public static int findSaltRSA_PSS(String digestEncryptionAlgorithm, Certificate docSigningCert, byte[] eContent, byte[] signature) {

        for (int i = 0; i <= 512; i++) {
            try {
                Signature sig = Signature.getInstance(digestEncryptionAlgorithm, BC_PROVIDER);
                if (digestEncryptionAlgorithm.endsWith("withRSA/PSS")) {
                    MGF1ParameterSpec mgf1ParameterSpec = new MGF1ParameterSpec("SHA-256");
                    PSSParameterSpec pssParameterSpec = new PSSParameterSpec("SHA-256", "MGF1", mgf1ParameterSpec, i, 1);
                    sig.setParameter(pssParameterSpec);
                }

                sig.initVerify(docSigningCert);
                sig.update(eContent);
                boolean verify = sig.verify(signature);
                if (verify) {
                    return i;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

        }
        return 0;
    }

    public static String convertToPem(X509Certificate cert) throws CertificateEncodingException {
        Base64 encoder = new Base64();
        String cert_begin = "-----BEGIN CERTIFICATE-----\n";
        String end_cert = "-----END CERTIFICATE-----";

        byte[] derCert = new byte[0];
        try {
            derCert = cert.getEncoded();
        } catch (CertificateEncodingException e) {
            e.printStackTrace();
        }
        String pemCertPre = new String(encoder.encode(derCert));
        String pemCert = cert_begin + pemCertPre + end_cert;
        return pemCert;
    }
}
