package ru.itmo.rusinov.consensus.raft.model.raft;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class ClientResponse {
    private final RaftCommandResult commandResult;
    private final String suggestedLeader;

    @JsonCreator
    public ClientResponse(
            @JsonProperty("commandResult") RaftCommandResult commandResult,
            @JsonProperty("suggestedLeader") String suggestedLeader) {
        this.commandResult = commandResult;
        this.suggestedLeader = suggestedLeader;
    }
}
