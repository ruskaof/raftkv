package ru.itmo.rusinov.consensus.raft.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class SetMessage {
    private final byte[] key;
    private final byte[] value;

    @JsonCreator
    public SetMessage(@JsonProperty("key") byte[] key, @JsonProperty("value") byte[] value) {
        this.key = key;
        this.value = value;
    }
}
