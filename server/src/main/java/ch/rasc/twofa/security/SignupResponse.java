package ch.rasc.twofa.security;

public record SignupResponse(Status status, String username, String secret) {

	enum Status {

		OK, USERNAME_TAKEN, WEAK_PASSWORD

	}

	public SignupResponse(Status status) {
		this(status, null, null);
	}

}
