package ch.rasc.twofa.security;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.stream.Collectors;

import org.jboss.aerogear.security.otp.api.Base32;
import org.jboss.aerogear.security.otp.api.Digits;
import org.jboss.aerogear.security.otp.api.Hash;
import org.jboss.aerogear.security.otp.api.Hmac;

public class CustomTotp {

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

  public CustomTotp(String secret) {
    this.secret = secret;
  }

  public Result verify(String codeString, int pastIntervals, int futureIntervals) {

    boolean valid = false;
    long shift = 0;

    int code = Integer.parseInt(codeString);
    long currentInterval = System.currentTimeMillis() / 1000 / 30;

    int expectedResponse = generate(currentInterval);
    if (expectedResponse == code) {
      valid = true;
    }

    for (int i = 1; i <= pastIntervals; i++) {
      int pastResponse = generate(currentInterval - i);
      if (pastResponse == code) {
        valid = true;
        shift = -i;
      }
    }
    for (int i = 1; i <= futureIntervals; i++) {
      int futureResponse = generate(currentInterval + i);
      if (futureResponse == code) {
        valid = true;
        shift = i;
      }
    }

    return new Result(valid, shift);
  }

  public Result verify(List<String> codeStrings, long pastIntervals,
      long futureIntervals) {
    List<Integer> codes = codeStrings.stream().map(Integer::valueOf)
        .collect(Collectors.toList());
    long shift = 0;

    long currentInterval = System.currentTimeMillis() / 1000 / 30;

    int first = codes.get(0);
    for (long i = -pastIntervals; i <= futureIntervals; i++) {
      int generated = generate(currentInterval + i);
      if (first == generated) {
        boolean codesOkay = true;
        shift = i;
        for (int j = 1; j < codes.size(); j++) {
          int next = generate(currentInterval + i + j);
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

  private int generate(long interval) {
    return hash(interval);
  }

  private int hash(long interval) {
    byte[] hash = {};
    try {
      hash = new Hmac(Hash.SHA1, Base32.decode(this.secret), interval).digest();
    }
    catch (NoSuchAlgorithmException | InvalidKeyException | Base32.DecodingException e) {
      e.printStackTrace();
    }
    return bytesToInt(hash);
  }

  private static int bytesToInt(byte[] hash) {
    // put selected bytes into result int
    int offset = hash[hash.length - 1] & 0xf;

    int binary = (hash[offset] & 0x7f) << 24 | (hash[offset + 1] & 0xff) << 16
        | (hash[offset + 2] & 0xff) << 8 | hash[offset + 3] & 0xff;

    return binary % Digits.SIX.getValue();
  }

}