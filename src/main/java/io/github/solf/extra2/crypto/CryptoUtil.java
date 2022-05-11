/**
 * Copyright Sergey Olefir
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.solf.extra2.crypto;

import static io.github.solf.extra2.util.NullUtil.nnChecked;

import java.nio.ByteBuffer;
import java.security.*;
import java.security.spec.*;

import javax.annotation.Nullable;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

import org.eclipse.jdt.annotation.NonNullByDefault;

import io.github.solf.extra2.exception.AssertionException;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

/**
 * Utility for dealing with encryption/decryption stuff in Java.
 * <p>
 * NOTE: instances of this class are thread-safe.
 *
 * @author Sergey Olefir
 */
@NonNullByDefault
@RequiredArgsConstructor
public class CryptoUtil
{
	/**
	 * AES encryption algorithm to be used.
	 */
	private static final String aesEncryptionAlgo = "AES/GCM/NoPadding";
	
	/**
	 * Secure random generator for this class.
	 */
	private static final SecureRandom secureRandom = new SecureRandom();
	
	/**
	 * Key size for RSA when not otherwise specified (e.g. 1,024 or 2,048 or 
	 * 4,096 bits).
	 */
	@Getter
	private final int defaultRsaKeySize;
	
	/**
	 * Key size for AES when not otherwise specified (legal key sizes are 128, 192, and 256 bits)
	 */
	@Getter
	private final int defaultAesKeySize;
	
	/**
	 * Constructor (uses defaults for key sizes, 2048 RSA, 128 AES).
	 */
	public CryptoUtil()
	{
		this(2048, 128);
	}

	/**
	 * Generates RSA private/public key pair using default key size (specified
	 * in constructor).
	 */
	public KeyPair generateRsaKeyPair() throws NoSuchAlgorithmException
	{
		return generateRsaKeyPair(defaultRsaKeySize);
	}
	
	/**
	 * Generates RSA private/public key pair using requested RSA key size.
	 */
	public KeyPair generateRsaKeyPair(int rsaKeySize) throws NoSuchAlgorithmException
	{
		KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
		generator.initialize(rsaKeySize);
		
		return generator.generateKeyPair();
	}
	
	/**
	 * Encrypts given data using provided AES key (in its serialized form).
	 * <p>
	 * The encryption uses {@value #aesEncryptionAlgo} algorithm.
	 * <p> 
	 * The format of encrypted data is: [hibyte iv length][low byte iv length][iv][encryptedData]
	 */
	public byte[] aesEncrypt(@NonNull byte[] dataToEncrypt, @NonNull byte[] aesKeyBytes) throws GeneralSecurityException
	{
		return aesEncrypt(dataToEncrypt, deserializeAesKey(aesKeyBytes));
	}
	
	/**
	 * Encrypts given data using provided AES key.
	 * <p>
	 * The encryption uses {@value #aesEncryptionAlgo} algorithm.
	 * <p> 
	 * The format of encrypted data is: [hibyte iv length][low byte iv length][iv][encryptedData]
	 */
	public byte[] aesEncrypt(@NonNull byte[] dataToEncrypt, @NonNull SecretKey aesKey) throws GeneralSecurityException
	{
		Cipher aesCipher = Cipher.getInstance(aesEncryptionAlgo);
		
		// Create GCMParameterSpec
		byte[] ivBytes = new byte[12]; // 'For GCM a 12 byte IV is strongly suggested as other IV lengths will require additional calculations.'
		secureRandom.nextBytes(ivBytes);
        GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(128 /*tag length, */, ivBytes); // GCM is defined for the tag sizes 128, 120, 112, 104, or 96, 64 and 32. Note that the security of GCM is strongly dependent on the tag size. You should try and use a tag size of 64 bits at the very minimum, but in general a tag size of the full 128 bits should be preferred.
		
		aesCipher.init(Cipher.ENCRYPT_MODE, aesKey, gcmParameterSpec);
		
		byte[] encryptedData = aesCipher.doFinal(dataToEncrypt);
		
		ByteBuffer bb = ByteBuffer.allocate(encryptedData.length + ivBytes.length + 2);
		
		byte highByte = (byte)(ivBytes.length / 256);
		byte lowByte = (byte)(ivBytes.length - highByte * 256);
		
		bb.put(highByte);
		bb.put(lowByte);
		bb.put(ivBytes);
		bb.put(encryptedData);
		
		return bb.array();
	}
	
	
	/**
	 * Decrypts given data using provided AES key (in its serialized form).
	 * <p>
	 * See {@link #aesEncrypt(byte[], SecretKey)} for format description.
	 */
	public byte[] aesDecrypt(@NonNull byte[] aesEncryptedData, @NonNull byte[] aesKeyBytes) throws GeneralSecurityException
	{
		return aesDecrypt(aesEncryptedData, deserializeAesKey(aesKeyBytes));
	}
	
	/**
	 * Decrypts given data using provided AES key.
	 * <p>
	 * See {@link #aesEncrypt(byte[], SecretKey)} for format description.
	 */
	public byte[] aesDecrypt(@NonNull byte[] aesEncryptedData, @NonNull SecretKey aesKey) throws GeneralSecurityException
	{
		if (aesEncryptedData.length < 3)
			throw new InvalidParameterSpecException("Data to decrypt got invalid length: " + aesEncryptedData.length);

		Cipher aesCipher = Cipher.getInstance(aesEncryptionAlgo);
		ByteBuffer bb = ByteBuffer.wrap(aesEncryptedData);
		
		int ivLength = bb.get() * 256 + bb.get();
		
		if (bb.remaining() <= ivLength)
			throw new InvalidParameterSpecException("Data to decrypt got invalid length, IV length is [" + ivLength + "], but remaining bytes (including encrypted data) are: " + bb.remaining());
		byte[] ivBytes = new byte[ivLength];
		bb.get(ivBytes);
		byte[] restOfEncryptedData = new byte[bb.remaining()];
		bb.get(restOfEncryptedData);
		
        GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(128 /*tag length, */, ivBytes); // GCM is defined for the tag sizes 128, 120, 112, 104, or 96, 64 and 32. Note that the security of GCM is strongly dependent on the tag size. You should try and use a tag size of 64 bits at the very minimum, but in general a tag size of the full 128 bits should be preferred.
		
		aesCipher.init(Cipher.DECRYPT_MODE, aesKey, gcmParameterSpec);
		
		return aesCipher.doFinal(restOfEncryptedData);
	}
	
	/**
	 * Generates random AES key using default key size (specified in constructor).
	 */
	public SecretKey aesGenerateRandomKey() throws NoSuchAlgorithmException
	{
		return aesGenerateRandomKey(defaultAesKeySize);
	}
	
	/**
	 * Generates random AES key using provided key size.
	 */
	public SecretKey aesGenerateRandomKey(int aesKeySize) throws NoSuchAlgorithmException
	{
		KeyGenerator generator = KeyGenerator.getInstance("AES");
		generator.init(aesKeySize); // The AES key size in number of bits
		return generator.generateKey();
	}
	
	/**
	 * Generates AES key from the provided passphrase using optional salt (if
	 * salt is not specified, then preset salt of [1, 2, 3] is used).
	 * <p>
	 * Uses default key size (specified in constructor) and preset iteration
	 * count (100_000). 
	 */
	public SecretKey aesGenerateKeyFromPassphrase(@NonNull String passphrase, byte @Nullable[] salt) throws GeneralSecurityException
	{
		return aesGenerateKeyFromPassphrase(passphrase, salt, 100_000, defaultAesKeySize);
	}
	
	/**
	 * Default password salt used in case no other salt is specified, it's: {1, 2, 3}
	 */
	private final static byte[] defaultPasswordSalt = new byte[] {1, 2, 3};
	
	
	/**
	 * Generates AES key from the provided passphrase using optional salt (if
	 * salt is not specified, then preset salt of [1, 2, 3] is used).
	 * 
	 * @param iterationCount iteration count for the key generation; generally
	 * 		more iterations is more secure; something like 100_000 is reasonable
	 * 		but takes a noticeable time to generate (=lots of CPU usage); values
	 * 		under 300 are illegal 
	 */
	public SecretKey aesGenerateKeyFromPassphrase(@NonNull String passphrase, 
		final byte @Nullable[] argSalt, int iterationCount, int aesKeySize) throws GeneralSecurityException
	{
		if (iterationCount < 300)
			throw new InvalidParameterSpecException("Iteration count must be at least 300 (maybe iteration count and aes key size parameters are switched?); got: " + iterationCount);
		
		byte[] salt = (argSalt == null) ? defaultPasswordSalt : argSalt;
		
	    SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
	    KeySpec spec = new PBEKeySpec(passphrase.toCharArray(), salt, iterationCount, aesKeySize);
	    SecretKey secret = new SecretKeySpec(factory.generateSecret(spec)
	        .getEncoded(), "AES");
	    return secret;
	}
	
	/**
	 * RSA encrypts provided data using provided RSA public key (in its serialized
	 * form).
	 * 
	 * @deprecated RSA encryption is very slow and can only handle data sizes
	 * 		smaller than RSA key itself; for general-purpose assymetric
	 * 		encryption see {@link #rsaWithAesEncrypt(byte[], PublicKey)}
	 */
	@Deprecated
	public byte[] rsaEncrypt(@NonNull byte[] dataToEncrypt, @NonNull byte[] publicKeyBytes) throws GeneralSecurityException
	{
		return rsaEncrypt(dataToEncrypt, deserializeRsaPublicKey(publicKeyBytes));
	}
	
	/**
	 * RSA encrypts provided data using provided RSA public key.
	 * 
	 * @deprecated RSA encryption is very slow and can only handle data sizes
	 * 		smaller than RSA key itself; for general-purpose asymmetric
	 * 		encryption see {@link #rsaWithAesEncrypt(byte[], PublicKey)}
	 */
	@Deprecated
	public byte[] rsaEncrypt(@NonNull byte[] dataToEncrypt, @NonNull PublicKey publicKey) throws GeneralSecurityException
	{
		Cipher encryptCipher = Cipher.getInstance("RSA");
		encryptCipher.init(Cipher.ENCRYPT_MODE, publicKey);
		
		return encryptCipher.doFinal(dataToEncrypt);
	}
	
	
	/**
	 * Decrypts provided data using provided RSA private key (in its serialized
	 * form).
	 * <p>
	 * NOTE: for general-purpose asymmetric encryption/decryption see instead
	 * {@link #rsaWithAesDecrypt(byte[], PrivateKey)}, {@link #rsaWithAesEncrypt(byte[], PublicKey)}
	 */
	public byte[] rsaDecrypt(@NonNull byte[] rsaEncryptedData, @NonNull byte[] privateKeyBytes) throws GeneralSecurityException
	{
		return rsaDecrypt(rsaEncryptedData, deserializeRsaPrivateKey(privateKeyBytes));
		
	}
	
	/**
	 * Decrypts provided data using provided RSA private key.
	 * <p>
	 * NOTE: for general-purpose asymmetric encryption/decryption see instead
	 * {@link #rsaWithAesDecrypt(byte[], PrivateKey)}, {@link #rsaWithAesEncrypt(byte[], PublicKey)}
	 */
	public byte[] rsaDecrypt(@NonNull byte[] rsaEncryptedData, @NonNull PrivateKey privateKey) throws GeneralSecurityException
	{
		Cipher decryptCipher = Cipher.getInstance("RSA");
		decryptCipher.init(Cipher.DECRYPT_MODE, privateKey);
		
		return decryptCipher.doFinal(rsaEncryptedData);
	}

	/**
	 * RSA-AES asymmetric encrypts provided data using provided RSA public key 
	 * (in its serialized form).
	 * <p>
	 * This is the method to be used for general-purpose asymmetric encryption.
	 * <p> 
	 * The encrypted data format is:
	 * [hibyte key length][low byte key length][rsa-encrypted-random-aes-key]
	 * [hibyte iv length][low byte iv length][iv][aes-encryptedData(using random key from rsa section)]
	 */
	public byte[] rsaWithAesEncrypt(@NonNull byte[] dataToEncrypt, @NonNull byte[] publicKeyBytes) throws GeneralSecurityException
	{
		return rsaWithAesEncrypt(dataToEncrypt, deserializeRsaPublicKey(publicKeyBytes));
	}
	
	/**
	 * RSA-AES asymmetric encrypts provided data using provided RSA public key.
	 * <p>
	 * This is the method to be used for general-purpose asymmetric encryption.
	 * <p> 
	 * The encrypted data format is:
	 * [hibyte key length][low byte key length][rsa-encrypted-random-aes-key]
	 * [hibyte iv length][low byte iv length][iv][aes-encryptedData(using random key from rsa section)]
	 */
	public byte[] rsaWithAesEncrypt(@NonNull byte[] dataToEncrypt, @NonNull PublicKey publicKey) throws GeneralSecurityException
	{
		return rsaWithAesEncrypt(dataToEncrypt, publicKey, aesGenerateRandomKey());
	}

	/**
	 * RSA-AES asymmetric encrypts provided data using provided RSA public key
	 * AND provided AES key (instead of generating a random one for every
	 * encryption).
	 * <p>
	 * This is the method to be used for general-purpose asymmetric encryption.
	 * <p> 
	 * The encrypted data format is:
	 * [hibyte key length][low byte key length][rsa-encrypted-random-aes-key]
	 * [hibyte iv length][low byte iv length][iv][aes-encryptedData(using random key from rsa section)]
	 */
	public byte[] rsaWithAesEncrypt(@NonNull byte[] dataToEncrypt, @NonNull PublicKey publicKey, @NonNull SecretKey aesKey) throws GeneralSecurityException
	{
		byte[] aesEncryptedData = aesEncrypt(dataToEncrypt, aesKey);
		
		byte[] aesKeyBytes = serializeAesKey(aesKey);
		
		byte[] rsaEncryptedAesKey = rsaEncrypt(aesKeyBytes, publicKey);
		int keyLength = rsaEncryptedAesKey.length; 
		
		byte highByte = (byte)(keyLength / 256);
		byte lowByte = (byte)(keyLength - highByte * 256);
		
		ByteBuffer bb = ByteBuffer.allocate(aesEncryptedData.length + keyLength + 2);
		
		bb.put(highByte);
		bb.put(lowByte);
		bb.put(rsaEncryptedAesKey);
		bb.put(aesEncryptedData);
		
		return bb.array();
	}

	/**
	 * RSA-AES asymmetric decryption of the provided data using provided private
	 * key (in its serialized form).
	 * <p>
	 * This is the method to be used for general-purpose asymmetric decryption.
	 * <p>
	 * See {@link #rsaWithAesEncrypt(byte[], PublicKey)} for format description.
	 */
	public byte[] rsaWithAesDecrypt(@NonNull byte[] dataToDecrypt, @NonNull byte[] privateKeyBytes) throws GeneralSecurityException
	{
		return rsaWithAesDecrypt(dataToDecrypt, deserializeRsaPrivateKey(privateKeyBytes));
	}
	
	/**
	 * RSA-AES asymmetric decryption of the provided data using provided private key.
	 * <p>
	 * This is the method to be used for general-purpose asymmetric decryption.
	 * <p>
	 * See {@link #rsaWithAesEncrypt(byte[], PublicKey)} for format description.
	 */
	public byte[] rsaWithAesDecrypt(@NonNull byte[] dataToDecrypt, @NonNull PrivateKey privateKey) throws GeneralSecurityException
	{
		if (dataToDecrypt.length < 3)
			throw new InvalidParameterSpecException("Data to decrypt got invalid length: " + dataToDecrypt.length);
		
		ByteBuffer bb = ByteBuffer.wrap(dataToDecrypt);
		
		final int keyLength = bb.get() * 256 + bb.get();
		final int dataLength = dataToDecrypt.length - keyLength - 2;

		if (dataLength < 1)
			throw new InvalidParameterSpecException("Data to decrypt got invalid length, encrypted AES key length is [" + keyLength + "], but remaining bytes (including encrypted data) are: " + bb.remaining());
		
		byte[] rsaEncryptedAesKeyBytes = new byte[keyLength];
		bb.get(rsaEncryptedAesKeyBytes);
		
		SecretKey aesKey = deserializeAesKey(rsaDecrypt(rsaEncryptedAesKeyBytes, privateKey));
		
		byte[] aesEncryptedBytes = new byte[dataLength];
		bb.get(aesEncryptedBytes);
		
		return aesDecrypt(aesEncryptedBytes, aesKey);
	}
	
	/**
	 * Serializes provided AES secret key.
	 * 
	 * @throws IllegalStateException if key doesn't support serialization but
	 * 		 that shouldn't happen for AES keys
	 */
	public byte[] serializeAesKey(@NonNull SecretKey secretKey)
	{
		return nnChecked(secretKey.getEncoded());
	}
	
	/**
	 * Serializes provided RSA private key.
	 * 
	 * @throws IllegalStateException if key doesn't support serialization but
	 * 		 that shouldn't happen for RSA private keys
	 */
	public byte[] serializeRsaPrivateKey(@NonNull PrivateKey privateKey)
	{
		return nnChecked(privateKey.getEncoded());
	}
	
	/**
	 * Serializes provided RSA public key.
	 * 
	 * @throws IllegalStateException if key doesn't support serialization but
	 * 		 that shouldn't happen for RSA public keys
	 */
	public byte[] serializeRsaPublicKey(@NonNull PublicKey publicKey)
	{
		return nnChecked(publicKey.getEncoded());
	}
	
	/**
	 * De-serializes AES secret key from the provided bytes.
	 * <p>
	 * NB: this will NOT fail on invalid key bytes until you actually try
	 * to encrypt/decrypt with the resulting key.
	 * 
	 * @throws IllegalArgumentException if key bytes is an empty array
	 */
	public SecretKey deserializeAesKey(@NonNull byte[] aesKeyBytes) throws IllegalArgumentException
	{
		return new SecretKeySpec(aesKeyBytes , 0, aesKeyBytes.length, "AES");
	}
	
	/**
	 * De-serializes RSA private key from the provided bytes.
	 * 
	 * @throws InvalidKeySpecException if provided bytes are not a valid key 
	 */
	public PrivateKey deserializeRsaPrivateKey(@NonNull byte[] privateKeyBytes) throws InvalidKeySpecException
	{
		final KeyFactory keyFactory;
		try
		{
			keyFactory = KeyFactory.getInstance("RSA");
		} catch (NoSuchAlgorithmException e)
		{
			throw new AssertionException(e);
		}
		
		EncodedKeySpec privateKeySpec = new PKCS8EncodedKeySpec(privateKeyBytes);
		
		return keyFactory.generatePrivate(privateKeySpec);
	}
	
	/**
	 * De-serializes RSA public key from the provided bytes.
	 * 
	 * @throws InvalidKeySpecException if provided bytes are not a valid key 
	 */
	public PublicKey deserializeRsaPublicKey(@NonNull byte[] publicKeyBytes) throws InvalidKeySpecException
	{
		final KeyFactory keyFactory;
		try
		{
			keyFactory = KeyFactory.getInstance("RSA");
		} catch (NoSuchAlgorithmException e)
		{
			throw new AssertionException(e);
		}
		
		EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(publicKeyBytes);
		
		return keyFactory.generatePublic(publicKeySpec);
	}
}
