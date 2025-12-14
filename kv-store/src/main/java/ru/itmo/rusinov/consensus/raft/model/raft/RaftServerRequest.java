package ru.itmo.rusinov.consensus.raft.model.raft;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Data;

@Data
@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    property = "type"
)
@JsonSubTypes({
    @JsonSubTypes.Type(value = RaftServerRequest.ClientRequestRaftServerRequest.class, name = "client_request"),
    @JsonSubTypes.Type(value = RaftServerRequest.RaftMessageRaftServerRequest.class, name = "raft_message")
})
public abstract class RaftServerRequest {
    private final String type;

    @JsonCreator
    protected RaftServerRequest(@JsonProperty("type") String type) {
        this.type = type;
    }

    public static RaftServerRequest createClientRequest(ClientRequest clientRequest) {
        return new ClientRequestRaftServerRequest(clientRequest);
    }

    public static RaftServerRequest createRaftMessage(RaftMessage raftMessage) {
        return new RaftMessageRaftServerRequest(raftMessage);
    }

    @Data
    public static class ClientRequestRaftServerRequest extends RaftServerRequest {
        private final ClientRequest clientRequest;

        @JsonCreator
        public ClientRequestRaftServerRequest(@JsonProperty("clientRequest") ClientRequest clientRequest) {
            super("client_request");
            this.clientRequest = clientRequest;
        }
    }

    @Data
    public static class RaftMessageRaftServerRequest extends RaftServerRequest {
        private final RaftMessage raftMessage;

        @JsonCreator
        public RaftMessageRaftServerRequest(@JsonProperty("raftMessage") RaftMessage raftMessage) {
            super("raft_message");
            this.raftMessage = raftMessage;
        }
    }
}
