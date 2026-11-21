package com.timefold.common.util;

import com.timefold.config.AppProperties;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Service;

@Service
public class MessageEncryptionService {

	private static final String TRANSFORMATION = "AES/GCM/NoPadding";
	private static final int IV_LENGTH_BYTES = 12;
	private static final int TAG_LENGTH_BITS = 128;

	private final SecureRandom secureRandom = new SecureRandom();
	private final short currentKeyVersion;
	private final SecretKey secretKey;

	public MessageEncryptionService(AppProperties appProperties) {
		this.currentKeyVersion = (short) appProperties.encryption().currentKeyVersion();
		byte[] keyBytes = Base64.getDecoder().decode(appProperties.encryption().secretKeyBase64());
		if (keyBytes.length != 32) {
			throw new IllegalStateException(
					"app.encryption.secret-key-base64 must decode to exactly 32 bytes (256 bits) for AES-256-GCM.");
		}
		this.secretKey = new SecretKeySpec(keyBytes, "AES");
	}

	public EncryptedPayload encrypt(String plaintext) {
		try {
			byte[] iv = new byte[IV_LENGTH_BYTES];
			secureRandom.nextBytes(iv);

			Cipher cipher = Cipher.getInstance(TRANSFORMATION);
			cipher.init(Cipher.ENCRYPT_MODE, secretKey, new GCMParameterSpec(TAG_LENGTH_BITS, iv));
			byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

			return new EncryptedPayload(Base64.getEncoder().encodeToString(ciphertext), iv, currentKeyVersion);
		} catch (GeneralSecurityException ex) {
			throw new IllegalStateException("Failed to encrypt capsule message", ex);
		}
	}

	public String decrypt(String storedContent, byte[] iv, Short keyVersion) {
		if (keyVersion == null || iv == null || iv.length == 0) {
			return storedContent;
		}
		try {
			byte[] ciphertext = Base64.getDecoder().decode(storedContent);
			Cipher cipher = Cipher.getInstance(TRANSFORMATION);
			cipher.init(Cipher.DECRYPT_MODE, secretKey, new GCMParameterSpec(TAG_LENGTH_BITS, iv));
			byte[] plaintext = cipher.doFinal(ciphertext);
			return new String(plaintext, StandardCharsets.UTF_8);
		} catch (GeneralSecurityException ex) {
			throw new IllegalStateException("Failed to decrypt capsule message", ex);
		}
	}

	public record EncryptedPayload(String ciphertextBase64, byte[] iv, short keyVersion) {
	}

}