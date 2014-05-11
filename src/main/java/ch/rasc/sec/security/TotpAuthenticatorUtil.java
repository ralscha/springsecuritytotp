package ch.rasc.sec.security;

import java.nio.ByteBuffer;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.binary.Base32;

public class TotpAuthenticatorUtil {

	public static boolean verifyCode(String secret, int code, int variance) throws InvalidKeyException,
			NoSuchAlgorithmException {
		long timeIndex = System.currentTimeMillis() / 1000 / 30;
		byte[] secretBytes = new Base32().decode(secret);
		for (int i = -variance; i <= variance; i++) {
			if (getCode(secretBytes, timeIndex + i) == code) {
				return true;
			}
		}
		return false;
	}

	public static long getCode(byte[] secret, long timeIndex) throws NoSuchAlgorithmException, InvalidKeyException {
		SecretKeySpec signKey = new SecretKeySpec(secret, "HmacSHA1");
		ByteBuffer buffer = ByteBuffer.allocate(8);
		buffer.putLong(timeIndex);
		byte[] timeBytes = buffer.array();
		Mac mac = Mac.getInstance("HmacSHA1");
		mac.init(signKey);
		byte[] hash = mac.doFinal(timeBytes);
		int offset = hash[19] & 0xf;
		long truncatedHash = hash[offset] & 0x7f;
		for (int i = 1; i < 4; i++) {
			truncatedHash <<= 8;
			truncatedHash |= hash[offset + i] & 0xff;
		}
		return truncatedHash %= 1000000;
	}

}
