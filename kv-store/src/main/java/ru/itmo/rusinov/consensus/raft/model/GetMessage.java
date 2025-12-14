package ru.itmo.rusinov.consensus.raft.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class GetMessage {
    private final byte[] key;

    @JsonCreator
    public GetMessage(@JsonProperty("key") byte[] key) {
        this.key = key;
    }
}
