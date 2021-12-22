package jetbrains.buildServer.deployer.agent.ftp;

public enum DataChannelProtection {
  DISABLE('D'), CLEAR('C'), SAFE('S'), CONFIDENTIAL('E'), PRIVATE('P');

  private final char code;

  private DataChannelProtection(char code) {
    this.code = code;
  }

  public char getCode() {
    return code;
  }

  public String getCodeAsString() {
    return "" + code;
  }

  public static DataChannelProtection getByCode(String code) {
    if (code == null)
      return DISABLE;
    for (DataChannelProtection dcp: values()) {
      if (dcp.code == code.charAt(0))
        return dcp;
    }
    throw new RuntimeException("DataChannelProtection code not found: " + code);
  }

  public boolean isDisabled() {
    return this == DISABLE;
  }
}
