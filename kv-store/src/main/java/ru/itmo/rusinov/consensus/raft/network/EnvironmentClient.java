package ru.itmo.rusinov.consensus.raft.network;

import java.util.concurrent.CompletableFuture;

public interface EnvironmentClient extends AutoCloseable {
    void initialize();

    CompletableFuture<byte[]> sendMessage(byte[] message, String serverId);
}
