package ch.rasc.twofa.security;

import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.LongSupplier;
import java.util.regex.Pattern;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.binary.Base32;

public class CustomTotp {

	private static final SecureRandom SECURE_RANDOM = new SecureRandom();

	private static final Base32 BASE32 = new Base32();

	private static final String HMAC_SHA1 = "HmacSHA1";

	private static final int TIME_STEP_SECONDS = 30;

	private static final int SIX_DIGITS = 1_000_000;

	private static final Pattern TOTP_CODE = Pattern.compile("\\d{6}");

	private static final Pattern BASE32_SECRET = Pattern.compile("[A-Z2-7]+");

	static class Result {

		private final boolean valid;

		private final long shift;

		private final long matchedInterval;

		private Result(boolean valid, long shift, long matchedInterval) {
			this.valid = valid;
			this.shift = shift;
			this.matchedInterval = matchedInterval;
		}

		public boolean isValid() {
			return this.valid;
		}

		public long getShift() {
			return this.shift;
		}

		public long getMatchedInterval() {
			if (!this.valid) {
				throw new IllegalStateException("An invalid TOTP result has no matched interval");
			}
			return this.matchedInterval;
		}

	}

	private final byte[] secretBytes;

	private final LongSupplier currentIntervalSupplier;

	private final Mac mac;

	public CustomTotp(String secret) {
		this(secret, () -> System.currentTimeMillis() / 1000 / TIME_STEP_SECONDS);
	}

	CustomTotp(String secret, LongSupplier currentIntervalSupplier) {
		String normalizedSecret = Objects.requireNonNull(secret, "secret").toUpperCase(Locale.ROOT);
		if (!BASE32_SECRET.matcher(normalizedSecret).matches()) {
			throw new IllegalArgumentException("Invalid TOTP secret");
		}
		this.secretBytes = BASE32.decode(normalizedSecret);
		if (this.secretBytes.length == 0) {
			throw new IllegalArgumentException("Invalid TOTP secret");
		}
		this.currentIntervalSupplier = Objects.requireNonNull(currentIntervalSupplier);
		this.mac = createMac();
	}

	public static String randomSecret() {
		byte[] bytes = new byte[20];
		SECURE_RANDOM.nextBytes(bytes);
		return BASE32.encodeToString(bytes).replace("=", "");
	}

	public Result verify(String codeString, int pastIntervals, int futureIntervals) {

		if (!isValidCode(codeString) || pastIntervals < 0 || futureIntervals < 0) {
			return invalidResult();
		}

		int code = Integer.parseInt(codeString);
		long currentInterval = this.currentIntervalSupplier.getAsLong();

		int expectedResponse = generateAtInterval(currentInterval);
		if (expectedResponse == code) {
			return new Result(true, 0, currentInterval);
		}

		for (int i = 1; i <= pastIntervals; i++) {
			int pastResponse = generateAtInterval(currentInterval - i);
			if (pastResponse == code) {
				return new Result(true, -i, currentInterval - i);
			}
		}
		for (int i = 1; i <= futureIntervals; i++) {
			int futureResponse = generateAtInterval(currentInterval + i);
			if (futureResponse == code) {
				return new Result(true, i, currentInterval + i);
			}
		}

		return invalidResult();
	}

	public Result verify(List<String> codeStrings, long pastIntervals, long futureIntervals) {
		if (codeStrings == null || codeStrings.isEmpty() || pastIntervals < 0 || futureIntervals < 0
				|| codeStrings.stream().anyMatch(code -> !isValidCode(code))) {
			return invalidResult();
		}

		List<Integer> codes = codeStrings.stream().map(Integer::valueOf).toList();
		long currentInterval = this.currentIntervalSupplier.getAsLong();

		int first = codes.get(0);
		for (long i = -pastIntervals; i <= futureIntervals; i++) {
			int generated = generateAtInterval(currentInterval + i);
			if (first == generated) {
				boolean codesOkay = true;
				for (int j = 1; j < codes.size(); j++) {
					int next = generateAtInterval(currentInterval + i + j);
					if (next != codes.get(j)) {
						codesOkay = false;
						break;
					}
				}
				if (codesOkay) {
					return new Result(true, i, currentInterval + i);
				}
			}
		}

		return invalidResult();
	}

	private static Result invalidResult() {
		return new Result(false, 0, 0);
	}

	private static boolean isValidCode(String code) {
		return code != null && TOTP_CODE.matcher(code).matches();
	}

	int generateAtInterval(long interval) {
		return hash(interval);
	}

	private int hash(long interval) {
		byte[] intervalBytes = ByteBuffer.allocate(Long.BYTES).putLong(interval).array();
		byte[] hash = this.mac.doFinal(intervalBytes);
		int offset = hash[hash.length - 1] & 0x0f;
		int binary = (hash[offset] & 0x7f) << 24 | (hash[offset + 1] & 0xff) << 16 | (hash[offset + 2] & 0xff) << 8
				| (hash[offset + 3] & 0xff);
		return binary % SIX_DIGITS;
	}

	private Mac createMac() {
		try {
			Mac mac = Mac.getInstance(HMAC_SHA1);
			mac.init(new SecretKeySpec(this.secretBytes, HMAC_SHA1));
			return mac;
		}
		catch (GeneralSecurityException e) {
			throw new IllegalStateException("Could not initialize TOTP MAC", e);
		}
	}

}
