package ru.itmo.rusinov.consensus.raft.model.raft;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class AppendEntriesRPC {
    private final String leaderId;
    private final Long prevLogIndex;
    private final Long prevLogTerm;
    private final List<LogEntry> entries;
    private final long leaderCommit;

    @JsonCreator
    public AppendEntriesRPC(
            @JsonProperty("leaderId") String leaderId,
            @JsonProperty("prevLogIndex") Long prevLogIndex,
            @JsonProperty("prevLogTerm") Long prevLogTerm,
            @JsonProperty("entries") List<LogEntry> entries,
            @JsonProperty("leaderCommit") long leaderCommit) {
        this.leaderId = leaderId;
        this.prevLogIndex = prevLogIndex;
        this.prevLogTerm = prevLogTerm;
        this.entries = entries;
        this.leaderCommit = leaderCommit;
    }
}
