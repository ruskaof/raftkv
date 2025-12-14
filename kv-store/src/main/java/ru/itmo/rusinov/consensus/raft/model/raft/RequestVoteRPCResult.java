package ru.itmo.rusinov.consensus.raft.model.raft;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class RequestVoteRPCResult {
    private final boolean voteGranted;

    @JsonCreator
    public RequestVoteRPCResult(@JsonProperty("voteGranted") boolean voteGranted) {
        this.voteGranted = voteGranted;
    }
}
