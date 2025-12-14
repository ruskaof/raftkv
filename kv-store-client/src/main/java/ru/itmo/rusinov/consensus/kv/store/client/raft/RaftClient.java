package ru.itmo.rusinov.consensus.kv.store.client.raft;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import ru.itmo.rusinov.consensus.raft.model.GetMessage;
import ru.itmo.rusinov.consensus.raft.model.KvStoreProtoMessage;
import ru.itmo.rusinov.consensus.raft.model.SetMessage;
import ru.itmo.rusinov.consensus.raft.model.JacksonObjectMapper;
import ru.itmo.rusinov.consensus.raft.model.raft.*;
import ru.itmo.rusinov.consensus.raft.network.EnvironmentClient;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
public class RaftClient {

    private final List<String> replicaIds;
    private final EnvironmentClient environmentClient;
    private final AtomicReference<String> currentLeader;

    public RaftClient(List<String> replicaIds, EnvironmentClient environmentClient) {
        this.replicaIds = replicaIds;
        this.environmentClient = environmentClient;
        this.currentLeader = new AtomicReference<>(replicaIds.getFirst());
    }

    @SneakyThrows
    private byte[] sendRaftMessage(RaftServerRequest raftServerRequest) {
        for (var retry = 0; retry < Integer.MAX_VALUE; retry ++) {
            var leader = currentLeader.get();

            try {
                log.info("Sending set to raft replica {}", leader);
                var responseBytes = environmentClient.sendMessage(JacksonObjectMapper.serialize(raftServerRequest), leader).get();
                var response = JacksonObjectMapper.deserialize(responseBytes, ClientResponse.class);
                if (response.getCommandResult() == null) {
                    if (response.getSuggestedLeader() != null) {
                        log.info("Suggested leader: {}", response.getSuggestedLeader());
                        currentLeader.compareAndSet(leader, response.getSuggestedLeader());
                    } else {
                        changeCurrentLeader(leader);
                    }
                } else {
                    return response.getCommandResult().getValue();
                }
            } catch (Exception e) {
                log.warn("Got error:", e);
                changeCurrentLeader(leader);
            }
        }

        throw new RuntimeException("Could not send request to any replica");
    }

    private void changeCurrentLeader(String leader) {
        currentLeader.compareAndSet(leader, replicaIds.get((replicaIds.indexOf(leader) + 1) % replicaIds.size()));
    }

    @SneakyThrows
    public void setStringValue(String key, String value) {
        var setMessage = new SetMessage(key.getBytes(), value.getBytes());
        var kvStoreMessage = KvStoreProtoMessage.createSet(setMessage);
        var raftCommand = new RaftCommand(JacksonObjectMapper.serialize(kvStoreMessage));
        var clientRequest = new ClientRequest(raftCommand);
        var serverRequest = RaftServerRequest.createClientRequest(clientRequest);

        sendRaftMessage(serverRequest);
    }

    @SneakyThrows
    public String getStringValue(String key) {
        var getMessage = new GetMessage(key.getBytes());
        var kvStoreMessage = KvStoreProtoMessage.createGet(getMessage);
        var raftCommand = new RaftCommand(JacksonObjectMapper.serialize(kvStoreMessage));
        var clientRequest = new ClientRequest(raftCommand);
        var serverRequest = RaftServerRequest.createClientRequest(clientRequest);

        return new String(sendRaftMessage(serverRequest));
    }
}
