package ru.itmo.rusinov.consensus.raft.model.raft;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class RequestVoteRPC {
    private final String candidateId;
    private final long lastLogIndex;
    private final long lastLogTerm;

    @JsonCreator
    public RequestVoteRPC(
            @JsonProperty("candidateId") String candidateId,
            @JsonProperty("lastLogIndex") long lastLogIndex,
            @JsonProperty("lastLogTerm") long lastLogTerm) {
        this.candidateId = candidateId;
        this.lastLogIndex = lastLogIndex;
        this.lastLogTerm = lastLogTerm;
    }
}
