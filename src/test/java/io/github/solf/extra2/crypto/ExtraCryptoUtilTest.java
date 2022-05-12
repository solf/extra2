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

import static io.github.solf.extra2.testutil.AssertExtra.assertFails;
import static io.github.solf.extra2.testutil.AssertExtra.assertFailsWithSubstring;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.fail;

import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.util.Arrays;

import javax.crypto.SecretKey;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.testng.annotations.Test;

/**
 * Test for {@link CryptoUtil}
 *
 * @author Sergey Olefir
 */
@NonNullByDefault
public class ExtraCryptoUtilTest
{
	@Test
	public void test() throws GeneralSecurityException
	{
		final byte[] srcMsg = "This is a test encryption message.\r\nIt has multiple lines!\n".getBytes();
		
		testEncryptionWithSpecificMessage(srcMsg);
		testEncryptionWithSpecificMessage(new byte[0]); // verify that 0-sizes messages encrypt/decrypt properly
	}


	/**
	 * Tests that encryption/decryption works properly with specific source message.
	 */
	private void testEncryptionWithSpecificMessage(final byte[] srcMsg)
		throws GeneralSecurityException
	{
		final CryptoUtil cutil = new CryptoUtil();
		
		final KeyPair keyPair1 = cutil.generateRsaKeyPair();
		final KeyPair keyPair2 = cutil.generateRsaKeyPair();
		
		{
			// Try RSA-AES
			final byte[] encryptedMsg = cutil.rsaWithAesEncrypt(srcMsg, cutil.serializeRsaPublicKey(keyPair1.getPublic()));
			assertArraysNotEquals(encryptedMsg, srcMsg);
			
			final byte[] decryptedMsg = cutil.rsaWithAesDecrypt(encryptedMsg, cutil.serializeRsaPrivateKey(keyPair1.getPrivate()));
			assertArraysEquals(decryptedMsg, srcMsg);
			
			assertFails(() -> 
				cutil.rsaWithAesDecrypt(encryptedMsg, cutil.serializeRsaPrivateKey(keyPair2.getPrivate())));
			
			// encrypt second time, should not match 1st time
			{
				byte[] reencrypted = cutil.rsaWithAesEncrypt(srcMsg, keyPair1.getPublic());
				assertArraysNotEquals(reencrypted, encryptedMsg);
				assertArraysEquals(cutil.rsaWithAesDecrypt(reencrypted, keyPair1.getPrivate()), srcMsg);
			}
		}
		
		{
			// Try AES
			final String passphrase = "a password";
			SecretKey noSaltKey = cutil.aesGenerateKeyFromPassphrase(passphrase, null);
			
			byte[] noSaltEncrypted = cutil.aesEncrypt(srcMsg, cutil.serializeAesKey(noSaltKey));
			assertArraysNotEquals(noSaltEncrypted, srcMsg);
			
			final byte[] noSaltDecryptedMsg = cutil.aesDecrypt(noSaltEncrypted, cutil.serializeAesKey(noSaltKey));
			assertArraysEquals(noSaltDecryptedMsg, srcMsg);
			
			assertFails(() -> 
				cutil.aesDecrypt(noSaltEncrypted, cutil.aesGenerateKeyFromPassphrase("bad", null)));
			
			// check re-encryption gets different result (due to IV) & that key re-generation works
			{
				byte[] reencrypted = cutil.aesEncrypt(srcMsg, 
					cutil.aesGenerateKeyFromPassphrase(passphrase, null));
				assertArraysNotEquals(reencrypted, noSaltEncrypted);
				assertArraysEquals(cutil.aesDecrypt(reencrypted, noSaltKey), srcMsg);
			}
			
			
			// Check with salt.
			final byte[] salt = new byte[] {4, 5, 6};
			SecretKey saltKey = cutil.aesGenerateKeyFromPassphrase(passphrase, salt);
			assertArraysNotEquals(cutil.serializeAesKey(saltKey), cutil.serializeAesKey(noSaltKey));
			
			byte[] saltEncrypted = cutil.aesEncrypt(srcMsg, cutil.serializeAesKey(saltKey));
			assertArraysNotEquals(saltEncrypted, srcMsg);
			
			final byte[] saltDecryptedMsg = cutil.aesDecrypt(saltEncrypted, cutil.serializeAesKey(saltKey));
			assertArraysEquals(saltDecryptedMsg, srcMsg);
			
			assertFails(() -> 
				cutil.aesDecrypt(saltEncrypted, cutil.aesGenerateKeyFromPassphrase("bad", null)));
			
			// check re-encryption gets different result (due to IV) & that key re-generation works
			{
				byte[] reencrypted = cutil.aesEncrypt(srcMsg, 
					cutil.aesGenerateKeyFromPassphrase(passphrase, salt));
				assertArraysNotEquals(reencrypted, saltEncrypted);
				assertArraysEquals(cutil.aesDecrypt(reencrypted, saltKey), srcMsg);
			}
		}
		
		{
			// try pure RSA
			for (String srcString : Arrays.asList("12345", "")) // test with empty array/message too
			{
				final byte[] rsaMsg = srcString.getBytes();
				
				@SuppressWarnings("deprecation") byte[] enc1 = cutil.rsaEncrypt(rsaMsg, cutil.serializeRsaPublicKey(keyPair1.getPublic()));
				@SuppressWarnings("deprecation") byte[] enc2 = cutil.rsaEncrypt(rsaMsg, cutil.serializeRsaPublicKey(keyPair1.getPublic()));
				assertArraysNotEquals(enc1, rsaMsg);
				assertArraysNotEquals(enc2, rsaMsg);
				assertArraysNotEquals(enc1, enc2);
				
				byte[] dec1 = cutil.rsaDecrypt(enc1, cutil.serializeRsaPrivateKey(keyPair1.getPrivate()));
				byte[] dec2 = cutil.rsaDecrypt(enc2, cutil.serializeRsaPrivateKey(keyPair1.getPrivate()));
				assertArraysEquals(dec1, rsaMsg);
				assertArraysEquals(dec2, rsaMsg);
			}
		}
	}
	

	/**
	 * Tests some failures.
	 * @throws GeneralSecurityException 
	 */
	@Test
	public void testSomeFailures() throws GeneralSecurityException
	{
		final CryptoUtil cutil = new CryptoUtil();
		
		final byte[] emptyArray = new byte[] {};
		final byte[] array123 = new byte[] {1, 2, 3};
		
		{
			// AES key deserialization
			assertFailsWithSubstring(
				() -> cutil.deserializeAesKey(emptyArray), 
				"java.lang.IllegalArgumentException: Empty key");
			assertFailsWithSubstring(
				() -> cutil.aesEncrypt(array123, cutil.deserializeAesKey(array123)), 
				"Invalid AES key length");
		}
		
		{
			// RSA private key deserialization
			assertFailsWithSubstring(
				() -> cutil.deserializeRsaPrivateKey(emptyArray), 
				"java.security.InvalidKeyException");
			assertFailsWithSubstring(
				() -> cutil.deserializeRsaPrivateKey(array123), 
				"java.security.InvalidKeyException");
		}
		
		{
			// RSA public key deserialization
			assertFailsWithSubstring(
				() -> cutil.deserializeRsaPublicKey(emptyArray), 
				"java.security.InvalidKeyException");
			assertFailsWithSubstring(
				() -> cutil.deserializeRsaPublicKey(array123), 
				"java.security.InvalidKeyException");
		}
		
		{
			// Invalid iteration count for AES key generation from passphrase.
			assertFailsWithSubstring(
				() -> cutil.aesGenerateKeyFromPassphrase("123", null, 299, 128),
				"Iteration count must be at least 300");
			
			cutil.aesGenerateKeyFromPassphrase("123", null, 300, 128); // 300 should be fine
		}
		
		{
			// Invalid AES decryption.
			SecretKey key = cutil.aesGenerateKeyFromPassphrase("123", null);
			assertFailsWithSubstring(
				() -> cutil.aesDecrypt(emptyArray, key),
				"java.security.spec.InvalidParameterSpecException: Data to decrypt got invalid length");
			assertFailsWithSubstring(
				() -> cutil.aesDecrypt(array123, key),
				"java.security.spec.InvalidParameterSpecException: Data to decrypt got invalid length");
		}
		
		{
			// Invalid RSA-with-AES decryption.
			PrivateKey key = cutil.generateRsaKeyPair().getPrivate();
			assertFailsWithSubstring(
				() -> cutil.rsaWithAesDecrypt(emptyArray, key),
				"java.security.spec.InvalidParameterSpecException: Data to decrypt got invalid length");
			assertFailsWithSubstring(
				() -> cutil.rsaWithAesDecrypt(array123, key),
				"java.security.spec.InvalidParameterSpecException: Data to decrypt got invalid length");
		}
	}

	/**
	 * Asserts that two arrays are not equals.
	 */
	private static void assertArraysNotEquals(byte[] actual, byte[] expected)
	{
		try
		{
			assertEquals(actual, expected);
		} catch (AssertionError e)
		{
			// Expected
			return;
		}
		
		fail("Arrays were equal: " + Arrays.toString(actual));
	}

	/**
	 * Asserts that two arrays are equals.
	 */
	private static void assertArraysEquals(byte[] actual, byte[] expected)
	{
		assertEquals(actual, expected);
	}
}
