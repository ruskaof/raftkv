package ru.itmo.rusinov.consensus.raft.model.raft;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class LogEntry {
    private final RaftCommand command;
    private final long term;

    @JsonCreator
    public LogEntry(@JsonProperty("command") RaftCommand command, @JsonProperty("term") long term) {
        this.command = command;
        this.term = term;
    }
}
