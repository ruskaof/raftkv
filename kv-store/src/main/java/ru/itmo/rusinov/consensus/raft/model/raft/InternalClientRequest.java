package ru.itmo.rusinov.consensus.raft.model.raft;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class InternalClientRequest {
    private final ClientRequest clientRequest;
    private final String requestId;

    @JsonCreator
    public InternalClientRequest(
            @JsonProperty("clientRequest") ClientRequest clientRequest,
            @JsonProperty("requestId") String requestId) {
        this.clientRequest = clientRequest;
        this.requestId = requestId;
    }
}
