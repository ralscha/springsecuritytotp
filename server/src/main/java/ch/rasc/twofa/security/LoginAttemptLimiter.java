package ch.rasc.twofa.security;

import java.time.Clock;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

/**
 * A bounded, single-instance limiter. Use a shared store when running multiple instances.
 */
@Component
public class LoginAttemptLimiter {

	private static final long WINDOW_MILLIS = Duration.ofMinutes(5).toMillis();

	private static final int MAX_BUCKETS = 10_000;

	private final Map<String, Attempts> buckets = new HashMap<>();

	private final Clock clock;

	public LoginAttemptLimiter() {
		this(Clock.systemUTC());
	}

	LoginAttemptLimiter(Clock clock) {
		this.clock = clock;
	}

	public synchronized void check(String bucket, int limit) {
		long now = this.clock.millis();
		Attempts attempts = this.buckets.get(bucket);
		if (attempts == null || now >= attempts.expiresAt) {
			if (this.buckets.size() >= MAX_BUCKETS) {
				this.buckets.values().removeIf(value -> now >= value.expiresAt);
				if (this.buckets.size() >= MAX_BUCKETS) {
					throw throttled();
				}
			}
			attempts = new Attempts(now + WINDOW_MILLIS);
			this.buckets.put(bucket, attempts);
		}
		if (attempts.count >= limit) {
			throw throttled();
		}
		attempts.count++;
	}

	private static ResponseStatusException throttled() {
		return new RateLimitExceededException();
	}

	private static final class RateLimitExceededException extends ResponseStatusException {

		private static final long serialVersionUID = 1L;

		RateLimitExceededException() {
			super(HttpStatus.TOO_MANY_REQUESTS, "Too many attempts. Try again in five minutes.");
		}

		@Override
		public HttpHeaders getHeaders() {
			HttpHeaders headers = new HttpHeaders();
			headers.set("Retry-After", "300");
			return headers;
		}

	}

	private static final class Attempts {

		private final long expiresAt;

		private int count;

		Attempts(long expiresAt) {
			this.expiresAt = expiresAt;
		}

	}

}
