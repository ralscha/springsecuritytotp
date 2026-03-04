package ch.rasc.twofa.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.apache.commons.codec.binary.Base32;
import org.junit.jupiter.api.Test;

class CustomTotpTest {

	private static final String RFC_SECRET_BASE32 = "GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ";

	@Test
	void randomSecretIsBase32AndDecodesToTenBytes() {
		String secret = CustomTotp.randomSecret();

		assertTrue(secret.matches("^[A-Z2-7]+$"));
		byte[] decoded = new Base32().decode(secret);
		assertEquals(10, decoded.length);
	}

	@Test
	void generatesExpectedCodesFromRfcVectorsModuloSixDigits() {
		CustomTotp totp = new CustomTotp(RFC_SECRET_BASE32, () -> 0);

		assertEquals(287082, totp.generateAtInterval(59L / 30L));
		assertEquals(81804, totp.generateAtInterval(1111111109L / 30L));
		assertEquals(50471, totp.generateAtInterval(1111111111L / 30L));
		assertEquals(5924, totp.generateAtInterval(1234567890L / 30L));
		assertEquals(279037, totp.generateAtInterval(2000000000L / 30L));
		assertEquals(353130, totp.generateAtInterval(20000000000L / 30L));
	}

	@Test
	void verifySingleCodeInCurrentInterval() {
		long currentInterval = 1000;
		CustomTotp totp = new CustomTotp(RFC_SECRET_BASE32, () -> currentInterval);
		String code = String.valueOf(totp.generateAtInterval(currentInterval));

		CustomTotp.Result result = totp.verify(code, 2, 2);

		assertTrue(result.isValid());
		assertEquals(0, result.getShift());
	}

	@Test
	void verifySingleCodeInPastInterval() {
		long currentInterval = 1000;
		CustomTotp totp = new CustomTotp(RFC_SECRET_BASE32, () -> currentInterval);
		String code = String.valueOf(totp.generateAtInterval(currentInterval - 1));

		CustomTotp.Result result = totp.verify(code, 2, 0);

		assertTrue(result.isValid());
		assertEquals(-1, result.getShift());
	}

	@Test
	void verifySingleCodeInFutureInterval() {
		long currentInterval = 1000;
		CustomTotp totp = new CustomTotp(RFC_SECRET_BASE32, () -> currentInterval);
		String code = String.valueOf(totp.generateAtInterval(currentInterval + 2));

		CustomTotp.Result result = totp.verify(code, 0, 2);

		assertTrue(result.isValid());
		assertEquals(2, result.getShift());
	}

	@Test
	void rejectSingleCodeOutsideWindow() {
		long currentInterval = 1000;
		CustomTotp totp = new CustomTotp(RFC_SECRET_BASE32, () -> currentInterval);
		String code = String.valueOf(totp.generateAtInterval(currentInterval - 3));

		CustomTotp.Result result = totp.verify(code, 2, 0);

		assertFalse(result.isValid());
	}

	@Test
	void verifyConsecutiveCodesList() {
		long currentInterval = 1500;
		CustomTotp totp = new CustomTotp(RFC_SECRET_BASE32, () -> currentInterval);

		List<String> codes = List.of(String.valueOf(totp.generateAtInterval(currentInterval - 2)),
				String.valueOf(totp.generateAtInterval(currentInterval - 1)),
				String.valueOf(totp.generateAtInterval(currentInterval)));

		CustomTotp.Result result = totp.verify(codes, 5, 5);

		assertTrue(result.isValid());
		assertEquals(-2, result.getShift());
	}

	@Test
	void rejectNonConsecutiveCodesList() {
		long currentInterval = 1500;
		CustomTotp totp = new CustomTotp(RFC_SECRET_BASE32, () -> currentInterval);

		List<String> codes = List.of(String.valueOf(totp.generateAtInterval(currentInterval - 2)),
				String.valueOf(totp.generateAtInterval(currentInterval)),
				String.valueOf(totp.generateAtInterval(currentInterval + 1)));

		CustomTotp.Result result = totp.verify(codes, 5, 5);

		assertFalse(result.isValid());
	}

	@Test
	void invalidNumericCodeThrowsNumberFormatException() {
		CustomTotp totp = new CustomTotp(RFC_SECRET_BASE32, () -> 1000L);

		assertThrows(NumberFormatException.class, () -> totp.verify("ABCDEF", 2, 2));
	}

}
