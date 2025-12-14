package ru.itmo.rusinov.consensus.raft.model.raft;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class RaftCommandResult {
    private final byte[] value;

    @JsonCreator
    public RaftCommandResult(@JsonProperty("value") byte[] value) {
        this.value = value;
    }
}
