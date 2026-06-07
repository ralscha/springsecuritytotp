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
- `client`: Angular 21 application using PrimeNG and `angularx-qrcode`.

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
npm install
npm start
```

Open `http://localhost:4200`. The Angular dev server proxies API calls to the
backend on `http://localhost:8080`.

Demo users
----------

The Flyway migration creates three users:

| Username | Password | TOTP |
| --- | --- | --- |
| `admin` | `admin` | Enabled, QR code shown on the sign-in page |
| `user` | `user` | Enabled, QR code shown on the sign-in page |
| `lazy` | `lazy` | Disabled |

Security notes
--------------

- Passwords are stored with Argon2.
- Signup passwords are checked against the bundled password policy.
- Session-backed form login uses Spring Security CSRF protection. The Angular
  client obtains an XSRF cookie from `/csrf` and sends the token on mutating
  requests.
- Invalid TOTP input is rejected as a failed verification instead of surfacing
  as a server error.
- The H2 database is configured for local demo use in `server/src/main/resources/application.properties`.

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
