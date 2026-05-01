package ch.rasc.twofa.security;

import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.LongSupplier;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.binary.Base32;

public class CustomTotp {

	private static final SecureRandom SECURE_RANDOM = new SecureRandom();

	private static final Base32 BASE32 = new Base32();

	private static final String HMAC_SHA1 = "HmacSHA1";

	private static final int TIME_STEP_SECONDS = 30;

	private static final int SIX_DIGITS = 1_000_000;

	class Result {

		private final boolean valid;

		private final long shift;

		public Result(boolean valid, long shift) {
			this.valid = valid;
			this.shift = shift;
		}

		public boolean isValid() {
			return this.valid;
		}

		public long getShift() {
			return this.shift;
		}

	}

	private final String secret;

	private final byte[] secretBytes;

	private final LongSupplier currentIntervalSupplier;

	private final ThreadLocal<Mac> macThreadLocal;

	public CustomTotp(String secret) {
		this(secret, () -> System.currentTimeMillis() / 1000 / TIME_STEP_SECONDS);
	}

	CustomTotp(String secret, LongSupplier currentIntervalSupplier) {
		this.secret = secret;
		this.secretBytes = BASE32.decode(secret.toUpperCase(Locale.ROOT));
		if (this.secretBytes.length == 0) {
			throw new IllegalArgumentException("Invalid TOTP secret");
		}
		this.currentIntervalSupplier = Objects.requireNonNull(currentIntervalSupplier);
		this.macThreadLocal = ThreadLocal.withInitial(this::createMac);
	}

	public static String randomSecret() {
		byte[] bytes = new byte[10];
		SECURE_RANDOM.nextBytes(bytes);
		return BASE32.encodeToString(bytes).replace("=", "");
	}

	public Result verify(String codeString, int pastIntervals, int futureIntervals) {

		boolean valid = false;
		long shift = 0;

		int code = Integer.parseInt(codeString);
		long currentInterval = this.currentIntervalSupplier.getAsLong();

		int expectedResponse = generateAtInterval(currentInterval);
		if (expectedResponse == code) {
			valid = true;
		}

		for (int i = 1; i <= pastIntervals; i++) {
			int pastResponse = generateAtInterval(currentInterval - i);
			if (pastResponse == code) {
				valid = true;
				shift = -i;
			}
		}
		for (int i = 1; i <= futureIntervals; i++) {
			int futureResponse = generateAtInterval(currentInterval + i);
			if (futureResponse == code) {
				valid = true;
				shift = i;
			}
		}

		return new Result(valid, shift);
	}

	public Result verify(List<String> codeStrings, long pastIntervals, long futureIntervals) {
		List<Integer> codes = codeStrings.stream().map(Integer::valueOf).toList();
		long shift = 0;

		long currentInterval = this.currentIntervalSupplier.getAsLong();

		int first = codes.get(0);
		for (long i = -pastIntervals; i <= futureIntervals; i++) {
			int generated = generateAtInterval(currentInterval + i);
			if (first == generated) {
				boolean codesOkay = true;
				shift = i;
				for (int j = 1; j < codes.size(); j++) {
					int next = generateAtInterval(currentInterval + i + j);
					if (next != codes.get(j)) {
						codesOkay = false;
						break;
					}
				}
				return new Result(codesOkay, shift);
			}
		}

		return new Result(false, shift);
	}

	int generateAtInterval(long interval) {
		return hash(interval);
	}

	private int hash(long interval) {
		Mac mac = this.macThreadLocal.get();
		byte[] intervalBytes = ByteBuffer.allocate(Long.BYTES).putLong(interval).array();
		byte[] hash = mac.doFinal(intervalBytes);
		int offset = hash[hash.length - 1] & 0x0f;
		int binary = (hash[offset] & 0x7f) << 24 | (hash[offset + 1] & 0xff) << 16 | (hash[offset + 2] & 0xff) << 8
				| hash[offset + 3] & 0xff;
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