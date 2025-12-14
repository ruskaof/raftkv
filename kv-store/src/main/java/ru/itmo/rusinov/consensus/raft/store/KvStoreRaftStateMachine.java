package ru.itmo.rusinov.consensus.raft.store;

import lombok.SneakyThrows;
import ru.itmo.rusinov.consensus.raft.model.GetMessage;
import ru.itmo.rusinov.consensus.raft.model.KvStoreProtoMessage;
import ru.itmo.rusinov.consensus.raft.model.SetMessage;
import ru.itmo.rusinov.consensus.raft.model.JacksonObjectMapper;
import ru.itmo.rusinov.consensus.raft.raft.StateMachine;

import java.io.File;
import java.util.Optional;

public class KvStoreRaftStateMachine implements StateMachine {
    private final KvDatabase database;

    public KvStoreRaftStateMachine(KvDatabase database) {
        this.database = database;
    }

    @Override
    public void initialize(File stateMachineDir) {
        File dbFile = new File(stateMachineDir, "dbData");

        database.initialize(dbFile.toPath());
    }

    @SneakyThrows
    @Override
    public byte[] applyCommand(byte[] command) {
        var kvStoreMessage = JacksonObjectMapper.deserialize(command, KvStoreProtoMessage.class);

        if (kvStoreMessage instanceof KvStoreProtoMessage.GetKvStoreProtoMessage) {
            var get = ((KvStoreProtoMessage.GetKvStoreProtoMessage) kvStoreMessage).getGet();
            return handleGet(get);
        } else if (kvStoreMessage instanceof KvStoreProtoMessage.SetKvStoreProtoMessage) {
            var set = ((KvStoreProtoMessage.SetKvStoreProtoMessage) kvStoreMessage).getSet();
            return handleSet(set);
        }
        return null;
    }

    private byte[] handleSet(SetMessage set) {
        database.put(set.getKey(), set.getValue());
        return new byte[0];
    }

    private byte[] handleGet(GetMessage get) {
        var bytes = database.get(get.getKey());
        return Optional.ofNullable(bytes).orElse(new byte[0]);
    }
}
