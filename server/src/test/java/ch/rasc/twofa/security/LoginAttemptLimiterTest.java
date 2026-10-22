package ch.rasc.twofa.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

class LoginAttemptLimiterTest {

	@Test
	void blocksAttemptsUntilFixedWindowExpires() {
		Clock clock = mock(Clock.class);
		when(clock.millis()).thenReturn(0L);
		LoginAttemptLimiter limiter = new LoginAttemptLimiter(clock);
		limiter.check("account", 1);
		ResponseStatusException exception = assertThrows(ResponseStatusException.class,
				() -> limiter.check("account", 1));
		assertEquals(429, exception.getStatusCode().value());
		assertEquals("300", exception.getHeaders().getFirst("Retry-After"));
		limiter.check("different-account", 1);
		when(clock.millis()).thenReturn(Duration.ofMinutes(5).toMillis());
		limiter.check("account", 1);
	}

	@Test
	void concurrentAttemptsCannotExceedLimit() {
		LoginAttemptLimiter limiter = new LoginAttemptLimiter();
		AtomicInteger accepted = new AtomicInteger();
		var calls = IntStream.range(0, 30).mapToObj(i -> CompletableFuture.runAsync(() -> {
			try {
				limiter.check("account", 5);
				accepted.incrementAndGet();
			}
			catch (ResponseStatusException ex) {
				assertEquals(429, ex.getStatusCode().value());
			}
		})).toList();
		calls.forEach(CompletableFuture::join);
		assertEquals(5, accepted.get());
	}

}
