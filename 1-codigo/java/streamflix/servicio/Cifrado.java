package streamflix.servicio;

import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * Cifra contraseñas con PBKDF2 (SHA-256, 65 536 vueltas y sal al azar).
 * En la base solo se guarda el resultado, nunca la contraseña escrita.
 * Formato: pbkdf2$vueltas$sal$hash
 */
public final class Cifrado {

    private static final int VUELTAS = 65536;
    private static final SecureRandom AZAR = new SecureRandom();

    private Cifrado() {}

    public static String cifrar(String clave) {
        byte[] sal = new byte[16];
        AZAR.nextBytes(sal);
        byte[] hash = pbkdf2(clave, sal, VUELTAS);
        Base64.Encoder b64 = Base64.getEncoder();
        return "pbkdf2$" + VUELTAS + "$" + b64.encodeToString(sal) + "$" + b64.encodeToString(hash);
    }

    public static boolean coincide(String clave, String guardado) {
        String[] partes = guardado.split("\\$");
        if (partes.length != 4) return false;
        int vueltas = Integer.parseInt(partes[1]);
        byte[] sal = Base64.getDecoder().decode(partes[2]);
        byte[] esperado = Base64.getDecoder().decode(partes[3]);
        return MessageDigest.isEqual(esperado, pbkdf2(clave, sal, vueltas));
    }

    private static byte[] pbkdf2(String clave, byte[] sal, int vueltas) {
        try {
            PBEKeySpec spec = new PBEKeySpec(clave.toCharArray(), sal, vueltas, 256);
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("No se pudo cifrar la contraseña", e);
        }
    }

    /** Token al azar para identificar la sesión en la cookie. */
    public static String token() {
        byte[] b = new byte[24];
        AZAR.nextBytes(b);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(b);
    }
}
