package jetbrains.buildServer.deployer.common;

public enum FtpSSLMode {
  NONE(0),
  FTPS(1), // FTP Implicit Security port 990
  FTPES(2); // FTP Explicit Security port 21, AUTH TLS

  private final int code;

  FtpSSLMode(int code) {
    this.code = code;
  }

  public static FtpSSLMode getByCode(String value, FtpSSLMode defaultValue) {
    Integer code = parseIntOrNull(value);
    if (code == null) {
      return defaultValue;
    }
    for (FtpSSLMode sslMode: values()) {
      if (sslMode.code == code)
        return sslMode;
    }

    return defaultValue;
  }

  public int getCode() {
    return code;
  }

  public boolean isImplicit() {
    return this == FTPS;
  }

  public boolean isExplicit() {
    return this == FTPES;
  }

  public boolean isNonSecure() {
    return this == NONE;
  }

  public boolean isSecure() {
    return !isNonSecure();
  }

  public static Integer parseIntOrNull(String str) {
    if (str == null) {
      return null;
    }
    try {
      return Integer.parseInt(str);
    } catch (NumberFormatException e) {
      return null;
    }
  }
}
