package ch.rasc.twofa.security;

public enum AuthenticationFlow {
  NOT_AUTHENTICATED, AUTHENTICATED, TOTP, TOTP_ADDITIONAL_SECURITY;
}
