package com.example.myhikaricp;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

public class MyConnection {
  private AtomicReference<State> state;
  private String id;
  private Instant createdAt;
  private Instant lastUsedAt;
  private Connection connection;

  public MyConnection(String url, String user, String password) throws SQLException {
    this.id = UUID.randomUUID().toString();
    this.createdAt = Instant.now();
    this.lastUsedAt = Instant.now();
    this.state = new AtomicReference<>(State.AVAILABLE);
    this.connection = DriverManager.getConnection(url, user, password);
  }

  public boolean isAlive() throws SQLException {
    try (PreparedStatement preparedStatement = connection.prepareStatement("select 1")) {
      return preparedStatement.execute();
    } catch (Exception exception) {
      return false;
    }
  }

  public String getId() {
    return this.id;
  }

  public Instant getCreatedAt() {
    return this.createdAt;
  }

  public Instant getLastUsedAt() {
    return this.lastUsedAt;
  }

  public void updateLastUsedAt() {
    this.lastUsedAt = Instant.now();
  }

  public State getState() {
    return state.get();
  }

  public void setState(State state) {
    this.state.set(state);
  }

  public boolean connectionAcquired(State state) {
    return this.state.compareAndSet(MyConnection.State.AVAILABLE, state);
  }

  public void close() throws SQLException {
    connection.close();
  }

  public enum State {
    AVAILABLE,
    IN_USE,
    RESERVED,
    CLOSED
  }
}
