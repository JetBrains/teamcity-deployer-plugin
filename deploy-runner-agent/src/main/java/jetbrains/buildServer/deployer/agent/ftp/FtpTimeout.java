package jetbrains.buildServer.deployer.agent.ftp;

import com.intellij.openapi.diagnostic.Logger;
import jetbrains.buildServer.agent.BuildRunnerContext;
import jetbrains.buildServer.deployer.common.FTPRunnerConstants;

public class FtpTimeout {
    private static final Logger LOG = Logger.getInstance(FtpTimeout.class.getName());
    private static final int DEFAULT_FTP_CONNECT_TIMEOUT = 30 * 1000 * 60; // 30 Min

    private int connectTimeout = -1;
    private int dataTimeout = -1;
    private int socketTimeout = -1;
    private int controlKeepAliveTimeout = -1;

    public static FtpTimeout parseTimeout(BuildRunnerContext context) {
        FtpTimeout fct = new FtpTimeout();
        String timeout = context.getBuild().getSharedConfigParameters().get(FTPRunnerConstants.PARAM_FTP_CONNECT_TIMEOUT);
        if (timeout != null && timeout.isEmpty()) {
            String[] timeouts = timeout.split(" ");
            try {
                if (timeouts.length == 1) {
                    fct.setAllTimeout(getTimeoutFromString(timeout, DEFAULT_FTP_CONNECT_TIMEOUT));
                } else if (timeouts.length == 3) {
                    fct.setTimeouts(getTimeoutFromString(timeouts[0], DEFAULT_FTP_CONNECT_TIMEOUT),
                            getTimeoutFromString(timeouts[1], DEFAULT_FTP_CONNECT_TIMEOUT),
                            getTimeoutFromString(timeouts[2], DEFAULT_FTP_CONNECT_TIMEOUT));
                }
            } catch (NumberFormatException err) {
                LOG.warn("Incorrect format of ftp connect timeout '" + timeout + "'. " +
                        "Expecting either single value integer either three integers for " +
                        "1. socketTimeout  2. connectTimeout  3. dataTimeout. " +
                        "Default value " + DEFAULT_FTP_CONNECT_TIMEOUT + "ms was used for 2. and 3.");
            }
        }
        String keepAliveTimeout = context.getBuild().getSharedConfigParameters().get(FTPRunnerConstants.PARAM_FTP_CONTROL_KEEP_ALIVE_TIMEOUT);
        if (keepAliveTimeout == null || keepAliveTimeout.isEmpty()) {
            try {
                fct.setControlKeepAliveTimeout(getTimeoutFromString(keepAliveTimeout, -1));
            } catch (NumberFormatException e) {
                LOG.warn("Incorrect value of controlKeepAliveTimeout '" + keepAliveTimeout + "'. " +
                        "Disabling setting this timeout by default.");
            }
        }
        return fct;
    }

    private static int getTimeoutFromString(String timeout, int defaultValue) {
        int timeoutAsInteger = Integer.parseInt(timeout);
        return (timeoutAsInteger > 0) ? timeoutAsInteger : defaultValue;
    }

    private void setAllTimeout(int timeout) {
      socketTimeout = timeout;
      connectTimeout = timeout;
      dataTimeout = timeout;
    }

    private void setTimeouts(int socketTimeout,
                             int connectTimeout,
                             int dataTimeout) {
      this.socketTimeout = socketTimeout;
      this.connectTimeout = connectTimeout;
      this.dataTimeout = dataTimeout;
    }

    private void setControlKeepAliveTimeout(int timeout) {
      this.controlKeepAliveTimeout = timeout;
    }

    public boolean controlKeepAliveTimeoutEnabled() {
      return controlKeepAliveTimeout > 0;
    }

    public int getConnectTimeout() {
      return connectTimeout;
    }

    public boolean connectTimeoutEnabled() {
      return connectTimeout > 0;
    }

    public boolean dataTimeoutEnabled() {
      return dataTimeout > 0;
    }

    public long getControlKeepAliveTimeoutInSecs() {
        return controlKeepAliveTimeout;
    }

    public int getDataTimeout() {
        return dataTimeout;
    }

    public boolean socketTimeoutEnabled() {
        return socketTimeout > 0;
    }

    public int getSocketTimeout() {
        return socketTimeout;
    }
}