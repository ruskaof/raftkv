package ru.itmo.rusinov.consensus.raft;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import ru.itmo.rusinov.consensus.raft.network.SimpleDistributedServer;
import ru.itmo.rusinov.consensus.raft.network.SimpleEnvironmentClient;
import ru.itmo.rusinov.consensus.raft.raft.MapDBDurableStateStore;
import ru.itmo.rusinov.consensus.raft.raft.RaftServer;
import ru.itmo.rusinov.consensus.raft.store.KvStoreRaftStateMachine;
import ru.itmo.rusinov.consensus.raft.store.MapDbKvDatabase;

import java.io.File;
import java.util.Arrays;
import java.util.stream.Collectors;

@Slf4j
public class KvStoreApplication {


    public static void main(String[] args) {
        runOnRaft();
    }

    @SneakyThrows
    private static void runOnRaft() {
        String id = System.getenv("KV_PEER_ID");
        int port = Integer.parseInt(System.getenv("KV_PORT"));
        var destinations = Arrays.stream(System.getenv("KV_PEERS").split(","))
                .collect(Collectors.toMap(
                        (p) -> p.split(":")[0],
                        (p) -> {
                            var addressParts = p.split(":");
                            return addressParts[1] + ":" + addressParts[2];
                        }
                ));
        String storagePath = System.getenv("KV_STORAGE_PATH");
        var stateMachine = new KvStoreRaftStateMachine(new MapDbKvDatabase());

        var server = new RaftServer(
                new MapDBDurableStateStore(),
                id,
                destinations.keySet(),
                new SimpleEnvironmentClient(destinations, 150),
                new SimpleDistributedServer(port),
                stateMachine,
                new File(storagePath)
        );

        server.start();

        while (true) {
            Thread.sleep(1000);
        }
    }
}
