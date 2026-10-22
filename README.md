Spring Security TOTP Demo
=========================

This project demonstrates form login with optional two-factor authentication
using Spring Security, Angular, and time-based one-time passwords (TOTP).

Original blog post:
https://blog.rasc.ch/2019/06/totp-spring-security.html

Project layout
--------------

- `server`: Spring Boot 4 application with Spring Security, jOOQ, Flyway, H2,
  and Argon2 password hashing.
- `client`: Angular 22 application using Angular template-driven forms and
  generated QR-code data URLs.

Requirements
------------

- Java 25
- Node.js 24 or newer
- npm

Run in development
------------------

Start the backend:

```sh
cd server
./mvnw spring-boot:run
```

Start the Angular dev server:

```sh
cd client
npm ci
npm start
```

Open `http://localhost:4200`. The Angular dev server proxies API calls to the
backend on `http://localhost:8080`.

Demo users
----------

New databases start without demo users. Register your own account through the
signup page. For a disposable local demonstration only, set
`APP_DEMO_USERS_ENABLED=true` **before the first start with a new database** to
seed these public credentials:

| Username | Password | TOTP |
| --- | --- | --- |
| `admin` | `admin` | Enabled, secret `W4AU5VIXXCPZ3S6T` |
| `user` | `user` | Enabled, secret `LRVLAZ4WVFOU3JBF` |
| `lazy` | `lazy` | Disabled |

Security controls
--------------

- New passwords use Argon2id with 19 MiB memory, two iterations, and parallelism 1.
  Existing hashes remain verifiable; the stronger parameters apply to new hashes.
- Signup passwords require 15–256 characters and pass the bundled breached
  password check. Existing passwords continue to work.
- Usernames are trimmed, normalized to lowercase for new accounts, and matched
  case-insensitively when signing in.
- Session-backed form login uses Spring Security CSRF protection. The Angular
  client obtains an XSRF cookie from `/csrf` and sends the token on mutating
  requests.
- The session ID rotates after a successful password check and again when MFA
  completes. Completing authentication also refreshes CSRF protection. Starting
  another login clears the previous authenticated context.
- Password checks, signup, and TOTP checks have bounded, per-process rate limits
  with HTTP 429 and `Retry-After`. Limits persist across browser sessions: 10
  password attempts per account, 30 sign-ins per client address, 10 signups per
  client address, and five TOTP attempts per account in each five-minute window.
  Successful requests also count, preventing rapid retries from resetting limits.
- Pending MFA expires five minutes after password verification and is invalidated
  when account credentials change. TOTP secrets are not stored in session principals.
- New TOTP secrets contain 160 bits of entropy, and successfully used time
  intervals advance atomically to prevent replay, including overlapping recovery
  sequences. Normal verification allows one adjacent interval on either side;
  the three-code clock-recovery flow is limited to five minutes on either side
  and requires the account to be in recovery mode. An accepted future interval
  cannot be reused after correcting the authenticator clock; wait for a newer code.
- Pending TOTP enrollment is bound to the browser session. Refreshing the QR
  page restores it; submitting the same username and password can also resume
  an interrupted enrollment within its fixed ten-minute lifetime. Enrollment has
  separate database state, so disabled accounts cannot expose their TOTP secret
  or reactivate through signup. Expired unfinished registrations can be replaced
  with a fresh registration and secret.
- Malformed TOTP input is rejected by request validation instead of surfacing
  as a server error.
- The H2 database is configured for local demo use in `server/src/main/resources/application.properties`.
- Session cookies are HttpOnly and SameSite=Strict; URL-based sessions are
  disabled. Responses include CSP, framing, referrer, and content-type protections.
- jOOQ query/result logging is disabled, and signup response log strings omit
  enrollment secrets.
- Embedded Tomcat is pinned to 11.0.27 and the Angular toolchain's Vite to 8.3.3
  to cover security fixes missing from their current parent dependency versions.
  Revisit these overrides when upgrading Spring Boot or Angular.

Deployment and remaining security work
--------------------------------------

Use HTTPS and activate the **Spring `prod` profile** (`SPRING_PROFILES_ACTIVE=prod`).
It requires `DB_URL`, `DB_USERNAME`, and `DB_PASSWORD` and enforces Secure cookies
for both sessions and CSRF. The Maven `production` profile only packages the
frontend; it does not activate the Spring profile. If TLS terminates at a proxy,
configure forwarded headers only from that trusted proxy, and block direct access
to the application port.

For existing installations, explicitly remove or disable the known demo accounts
and rotate their credentials/secrets if they were ever used. Changing the seed
setting does not alter existing database rows. Migration V0004 deliberately does
not mark legacy disabled accounts as pending enrollment; those unfinished demo
registrations need to be recreated after manual inspection. Existing session
state from older versions should be discarded on deployment.

Before exposing this demonstration to real users:

- Encrypt TOTP secrets at rest using a key held outside the database, with key
  rotation and restricted backup access. They are still plaintext in this demo DB.
- Move throttling to a shared store or trusted gateway for multiple application
  instances. The local limiter resets on restart; address limits use the actual
  remote address, so configure your proxy deliberately and consider shared NATs.
- Add account recovery with hashed, single-use recovery codes and audited factor
  changes. Consider WebAuthn/passkeys for phishing-resistant authentication and
  require MFA for privileged accounts.
- Add structured authentication audit events without logging passwords, OTPs,
  session identifiers, or enrollment secrets; monitor abuse and maintain dependency
  vulnerability scanning. Review CSP nonce support to remove inline-style permission.
- Replace the archived `passpol` library and its static password denylist with
  maintained breach-password checking. See the [upstream repository](https://github.com/codahale/passpol).

These choices follow [OWASP password storage guidance](https://cheatsheetseries.owasp.org/cheatsheets/Password_Storage_Cheat_Sheet.html),
[OWASP MFA guidance](https://cheatsheetseries.owasp.org/cheatsheets/Multifactor_Authentication_Cheat_Sheet.html),
and [Spring Security's session guidance](https://docs.spring.io/spring-security/reference/servlet/authentication/session-management.html).
Dependency patches are documented in the [Tomcat advisories](https://tomcat.apache.org/security-11.html)
and [Vite Windows command injection advisory](https://github.com/vitejs/launch-editor/security/advisories/GHSA-587h-7wmj-h5pm).

Build and verify
----------------

Backend tests:

```sh
cd server
./mvnw test
```

Frontend checks:

```sh
cd client
npm ci
npm run lint
npm run build
```

Production package:

```sh
cd server
./mvnw -Pproduction package
```

The production Maven profile runs `npm run build-prod` in `client` and copies
Angular output from `client/dist/app/browser` into the Spring Boot static
resources.
