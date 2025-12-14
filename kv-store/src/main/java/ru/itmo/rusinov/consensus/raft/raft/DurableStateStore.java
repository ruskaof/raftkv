package ru.itmo.rusinov.consensus.raft.raft;

import ru.itmo.rusinov.consensus.raft.model.raft.LogEntry;

import java.io.File;
import java.util.Map;
import java.util.Optional;

public interface DurableStateStore {
    void initialize(File storagePath);

    void saveCurrentTerm(long currentTerm);
    Optional<Long> loadCurrentTerm();

    void saveVotedFor(String votedFor);
    Optional<String> loadVotedFor();

    void addLog(Long index, LogEntry logEntry);
    Map<Long, LogEntry> loadLog();
}
