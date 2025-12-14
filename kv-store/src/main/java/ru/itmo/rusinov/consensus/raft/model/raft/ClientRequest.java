package ru.itmo.rusinov.consensus.raft.model.raft;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class ClientRequest {
    private final RaftCommand request;

    @JsonCreator
    public ClientRequest(@JsonProperty("request") RaftCommand request) {
        this.request = request;
    }
}
