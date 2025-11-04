package jetbrains.buildServer.deployer.agent.ftp;

public enum DataChannelProtection {
  DISABLE('D'), CLEAR('C'), SAFE('S'), CONFIDENTIAL('E'), PRIVATE('P');

  private final char code;

  DataChannelProtection(char code) {
    this.code = code;
  }

  public char getCode() {
    return code;
  }

  public String getCodeAsString() {
    return "" + code;
  }

  public static DataChannelProtection getByCode(String code, DataChannelProtection defaultValue) {
    if (code == null)
      return defaultValue;
    for (DataChannelProtection dcp: values()) {
      if (dcp.code == code.charAt(0))
        return dcp;
    }
    return defaultValue;
  }

  public boolean isDisabled() {
    return this == DISABLE;
  }

  public boolean isNotDisabled() {
    return !isDisabled();
  }
}
