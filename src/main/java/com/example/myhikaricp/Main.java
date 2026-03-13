package com.example.myhikaricp;

import java.sql.SQLException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class Main {
  public static void main(String[] args) throws SQLException, InterruptedException {
    ConnectionProperties connectionProperties = new ConnectionProperties("jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1", "sa",
        "", 2, 5000, 60000, 3000, 5);

    ConnectionProvider provider = new ConnectionProvider(connectionProperties);

    System.out.println("\n Getting one connection and releasing it \n");

    MyConnection connection = provider.getConnection();
    System.out.println("\n Connection Received -- " + connection.getId() + " [" + System.currentTimeMillis() + "]\n");

    provider.getCurrentState();

    System.out.println("Releasing connection " + connection.getId() + " [" + System.currentTimeMillis() + "]\n");
    provider.release(connection);

    provider.getCurrentState();

    System.out.println("\n--- 5 threads: get connection, hold 200ms, release ---\n");
    ExecutorService executor = Executors.newFixedThreadPool(5);
    for (int i = 0; i < 5; i++) {
      final int threadId = i + 1;
      executor.submit(() -> {
        try {
          MyConnection conn = provider.getConnection();
          System.out.println("Thread " + threadId + " got connection " + conn.getId() + " [" + System.currentTimeMillis() + "]");
          Thread.sleep(200);
          provider.release(conn);
          System.out.println("Thread " + threadId + " released connection " + conn.getId() + " [" + System.currentTimeMillis() + "]");
        } catch (InterruptedException | SQLException e) {
          Thread.currentThread().interrupt();
          throw new RuntimeException(e);
        }
      });
    }
    executor.shutdown();
    executor.awaitTermination(10, TimeUnit.SECONDS);
    System.out.println("\nFinal state:");
    provider.getCurrentState();

    System.out.println("\n--- Pool exhaustion: 6 threads, max 5 connections (one waits then gets) ---\n");
    ExecutorService exhaustionExecutor = Executors.newFixedThreadPool(6);
    for (int i = 0; i < 6; i++) {
      final int threadId = i + 1;
      exhaustionExecutor.submit(() -> {
        try {
          System.out.println("Thread " + threadId + " requesting connection... [" + System.currentTimeMillis() + "]");
          MyConnection conn = provider.getConnection();
          System.out.println("Thread " + threadId + " got connection " + conn.getId() + " [" + System.currentTimeMillis() + "]");
          Thread.sleep(200);
          provider.release(conn);
          System.out.println("Thread " + threadId + " released connection " + conn.getId() + " [" + System.currentTimeMillis() + "]");
        } catch (InterruptedException | SQLException e) {
          Thread.currentThread().interrupt();
          throw new RuntimeException(e);
        }
      });
    }
    exhaustionExecutor.shutdown();
    exhaustionExecutor.awaitTermination(15, TimeUnit.SECONDS);
    System.out.println("\nPool exhaustion final state:");
    provider.getCurrentState();

    System.out.println("Shutting down");
    provider.shutdown();
    provider.getCurrentState();
  }
}
