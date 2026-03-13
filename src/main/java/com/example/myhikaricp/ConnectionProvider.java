package com.example.myhikaricp;

import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedTransferQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class ConnectionProvider {
  private final CopyOnWriteArrayList<MyConnection> connections = new CopyOnWriteArrayList<>();
  private final ThreadLocal<List<MyConnection>> localConnections = ThreadLocal.withInitial(ArrayList::new);
  private final LinkedTransferQueue<MyConnection> linkedTransferQueue = new LinkedTransferQueue<>();
  private final ConnectionProperties connectionProperties;
  private AtomicInteger currentSize = new AtomicInteger(0);
  private AtomicBoolean isEnabled = new AtomicBoolean(false);
  private ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

  public ConnectionProvider(ConnectionProperties connectionProperties) throws SQLException {
    this.connectionProperties = connectionProperties;
    List<MyConnection> list = new ArrayList<>();
    for (int i = 0; i < connectionProperties.getMinIdle(); i++) {
      list.add(new MyConnection(connectionProperties.getUrl(), connectionProperties.getUsername(),
          connectionProperties.getPassword()));
    }
    connections.addAll(list);
    currentSize = new AtomicInteger(connections.size());
    isEnabled.set(true);
    scheduler.scheduleWithFixedDelay(() -> {
      try {
        startScheduler();
      } catch (SQLException e) {
        System.err.println("Error while running scheduler job" + e.getMessage());
      }
    }, connectionProperties.getKeepAlive(), connectionProperties.getKeepAlive(), TimeUnit.MILLISECONDS);
    getCurrentState();
  }

  public MyConnection getConnection() throws InterruptedException, SQLException {
    if (!isEnabled.get()) {
      throw new ConnectionNotAvailableException("Shutdown !!!!!");
    }
    final long startTime = System.currentTimeMillis();
    MyConnection connection = null;
    Iterator<MyConnection> iterator = localConnections.get().iterator();
    while (iterator.hasNext()) {
      MyConnection conn = iterator.next();
      if (conn.getState() == MyConnection.State.AVAILABLE) {
        if (conn.connectionAcquired(MyConnection.State.IN_USE)) {
          return conn;
        }
      } else {
        iterator.remove();
      }
    }

    connection = connections.stream()
        .filter(conn -> conn.getState() == MyConnection.State.AVAILABLE)
        .findFirst()
        .orElse(null);

    if (connection != null) {
      if (connection.connectionAcquired(MyConnection.State.IN_USE)) {
        localConnections.get().add(connection);
        return connection;
      }
    }

    long elapsed = System.currentTimeMillis() - startTime;

    if (currentSize.incrementAndGet() <= connectionProperties.getMaxSize()) {
      MyConnection conn = new MyConnection(connectionProperties.getUrl(), connectionProperties.getUsername(),
          connectionProperties.getPassword());
      conn.setState(MyConnection.State.IN_USE);
      connections.add(conn);
      localConnections.get().add(conn);
      return conn;
    }
    currentSize.decrementAndGet();
    connection = linkedTransferQueue.poll(connectionProperties.getConnectionTimeout() - elapsed, TimeUnit.MILLISECONDS);
    if (connection != null) {
      localConnections.get().add(connection);
      return connection;
    }
    throw new ConnectionNotAvailableException(
        "Connection not available after " + connectionProperties.getConnectionTimeout() + " ms");
  }

  public void release(MyConnection connection) throws SQLException, InterruptedException {
    if (!isEnabled.get()) {
      connection.close();
      return;
    }

    if (!connection.isAlive()) {
      removeConnection(connection);
      return;
    }
    if (linkedTransferQueue.hasWaitingConsumer()) {
      connection.connectionAcquired(MyConnection.State.IN_USE);
      if (linkedTransferQueue.tryTransfer(connection)) {
        return;
      }
    }
    connection.setState(MyConnection.State.AVAILABLE);
    connection.updateLastUsedAt();
  }

  public void shutdown() throws SQLException, InterruptedException {
    isEnabled.set(false);
    scheduler.shutdown();
    while (linkedTransferQueue.hasWaitingConsumer()) {
      linkedTransferQueue.offer(null);
    }
    while (connections.stream().anyMatch(c -> c.getState() == MyConnection.State.IN_USE)) {
      Thread.sleep(50);
    }

    for (MyConnection conn : connections) {
      conn.close();
    }

    connections.clear();
  }

  public void startScheduler() throws SQLException {
    for (MyConnection conn : connections) {
      if (conn.connectionAcquired(MyConnection.State.RESERVED)) {
        if (!conn.isAlive()) {
          removeConnection(conn);
        } else {
          Instant now = Instant.now();
          long ageMillis = Duration.between(conn.getCreatedAt(), now).toMillis();
          if (ageMillis > connectionProperties.getMaxAlive()) {
            removeConnection(conn);
          } else {
            conn.setState(MyConnection.State.AVAILABLE);
          }
        }
      }
    }
  }

  private void removeConnection(MyConnection connection) throws SQLException {
    connections.remove(connection);

    int size = currentSize.decrementAndGet();

    while (size < connectionProperties.getMinIdle()) {
      if (currentSize.compareAndSet(size, size + 1)) {
        connections.add(new MyConnection(
            connectionProperties.getUrl(),
            connectionProperties.getUsername(),
            connectionProperties.getPassword()));
      }
      size = currentSize.get();
    }
  }

  public void getCurrentState() {
    System.out.println("-----------------------------------------------------");
    System.out.println("--------- Current State Of Connections -----------");
    System.out.printf("%-40s %-12s%n", "Connection ID", "State");
    System.out.println("---------------------------------------- ------------");
    for (MyConnection connection : connections) {
      System.out.printf("%-40s %-12s%n", connection.getId(), connection.getState());
    }
    System.out.println("-----------------------------------------------------");
  }
}