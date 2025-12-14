package ru.itmo.rusinov.consensus.raft.model.raft;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class AppendEntriesRPCResult {
    private final boolean success;

    @JsonCreator
    public AppendEntriesRPCResult(@JsonProperty("success") boolean success) {
        this.success = success;
    }
}
