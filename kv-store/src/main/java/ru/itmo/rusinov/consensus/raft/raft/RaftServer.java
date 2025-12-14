package ru.itmo.rusinov.consensus.raft.raft;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import ru.itmo.rusinov.consensus.raft.model.JacksonObjectMapper;
import ru.itmo.rusinov.consensus.raft.model.raft.*;
import ru.itmo.rusinov.consensus.raft.network.DistributedServer;
import ru.itmo.rusinov.consensus.raft.network.EnvironmentClient;

import java.io.File;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingQueue;

@Slf4j
public class RaftServer {
    private final DurableStateStore durableStateStore;
    private final String id;

    private long currentTerm;
    private String votedFor;
    private final Map<Long, LogEntry> logEntries;
    private long lastIndex;
    private long commitIndex = 0;
    private long lastApplied = 0;

    private final Map<String, Long> nextIndex = new HashMap<>();
    private final Map<String, Long> matchIndex = new HashMap<>();

    private final Set<String> replicas;

    private final EnvironmentClient environmentClient;
    private final DistributedServer distributedServer;

    private final StateMachine stateMachine;

    private final Set<String> currentElectionVoters = new HashSet<>();

    private final BlockingQueue<RaftMessage> messagesQueue = new LinkedBlockingQueue<>();

    private volatile RaftRole currentRole;
    private final TimeoutTimer electionTimer = new TimeoutTimer();

    private final Map<UUID, CompletableFuture<ClientResponse>> awaitingClientRequests = new HashMap<>();
    private final Map<Long, CompletableFuture<ClientResponse>> inFlightClientRequests = new HashMap<>();

    private final Map<String, Long> maxSentLogs = new HashMap<>();

    public RaftServer(DurableStateStore durableStateStore, String id, Set<String> replicas, EnvironmentClient environmentClient, DistributedServer distributedServer, StateMachine stateMachine, File storagePath) {
        durableStateStore.initialize(storagePath);
        stateMachine.initialize(storagePath);

        this.durableStateStore = durableStateStore;
        currentTerm = durableStateStore.loadCurrentTerm().orElse(0L);
        votedFor = durableStateStore.loadVotedFor().orElse(null);
        logEntries = durableStateStore.loadLog();
        this.id = id;
        this.replicas = Set.copyOf(replicas);
        this.environmentClient = environmentClient;
        this.distributedServer = distributedServer;
        this.stateMachine = stateMachine;

        lastIndex = logEntries.keySet().stream().max(Comparator.naturalOrder()).orElse(0L);
        currentRole = RaftRole.FOLLOWER;
    }

    public void start() {
        distributedServer.setRequestHandler(this::handleMessage);
        distributedServer.initialize();
        environmentClient.initialize();

        new Thread(new ElectionTimerChecker()).start();
        new Thread(new HeartbeatSender(Duration.ofMillis(25))).start();
        new Thread(new ServerThread()).start();
    }

    private void handleMessage(RaftMessage raftMessage) {
        updateTerm(raftMessage);

        if (!(raftMessage instanceof RaftMessage.HeartBeatRaftMessage)) {
            log.info("{} handling {} from {}", id, getMessageType(raftMessage), raftMessage.getSrc());
        }

        switch (raftMessage) {
            case RaftMessage.RequestVoteRaftMessage requestVoteRaftMessage -> handleRequestVote(requestVoteRaftMessage);
            case RaftMessage.AppendEntriesRaftMessage appendEntriesRaftMessage ->
                    handleAppendEntries(appendEntriesRaftMessage);
            case RaftMessage.ElectionTimeoutElapsedRaftMessage ignored ->
                    convertToCandidate();
            case RaftMessage.RequestVoteResultRaftMessage requestVoteResultRaftMessage ->
                    handleRequestVoteResult(requestVoteResultRaftMessage);
            case RaftMessage.ClientRequestRaftMessage clientRequestRaftMessage ->
                    handleClientRequest(clientRequestRaftMessage);
            case RaftMessage.AppendEntriesResultRaftMessage appendEntriesResultRaftMessage ->
                    handleAppendEntriesResult(appendEntriesResultRaftMessage);
            case RaftMessage.HeartBeatRaftMessage heartBeatRaftMessage -> handleHeartBeat(heartBeatRaftMessage);
            default -> {
            }
        }
    }

    private String getMessageType(RaftMessage message) {
        if (message instanceof RaftMessage.RequestVoteRaftMessage) return "REQUEST_VOTE";
        if (message instanceof RaftMessage.AppendEntriesRaftMessage) return "APPEND_ENTRIES";
        if (message instanceof RaftMessage.ElectionTimeoutElapsedRaftMessage) return "ELECTION_TIMEOUT_ELAPSED";
        if (message instanceof RaftMessage.RequestVoteResultRaftMessage) return "REQUEST_VOTE_RESULT";
        if (message instanceof RaftMessage.ClientRequestRaftMessage) return "CLIENT_REQUEST";
        if (message instanceof RaftMessage.AppendEntriesResultRaftMessage) return "APPEND_ENTRIES_RESULT";
        if (message instanceof RaftMessage.HeartBeatRaftMessage) return "HEARTBEAT";
        return "UNKNOWN";
    }

    private void handleHeartBeat(RaftMessage.HeartBeatRaftMessage raftMessage) {
        electionTimer.resetTimer();
        if (raftMessage.getTerm() >= currentTerm) {
            votedFor = raftMessage.getSrc();
            if (!currentRole.equals(RaftRole.FOLLOWER)) {
                convertToFollower();
            }
        }
    }

    private void handleAppendEntriesResult(RaftMessage.AppendEntriesResultRaftMessage raftMessage) {
        if (!currentRole.equals(RaftRole.LEADER)) {
            return;
        }

        var appendEntriesResult = raftMessage.getAppendEntriesResult();
        if (!appendEntriesResult.isSuccess()) {
            nextIndex.compute(raftMessage.getSrc(), (k, prevNextIndex) -> prevNextIndex - 1);
        } else {
            matchIndex.put(raftMessage.getSrc(), nextIndex.get(raftMessage.getSrc()));
            nextIndex.compute(raftMessage.getSrc(), (k, prevNextIndex) -> prevNextIndex + 1);
        }
        maxSentLogs.remove(raftMessage.getSrc());
    }

    private void handleClientRequest(RaftMessage.ClientRequestRaftMessage raftMessage) {
        var clientRequest = raftMessage.getClientRequest();
        var requestId = UUID.fromString(clientRequest.getRequestId());
        var future = awaitingClientRequests.remove(requestId);
        var response = new ClientResponse(null, null);

        if (!currentRole.equals(RaftRole.LEADER)) {
            if (Objects.nonNull(votedFor)) {
                response = new ClientResponse(null, votedFor);
            }
            future.complete(response);
            log.info("Not a leader, skipping client request");
            return;
        }

        var logEntry = new LogEntry(clientRequest.getClientRequest().getRequest(), currentTerm);
        var selectedIndex = lastIndex + 1;
        lastIndex = lastIndex + 1;

        inFlightClientRequests.put(selectedIndex, future);
        logEntries.put(selectedIndex, logEntry);
        durableStateStore.addLog(selectedIndex, logEntry);
    }

    private void handleRequestVoteResult(RaftMessage.RequestVoteResultRaftMessage raftMessage) {
        if (!currentRole.equals(RaftRole.CANDIDATE)) {
            return;
        }

        var requestVoteResult = raftMessage.getRequestVoteResult();

        if (requestVoteResult.isVoteGranted()) {
            currentElectionVoters.add(raftMessage.getSrc());
        }

        if (currentElectionVoters.size() >= quorum()) {
            convertToLeader();
        }
    }

    private void handleAppendEntries(RaftMessage.AppendEntriesRaftMessage raftMessage) {
        if (!currentRole.equals(RaftRole.FOLLOWER)) {
            return;
        }

        var appendEntries = raftMessage.getAppendEntries();
        var response = RaftMessage.createAppendEntriesResult(id, currentTerm, new AppendEntriesRPCResult(false));

        if (raftMessage.getTerm() < currentTerm) {
            log.info("Ignoring append entries because term is too old: {} < {}", raftMessage.getTerm(), currentTerm);
            sendRequestToOtherServer(response, raftMessage.getSrc());
            return;
        }

        var prevLogCheck = appendEntries.getPrevLogIndex() != null && appendEntries.getPrevLogTerm() != null;

        if (prevLogCheck) {
            var prevLog = logEntries.get(appendEntries.getPrevLogIndex());
            if (Objects.isNull(prevLog) || prevLog.getTerm() != appendEntries.getPrevLogTerm()) {
                log.info("Ignoring append entries because prev log by index {} term {} not found", appendEntries.getPrevLogIndex(), appendEntries.getPrevLogTerm());
                sendRequestToOtherServer(response, raftMessage.getSrc());
                return;
            }
        }

        var entriesIndex = appendEntries.getPrevLogIndex() != null ? appendEntries.getPrevLogIndex() + 1 : 1;
        for (var e : appendEntries.getEntries()) {
            logEntries.put(entriesIndex, e);
            durableStateStore.addLog(entriesIndex, e);
            entriesIndex++;
        }
        lastIndex = entriesIndex - 1;

        if (appendEntries.getLeaderCommit() > commitIndex) {
            commitIndex = Math.min(appendEntries.getLeaderCommit(), lastIndex);
        }

        var successResponse = RaftMessage.createAppendEntriesResult(id, currentTerm, new AppendEntriesRPCResult(true));
        sendRequestToOtherServer(successResponse, raftMessage.getSrc());
    }

    private void handleRequestVote(RaftMessage.RequestVoteRaftMessage raftMessage) {
        if (!currentRole.equals(RaftRole.FOLLOWER)) {
            return;
        }

        var requestVote = raftMessage.getRequestVote();
        var response = RaftMessage.createRequestVoteResult(id, currentTerm, new RequestVoteRPCResult(false));

        if (raftMessage.getTerm() < currentTerm) {
            sendRequestToOtherServer(response, raftMessage.getSrc());
            return;
        }

        if (Objects.nonNull(votedFor) && !votedFor.equals(requestVote.getCandidateId())) {
            log.info("{} already voted for {}", id, votedFor);
            sendRequestToOtherServer(response, raftMessage.getSrc());
            return;
        }

        var lastTermIndex = lastIndex == 0 ? null : new TermIndex(logEntries.get(lastIndex).getTerm(), lastIndex);
        if (Objects.nonNull(lastTermIndex)
                && lastTermIndex.compareTo(new TermIndex(requestVote.getLastLogTerm(), requestVote.getLastLogIndex())) > 0) {
            sendRequestToOtherServer(response, raftMessage.getSrc());
            return;
        }
        electionTimer.resetTimer();

        votedFor = raftMessage.getSrc();
        durableStateStore.saveVotedFor(raftMessage.getSrc());
        currentRole = RaftRole.FOLLOWER;

        var successResponse = RaftMessage.createRequestVoteResult(id, currentTerm, new RequestVoteRPCResult(true));
        sendRequestToOtherServer(successResponse, raftMessage.getSrc());
    }

    private void applyLogs() {
        while (commitIndex > lastApplied) {
            log.info("Applying logs: commitIndex={}, lastApplied={}, requests: {}", commitIndex, lastApplied, inFlightClientRequests);
            lastApplied++;
            var result = stateMachine.applyCommand(logEntries.get(lastApplied).getCommand().getValue());

            if (!currentRole.equals(RaftRole.LEADER)) {
                continue;
            }
            var request = inFlightClientRequests.remove(lastApplied);

            if (Objects.isNull(request)) {
                continue;
            }

            var resultResponse = new ClientResponse(new RaftCommandResult(result), null);
            request.complete(resultResponse);
        }
    }

    private void updateTerm(RaftMessage raftMessage) {
        if (raftMessage.getTerm() == null) {
            return;
        }

        if (currentTerm < raftMessage.getTerm()) {
            currentTerm = raftMessage.getTerm();
            durableStateStore.saveCurrentTerm(currentTerm);
            convertToFollower();
        }
    }

    private void convertToLeader() {
        log.info("{} -> {}", id, RaftRole.LEADER);
        sendHeartBeats();

        var maxLogIndex = logEntries.keySet()
                .stream()
                .max(Comparator.naturalOrder());

        matchIndex.clear();
        nextIndex.clear();
        maxSentLogs.clear();

        for (var r : replicas) {
            if (!r.equals(id)) {
                nextIndex.put(r, maxLogIndex.orElse(1L));
                matchIndex.put(r, 0L);
            }
        }

        currentRole = RaftRole.LEADER;
    }

    private void convertToFollower() {
        log.info("{} -> {}", id, RaftRole.FOLLOWER);
        electionTimer.resetTimer();
        votedFor = null;

        currentRole = RaftRole.FOLLOWER;
    }

    private void convertToCandidate() {
        log.info("{} -> {}", id, RaftRole.CANDIDATE);
        electionTimer.resetTimer();

        currentRole = RaftRole.CANDIDATE;

        currentTerm++;
        durableStateStore.saveCurrentTerm(currentTerm);
        currentElectionVoters.clear();

        votedFor = id;
        currentElectionVoters.add(id);

        var requestVoteMessage = RaftMessage.createRequestVote(id, currentTerm, 
                new RequestVoteRPC(id, lastIndex, lastIndex == 0 ? 0 : logEntries.get(lastIndex).getTerm()));

        replicas.stream()
                .filter((r) -> !r.equals(id))
                .forEach((r) -> sendRequestToOtherServer(requestVoteMessage, r));
    }

    @SneakyThrows
    private CompletableFuture<byte[]> handleMessage(byte[] message) {
        var parsedMessage = JacksonObjectMapper.deserialize(message, RaftServerRequest.class);

        if (parsedMessage instanceof RaftServerRequest.ClientRequestRaftServerRequest clientRequestRaftServerRequest) {
            var requestId = UUID.randomUUID();

            var future = new CompletableFuture<ClientResponse>();
            awaitingClientRequests.put(requestId, future);

            messagesQueue.put(
                    RaftMessage.createClientRequest(id, currentTerm, 
                            new InternalClientRequest(clientRequestRaftServerRequest.getClientRequest(), requestId.toString()))
            );

            return future.thenApply(JacksonObjectMapper::serialize);
        } else if (parsedMessage instanceof RaftServerRequest.RaftMessageRaftServerRequest) {
            var raftMessageRaftServerRequest = (RaftServerRequest.RaftMessageRaftServerRequest) parsedMessage;
            messagesQueue.put(raftMessageRaftServerRequest.getRaftMessage());

            return CompletableFuture.completedFuture(new byte[0]);
        } else {
            return CompletableFuture.completedFuture(new byte[0]);
        }
    }

    private void updateFollowers() {
        if (!currentRole.equals(RaftRole.LEADER)) {
            return;
        }

        for (var r : replicas) {
            if (r.equals(id) || maxSentLogs.getOrDefault(r, 0L) >= nextIndex.get(r)) {
                continue;
            }
            if (nextIndex.get(r) > lastIndex) {
                continue;
            }

            var prevLogIndex = nextIndex.get(r) - 1;
            var prevLog = logEntries.get(prevLogIndex);
            Long prevLogTerm = null;
            if (Objects.nonNull(prevLog)) {
                prevLogTerm = prevLog.getTerm();
            }

            var appendEntries = new AppendEntriesRPC(id, prevLogIndex, prevLogTerm, 
                    List.of(logEntries.get(nextIndex.get(r))), commitIndex);
            var message = RaftMessage.createAppendEntries(id, currentTerm, appendEntries);

            maxSentLogs.put(r, nextIndex.get(r));

            sendRequestToOtherServer(message, r);
        }
    }

    private class ServerThread implements Runnable {

        @Override
        public void run() {
            try {
                while (true) {
                    handleMessage(messagesQueue.take());
                    updateFollowers();
                    updateCommitIndex();
                    applyLogs();
                }
            } catch (Exception e) {
                log.error("Message poller failed", e);
            }
        }
    }

    private void updateCommitIndex() {
        if (!currentRole.equals(RaftRole.LEADER)) {
            return;
        }

        var minCommit = commitIndex;
        for (var v : matchIndex.values()) {
            if (v > minCommit && matchIndex.values().stream().filter((it) -> it >= v).count() >= (replicas.size() / 2)) {
                minCommit = v;
            }
        }

        log.info("Updating min commit with {}", minCommit);
        if (minCommit == 0 || logEntries.get(minCommit).getTerm() != currentTerm) {
            return;
        }

        commitIndex = Math.max(commitIndex, minCommit);
    }

    private class ElectionTimerChecker implements Runnable {
        private final Duration tickDuration = Duration.ofMillis(25);

        @Override
        public void run() {
            while (true) {
                try {
                    if (currentRole.equals(RaftRole.LEADER)) {
                        continue;
                    }

                    if (electionTimer.isTimeout()) {
                        var message = RaftMessage.createElectionTimeoutElapsed();

                        messagesQueue.put(message);
                        electionTimer.deactivateTimer();
                    }

                    Thread.sleep(tickDuration);
                } catch (Exception e) {
                    log.error("Timer failed", e);
                    return;
                }
            }
        }
    }

    private class HeartbeatSender implements Runnable {
        private final Duration frequency;

        private HeartbeatSender(Duration frequency) {
            this.frequency = frequency;
        }

        @Override
        public void run() {
            while (true) {
                try {
                    if (!currentRole.equals(RaftRole.LEADER)) {
                        continue;
                    }

                    sendHeartBeats();

                    Thread.sleep(frequency);
                } catch (Exception e) {
                    log.error("Heartbeat sender failed", e);
                    return;
                }
            }
        }
    }

    void sendHeartBeats() {
        var message = RaftMessage.createHeartBeat(id, currentTerm);
        replicas.stream()
                .filter((r) -> !r.equals(id))
                .forEach((r) -> sendRequestToOtherServer(message, r));
    }

    private void sendRequestToOtherServer(RaftMessage raftMessage, String serverId) {
        var serverRequest = RaftServerRequest.createRaftMessage(raftMessage);
        environmentClient.sendMessage(JacksonObjectMapper.serialize(serverRequest), serverId);
    }

    private int quorum() {
        return replicas.size() / 2 + 1;
    }
}
