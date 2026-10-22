package ch.rasc.twofa.security;

import static ch.rasc.twofa.db.tables.AppUser.APP_USER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.HttpCookie;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import com.jayway.jsonpath.JsonPath;

import org.jooq.DSLContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
		properties = { "spring.datasource.url=jdbc:h2:mem:security-integration-${random.uuid};DB_CLOSE_DELAY=-1",
				"spring.datasource.username=sa", "spring.datasource.password=", "debug=false",
				"logging.level.root=INFO", "logging.level.org.jooq.tools.LoggerListener=OFF" })
class SecurityIntegrationTest {

	private static final String PASSWORD = "integration-password-F9!b7O-x";

	private static final String SECRET = "JBSWY3DPEHPK3PXP";

	@Value("${local.server.port}")
	private int port;

	@Autowired
	private DSLContext dsl;

	@Autowired
	private PasswordEncoder passwordEncoder;

	private CookieManager cookies;

	private HttpClient client;

	@BeforeEach
	void setUp() {
		this.cookies = new CookieManager(null, CookiePolicy.ACCEPT_ALL);
		this.client = HttpClient.newBuilder()
			.cookieHandler(this.cookies)
			.connectTimeout(Duration.ofSeconds(10))
			.build();
	}

	@AfterEach
	void tearDown() {
		this.client.close();
	}

	@Test
	void unauthenticatedRequestsCannotAccessProtectedEndpoints() throws Exception {
		assertEquals(401, get("/totp-shift").statusCode());
		assertEquals("\"NOT_AUTHENTICATED\"", get("/authenticate").body());
	}

	@Test
	void defaultDatabaseMigrationDoesNotCreateKnownDemoAccounts() {
		assertEquals(0, this.dsl.fetchCount(APP_USER, APP_USER.USERNAME.in("admin", "user", "lazy")));
	}

	@Test
	void everyAuthenticationMutationRequiresCsrf() throws Exception {
		for (String path : List.of("/signin", "/signup", "/verify-totp", "/verify-totp-additional-security",
				"/signup-confirm-secret", "/logout")) {
			assertEquals(403, post(path, Map.of(), false).statusCode(), path);
		}
	}

	@Test
	void csrfCookieAndMatchingHeaderAreRequiredTogether() throws Exception {
		assertEquals(200, get("/csrf").statusCode());
		String token = cookie("XSRF-TOKEN");
		assertFalse(token.isBlank());
		Map<String, String> credentials = Map.of("username", "missing-" + UUID.randomUUID(), "password", PASSWORD);

		assertEquals(403, post("/signin", credentials, false).statusCode());
		HttpResponse<String> mismatchedToken = this.client
			.send(request("/signin").header("Content-Type", "application/x-www-form-urlencoded")
				.header("X-XSRF-TOKEN", "incorrect-token")
				.POST(HttpRequest.BodyPublishers.ofString(form(credentials)))
				.build(), HttpResponse.BodyHandlers.ofString());
		assertEquals(403, mismatchedToken.statusCode());

		HttpResponse<String> accepted = post("/signin", credentials, true);
		assertEquals(200, accepted.statusCode());
		assertEquals("\"NOT_AUTHENTICATED\"", accepted.body());
	}

	@Test
	void malformedAuthenticationParametersReturnBadRequest() throws Exception {
		get("/csrf");
		assertEquals(400, post("/verify-totp", Map.of("code", "not-a-code"), true).statusCode());
		assertEquals(400, post("/verify-totp-additional-security",
				Map.of("code1", "123456", "code2", "", "code3", "654321"), true)
			.statusCode());
		assertEquals(400, post("/signup-confirm-secret", Map.of("code", "12345"), true).statusCode());
		assertEquals(400, post("/signin", Map.of("username", " ", "password", PASSWORD), true).statusCode());
		assertEquals(400,
				post("/signup", Map.of("username", "oversized", "password", "a".repeat(257), "totp", "false"), true)
					.statusCode());
	}

	@Test
	void authenticationResponsesPreventCachingAndApplySecurityHeaders() throws Exception {
		HttpResponse<String> response = get("/authenticate");

		assertEquals(200, response.statusCode());
		assertTrue(response.headers().firstValue("Cache-Control").orElseThrow().contains("no-store"));
		assertEquals("nosniff", response.headers().firstValue("X-Content-Type-Options").orElseThrow());
		assertEquals("DENY", response.headers().firstValue("X-Frame-Options").orElseThrow());
	}

	@Test
	void loginRotatesSessionAndLogoutRevokesAuthentication() throws Exception {
		String username = insertUser(null);
		get("/signup-pending");
		String originalSessionId = cookie("JSESSIONID");
		get("/csrf");
		String originalToken = cookie("XSRF-TOKEN");

		HttpResponse<String> login = post("/signin", Map.of("username", username, "password", PASSWORD), true);
		assertEquals(200, login.statusCode());
		assertEquals("\"AUTHENTICATED\"", login.body());
		assertNotEquals(originalSessionId, cookie("JSESSIONID"));
		assertTrue(login.headers()
			.allValues("Set-Cookie")
			.stream()
			.anyMatch(value -> value.startsWith("JSESSIONID=") && value.contains("HttpOnly")
					&& value.contains("SameSite=Strict")));
		assertEquals(200, get("/totp-shift").statusCode());
		assertEquals("\"AUTHENTICATED\"", get("/authenticate").body());
		assertNotEquals(originalToken, cookie("XSRF-TOKEN"));

		try (HttpClient oldSession = HttpClient.newHttpClient()) {
			HttpResponse<String> response = oldSession.send(
					request("/totp-shift").header("Cookie", "JSESSIONID=" + originalSessionId).GET().build(),
					HttpResponse.BodyHandlers.ofString());
			assertEquals(401, response.statusCode());
		}

		assertEquals(403, post("/logout", Map.of(), false).statusCode());
		assertEquals(200, get("/totp-shift").statusCode());
		assertEquals(200, post("/logout", Map.of(), true).statusCode());
		assertEquals(401, get("/totp-shift").statusCode());
		assertEquals("\"NOT_AUTHENTICATED\"", get("/authenticate").body());
	}

	@Test
	void switchingAccountsRevokesOldAuthenticationUntilTotpCompletes() throws Exception {
		String passwordUser = insertUser(null);
		String totpUser = insertUser(SECRET);
		get("/csrf");
		assertEquals("\"AUTHENTICATED\"",
				post("/signin", Map.of("username", passwordUser, "password", PASSWORD), true).body());
		get("/csrf");

		assertEquals("\"TOTP\"", post("/signin", Map.of("username", totpUser, "password", PASSWORD), true).body());
		String pendingSessionId = cookie("JSESSIONID");
		assertEquals(401, get("/totp-shift").statusCode());
		assertEquals("\"TOTP\"", get("/authenticate").body());

		long interval = System.currentTimeMillis() / 30_000;
		String code = String.format(Locale.ROOT, "%06d", new CustomTotp(SECRET).generateAtInterval(interval));
		HttpResponse<String> verification = post("/verify-totp", Map.of("code", code), true);
		assertEquals(200, verification.statusCode());
		assertEquals("\"AUTHENTICATED\"", verification.body());
		assertNotEquals(pendingSessionId, cookie("JSESSIONID"));
		assertEquals(200, get("/totp-shift").statusCode());
		assertEquals("\"AUTHENTICATED\"", get("/authenticate").body());
	}

	@Test
	void failedAccountSwitchDoesNotRetainAnAuthenticatedSession() throws Exception {
		String username = insertUser(null);
		get("/csrf");
		assertEquals("\"AUTHENTICATED\"",
				post("/signin", Map.of("username", username, "password", PASSWORD), true).body());
		get("/csrf");

		assertEquals("\"NOT_AUTHENTICATED\"",
				post("/signin", Map.of("username", username, "password", "wrong-password"), true).body());
		assertEquals(401, get("/totp-shift").statusCode());
		assertEquals("\"NOT_AUTHENTICATED\"", get("/authenticate").body());
	}

	@Test
	void concurrentTotpReplayAuthenticatesOnlyOneBrowserSession() throws Exception {
		String username = insertUser(SECRET);
		CookieManager secondCookies = new CookieManager(null, CookiePolicy.ACCEPT_ALL);
		try (HttpClient secondBrowser = HttpClient.newBuilder().cookieHandler(secondCookies).build()) {
			get("/csrf");
			get(secondBrowser, "/csrf");
			Map<String, String> credentials = Map.of("username", username, "password", PASSWORD);
			assertEquals("\"TOTP\"", post("/signin", credentials, true).body());
			assertEquals("\"TOTP\"", post(secondBrowser, secondCookies, "/signin", credentials, true).body());
			assertNotEquals(cookie("JSESSIONID"), cookie(secondCookies, "JSESSIONID"));

			long interval = System.currentTimeMillis() / 30_000;
			String code = String.format(Locale.ROOT, "%06d", new CustomTotp(SECRET).generateAtInterval(interval));
			var firstVerification = this.client.sendAsync(
					postRequest(this.cookies, "/verify-totp", Map.of("code", code), true),
					HttpResponse.BodyHandlers.ofString());
			var secondVerification = secondBrowser.sendAsync(
					postRequest(secondCookies, "/verify-totp", Map.of("code", code), true),
					HttpResponse.BodyHandlers.ofString());
			HttpResponse<String> firstResponse = firstVerification.join();
			HttpResponse<String> secondResponse = secondVerification.join();

			assertEquals(200, firstResponse.statusCode());
			assertEquals(200, secondResponse.statusCode());
			assertEquals(1L,
					List.of(firstResponse, secondResponse)
						.stream()
						.filter(response -> response.body().equals("\"AUTHENTICATED\""))
						.count());
			assertEquals(1L,
					List.of(firstResponse, secondResponse)
						.stream()
						.filter(response -> response.body().equals("\"TOTP\""))
						.count());
			assertEquals(firstResponse.body().equals("\"AUTHENTICATED\"") ? 200 : 401, get("/totp-shift").statusCode());
			assertEquals(secondResponse.body().equals("\"AUTHENTICATED\"") ? 200 : 401,
					get(secondBrowser, "/totp-shift").statusCode());
		}
	}

	@Test
	void signupKeepsAccountDisabledUntilSecretIsConfirmedAndThenRemovesPendingSecret() throws Exception {
		String username = "signup-" + UUID.randomUUID();
		get("/csrf");
		HttpResponse<String> signup = post("/signup",
				Map.of("username", username, "password", PASSWORD, "totp", "true"), true);
		assertEquals(200, signup.statusCode());
		assertEquals("OK", JsonPath.read(signup.body(), "$.status"));
		String secret = JsonPath.read(signup.body(), "$.secret");
		assertTrue(secret.matches("[A-Z2-7]{32}"));

		var pendingUser = this.dsl.selectFrom(APP_USER).where(APP_USER.USERNAME.eq(username)).fetchSingle();
		assertFalse(pendingUser.getEnabled());
		assertEquals(secret, pendingUser.getSecret());
		HttpResponse<String> pending = get("/signup-pending");
		assertEquals(200, pending.statusCode());
		assertEquals(username, JsonPath.read(pending.body(), "$.username"));
		assertEquals(secret, JsonPath.read(pending.body(), "$.secret"));
		assertTrue(pending.headers().firstValue("Cache-Control").orElseThrow().contains("no-store"));
		try (HttpClient unrelatedBrowser = HttpClient.newHttpClient()) {
			assertEquals(204, get(unrelatedBrowser, "/signup-pending").statusCode());
		}

		long interval = System.currentTimeMillis() / 30_000;
		String code = String.format(Locale.ROOT, "%06d", new CustomTotp(secret).generateAtInterval(interval));
		HttpResponse<String> confirmation = post("/signup-confirm-secret", Map.of("code", code), true);
		assertEquals(200, confirmation.statusCode());
		assertEquals("true", confirmation.body());
		var confirmedUser = this.dsl
			.select(APP_USER.ENABLED, APP_USER.LAST_TOTP_INTERVAL, SignupController.ENROLLMENT_PENDING,
					SignupController.ENROLLMENT_EXPIRES_AT)
			.from(APP_USER)
			.where(APP_USER.ID.eq(pendingUser.getId()))
			.fetchSingle();
		assertTrue(confirmedUser.get(APP_USER.ENABLED));
		assertNotNull(confirmedUser.get(APP_USER.LAST_TOTP_INTERVAL));
		assertFalse(confirmedUser.get(SignupController.ENROLLMENT_PENDING));
		assertNull(confirmedUser.get(SignupController.ENROLLMENT_EXPIRES_AT));
		assertEquals(204, get("/signup-pending").statusCode());
	}

	@Test
	void totpThrottlePersistsAcrossNewSessionsAndBothVerificationEndpoints() throws Exception {
		String username = insertUser(SECRET);
		this.dsl.update(APP_USER)
			.set(APP_USER.ADDITIONAL_SECURITY, true)
			.where(APP_USER.USERNAME.eq(username))
			.execute();

		for (int attempt = 0; attempt < 6; attempt++) {
			CookieManager browserCookies = new CookieManager(null, CookiePolicy.ACCEPT_ALL);
			try (HttpClient browser = HttpClient.newBuilder().cookieHandler(browserCookies).build()) {
				get(browser, "/csrf");
				assertEquals("\"TOTP_ADDITIONAL_SECURITY\"", post(browser, browserCookies, "/signin",
						Map.of("username", username, "password", PASSWORD), true)
					.body());
				if (attempt < 5) {
					HttpResponse<String> verification = post(browser, browserCookies, "/verify-totp",
							Map.of("code", "000000"), true);
					assertEquals(200, verification.statusCode());
					assertEquals("\"TOTP_ADDITIONAL_SECURITY\"", verification.body());
				}
				else {
					HttpResponse<String> throttled = post(browser, browserCookies, "/verify-totp-additional-security",
							Map.of("code1", "000000", "code2", "000000", "code3", "000000"), true);
					assertEquals(429, throttled.statusCode());
					assertEquals("300", throttled.headers().firstValue("Retry-After").orElseThrow());
				}
				assertEquals(401, get(browser, "/totp-shift").statusCode());
			}
		}
	}

	private String insertUser(String secret) {
		String username = "integration-" + UUID.randomUUID();
		this.dsl
			.insertInto(APP_USER, APP_USER.USERNAME, APP_USER.PASSWORD_HASH, APP_USER.SECRET, APP_USER.ENABLED,
					APP_USER.ADDITIONAL_SECURITY)
			.values(username, this.passwordEncoder.encode(PASSWORD), secret, true, false)
			.execute();
		return username;
	}

	private HttpResponse<String> get(String path) throws Exception {
		return get(this.client, path);
	}

	private HttpResponse<String> get(HttpClient browser, String path) throws Exception {
		return browser.send(request(path).GET().build(), HttpResponse.BodyHandlers.ofString());
	}

	private HttpResponse<String> post(String path, Map<String, String> values, boolean csrf) throws Exception {
		return post(this.client, this.cookies, path, values, csrf);
	}

	private HttpResponse<String> post(HttpClient browser, CookieManager browserCookies, String path,
			Map<String, String> values, boolean csrf) throws Exception {
		return browser.send(postRequest(browserCookies, path, values, csrf), HttpResponse.BodyHandlers.ofString());
	}

	private HttpRequest postRequest(CookieManager browserCookies, String path, Map<String, String> values,
			boolean csrf) {
		HttpRequest.Builder builder = request(path).header("Content-Type", "application/x-www-form-urlencoded");
		if (csrf) {
			builder.header("X-XSRF-TOKEN", cookie(browserCookies, "XSRF-TOKEN"));
		}
		return builder.POST(HttpRequest.BodyPublishers.ofString(form(values))).build();
	}

	private HttpRequest.Builder request(String path) {
		return HttpRequest.newBuilder(URI.create("http://localhost:" + this.port + path))
			.timeout(Duration.ofSeconds(10));
	}

	private String cookie(String name) {
		return cookie(this.cookies, name);
	}

	private static String cookie(CookieManager browserCookies, String name) {
		return browserCookies.getCookieStore()
			.getCookies()
			.stream()
			.filter(cookie -> cookie.getName().equals(name) && !cookie.hasExpired())
			.map(HttpCookie::getValue)
			.findFirst()
			.orElseThrow(() -> new AssertionError("Missing cookie " + name));
	}

	private static String form(Map<String, String> values) {
		return values.entrySet()
			.stream()
			.map(entry -> URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8) + "="
					+ URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8))
			.collect(Collectors.joining("&"));
	}

}
