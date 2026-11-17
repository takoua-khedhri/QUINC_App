package com.myapp.util;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * Hachage des mots de passe (PBKDF2-HMAC-SHA256 + sel aléatoire).
 * Format stocké : "pbkdf2$<iterations>$<sel base64>$<hash base64>".
 * Les anciens mots de passe stockés en clair restent acceptés une fois,
 * puis sont convertis (voir LoginUI).
 */
public final class PasswordUtil {

    private static final String PREFIXE = "pbkdf2$";
    private static final int ITERATIONS = 120_000;
    private static final int TAILLE_HASH_BITS = 256;

    private PasswordUtil() {}

    public static String hacher(String motDePasse) {
        byte[] sel = new byte[16];
        new SecureRandom().nextBytes(sel);
        byte[] hash = pbkdf2(motDePasse.toCharArray(), sel, ITERATIONS);
        Base64.Encoder b64 = Base64.getEncoder();
        return PREFIXE + ITERATIONS + "$" + b64.encodeToString(sel) + "$" + b64.encodeToString(hash);
    }

    public static boolean estHache(String stocke) {
        return stocke != null && stocke.startsWith(PREFIXE);
    }

    /** Vérifie un mot de passe saisi contre la valeur stockée (hachée ou, ancien format, en clair). */
    public static boolean verifier(String saisi, String stocke) {
        if (saisi == null || stocke == null) return false;
        if (!estHache(stocke)) {
            return MessageDigest.isEqual(saisi.getBytes(java.nio.charset.StandardCharsets.UTF_8),
                                         stocke.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        }
        String[] p = stocke.split("\\$");
        if (p.length != 4) return false;
        try {
            int iterations = Integer.parseInt(p[1]);
            byte[] sel = Base64.getDecoder().decode(p[2]);
            byte[] attendu = Base64.getDecoder().decode(p[3]);
            return MessageDigest.isEqual(attendu, pbkdf2(saisi.toCharArray(), sel, iterations));
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private static byte[] pbkdf2(char[] mdp, byte[] sel, int iterations) {
        try {
            PBEKeySpec spec = new PBEKeySpec(mdp, sel, iterations, TAILLE_HASH_BITS);
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
        } catch (Exception e) {
            throw new IllegalStateException("PBKDF2 indisponible", e);
        }
    }

    /** Utilitaire : java -cp ... com.myapp.util.PasswordUtil motdepasse -> affiche la valeur à stocker. */
    public static void main(String[] args) {
        if (args.length == 1) System.out.println(hacher(args[0]));
    }
}
