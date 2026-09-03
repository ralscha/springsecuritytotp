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
	void randomSecretIsBase32AndHas160BitsOfEntropy() {
		String secret = CustomTotp.randomSecret();

		assertTrue(secret.matches("^[A-Z2-7]+$"));
		byte[] decoded = new Base32().decode(secret);
		assertEquals(20, decoded.length);
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
		assertEquals(currentInterval, result.getMatchedInterval());
	}

	@Test
	void verifySingleCodeInPastInterval() {
		long currentInterval = 1000;
		CustomTotp totp = new CustomTotp(RFC_SECRET_BASE32, () -> currentInterval);
		String code = String.valueOf(totp.generateAtInterval(currentInterval - 1));

		CustomTotp.Result result = totp.verify(code, 2, 0);

		assertTrue(result.isValid());
		assertEquals(-1, result.getShift());
		assertEquals(currentInterval - 1, result.getMatchedInterval());
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
		assertThrows(IllegalStateException.class, result::getMatchedInterval);
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
	void rejectInvalidSingleCode() {
		CustomTotp totp = new CustomTotp(RFC_SECRET_BASE32, () -> 1000L);

		assertFalse(totp.verify("ABCDEF", 2, 2).isValid());
		assertFalse(totp.verify((String) null, 2, 2).isValid());
		assertFalse(totp.verify("12345", 2, 2).isValid());
	}

	@Test
	void rejectInvalidCodesList() {
		CustomTotp totp = new CustomTotp(RFC_SECRET_BASE32, () -> 1000L);

		assertFalse(totp.verify(List.of("123456", "ABCDEF", "234567"), 2, 2).isValid());
		assertFalse(totp.verify(List.of(), 2, 2).isValid());
		assertFalse(totp.verify((List<String>) null, 2, 2).isValid());
	}

	@Test
	void rejectsInvalidSecretAndWindows() {
		assertThrows(IllegalArgumentException.class, () -> new CustomTotp("not base32!"));

		CustomTotp totp = new CustomTotp(RFC_SECRET_BASE32, () -> 1000L);
		assertFalse(totp.verify("123456", -1, 1).isValid());
		assertFalse(totp.verify(List.of("123456"), 1, -1).isValid());
	}

	@Test
	void keepsSearchingWhenFirstCodeCollides() {
		CustomTotp totp = new CustomTotp(RFC_SECRET_BASE32, () -> 1000L) {
			@Override
			int generateAtInterval(long interval) {
				return switch ((int) (interval - 1000)) {
					case -2, 1 -> 111111;
					case -1 -> 999999;
					case 2 -> 222222;
					case 3 -> 333333;
					default -> 888888;
				};
			}
		};

		CustomTotp.Result result = totp.verify(List.of("111111", "222222", "333333"), 2, 3);

		assertTrue(result.isValid());
		assertEquals(1, result.getShift());
	}

}
