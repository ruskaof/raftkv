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
    @JsonSubTypes.Type(value = RaftMessage.AppendEntriesRaftMessage.class, name = "append_entries"),
    @JsonSubTypes.Type(value = RaftMessage.AppendEntriesResultRaftMessage.class, name = "append_entries_result"),
    @JsonSubTypes.Type(value = RaftMessage.RequestVoteRaftMessage.class, name = "request_vote"),
    @JsonSubTypes.Type(value = RaftMessage.RequestVoteResultRaftMessage.class, name = "request_vote_result"),
    @JsonSubTypes.Type(value = RaftMessage.HeartBeatRaftMessage.class, name = "heartbeat"),
    @JsonSubTypes.Type(value = RaftMessage.ElectionTimeoutElapsedRaftMessage.class, name = "election_timeout_elapsed"),
    @JsonSubTypes.Type(value = RaftMessage.ClientRequestRaftMessage.class, name = "client_request")
})
public abstract class RaftMessage {
    private final String src;
    private final Long term;
    private final String type;

    @JsonCreator
    protected RaftMessage(
            @JsonProperty("src") String src,
            @JsonProperty("term") Long term,
            @JsonProperty("type") String type) {
        this.src = src;
        this.term = term;
        this.type = type;
    }

    public static RaftMessage createAppendEntries(String src, Long term, AppendEntriesRPC appendEntries) {
        return new AppendEntriesRaftMessage(src, term, appendEntries);
    }

    public static RaftMessage createAppendEntriesResult(String src, Long term, AppendEntriesRPCResult appendEntriesResult) {
        return new AppendEntriesResultRaftMessage(src, term, appendEntriesResult);
    }

    public static RaftMessage createRequestVote(String src, Long term, RequestVoteRPC requestVote) {
        return new RequestVoteRaftMessage(src, term, requestVote);
    }

    public static RaftMessage createRequestVoteResult(String src, Long term, RequestVoteRPCResult requestVoteResult) {
        return new RequestVoteResultRaftMessage(src, term, requestVoteResult);
    }

    public static RaftMessage createHeartBeat(String src, Long term) {
        return new HeartBeatRaftMessage(src, term);
    }

    public static RaftMessage createElectionTimeoutElapsed() {
        return new ElectionTimeoutElapsedRaftMessage(null, null);
    }

    public static RaftMessage createClientRequest(String src, Long term, InternalClientRequest clientRequest) {
        return new ClientRequestRaftMessage(src, term, clientRequest);
    }

    @Data
    public static class AppendEntriesRaftMessage extends RaftMessage {
        private final AppendEntriesRPC appendEntries;

        @JsonCreator
        public AppendEntriesRaftMessage(
                @JsonProperty("src") String src,
                @JsonProperty("term") Long term,
                @JsonProperty("appendEntries") AppendEntriesRPC appendEntries) {
            super(src, term, "append_entries");
            this.appendEntries = appendEntries;
        }
    }

    @Data
    public static class AppendEntriesResultRaftMessage extends RaftMessage {
        private final AppendEntriesRPCResult appendEntriesResult;

        @JsonCreator
        public AppendEntriesResultRaftMessage(
                @JsonProperty("src") String src,
                @JsonProperty("term") Long term,
                @JsonProperty("appendEntriesResult") AppendEntriesRPCResult appendEntriesResult) {
            super(src, term, "append_entries_result");
            this.appendEntriesResult = appendEntriesResult;
        }
    }

    @Data
    public static class RequestVoteRaftMessage extends RaftMessage {
        private final RequestVoteRPC requestVote;

        @JsonCreator
        public RequestVoteRaftMessage(
                @JsonProperty("src") String src,
                @JsonProperty("term") Long term,
                @JsonProperty("requestVote") RequestVoteRPC requestVote) {
            super(src, term, "request_vote");
            this.requestVote = requestVote;
        }
    }

    @Data
    public static class RequestVoteResultRaftMessage extends RaftMessage {
        private final RequestVoteRPCResult requestVoteResult;

        @JsonCreator
        public RequestVoteResultRaftMessage(
                @JsonProperty("src") String src,
                @JsonProperty("term") Long term,
                @JsonProperty("requestVoteResult") RequestVoteRPCResult requestVoteResult) {
            super(src, term, "request_vote_result");
            this.requestVoteResult = requestVoteResult;
        }
    }

    @Data
    public static class HeartBeatRaftMessage extends RaftMessage {
        private final HeartBeat heartbeat;

        @JsonCreator
        public HeartBeatRaftMessage(
                @JsonProperty("src") String src,
                @JsonProperty("term") Long term) {
            super(src, term, "heartbeat");
            this.heartbeat = new HeartBeat();
        }
    }

    @Data
    public static class ElectionTimeoutElapsedRaftMessage extends RaftMessage {
        private final ElectionTimeoutElapsed electionTimeoutElapsed;

        @JsonCreator
        public ElectionTimeoutElapsedRaftMessage(
                @JsonProperty("src") String src,
                @JsonProperty("term") Long term) {
            super(src, term, "election_timeout_elapsed");
            this.electionTimeoutElapsed = new ElectionTimeoutElapsed();
        }
    }

    @Data
    public static class ClientRequestRaftMessage extends RaftMessage {
        private final InternalClientRequest clientRequest;

        @JsonCreator
        public ClientRequestRaftMessage(
                @JsonProperty("src") String src,
                @JsonProperty("term") Long term,
                @JsonProperty("clientRequest") InternalClientRequest clientRequest) {
            super(src, term, "client_request");
            this.clientRequest = clientRequest;
        }
    }
}
