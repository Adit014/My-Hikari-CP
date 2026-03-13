package com.example.myhikaricp;

public class ConnectionProperties {
  private final String url;
  private final String username;
  private final String password;
  private final int minIdle;
  private final long keepAlive;
  private final long maxAlive;
  private final long connectionTimeout;
  private final int maxSize;

  public ConnectionProperties(String url, String username, String password, int minIdle, long keepAlive,
      long maxAlive, long connectionTimeout, int maxSize) {
    this.url = url;
    this.username = username;
    this.password = password;
    this.minIdle = minIdle;
    this.keepAlive = keepAlive;
    this.maxAlive = maxAlive;
    this.connectionTimeout = connectionTimeout;
    this.maxSize = maxSize;
  }

  public String getUrl() {
    return url;
  }

  public String getUsername() {
    return username;
  }

  public String getPassword() {
    return password;
  }

  public int getMinIdle() {
    return minIdle;
  }

  public long getKeepAlive() {
    return keepAlive;
  }

  public long getMaxAlive() {
    return maxAlive;
  }

  public long getConnectionTimeout() {
    return connectionTimeout;
  }

  public int getMaxSize() {
    return maxSize;
  }
}