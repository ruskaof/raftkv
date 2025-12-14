package ru.itmo.rusinov.consensus.raft.model;

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
    @JsonSubTypes.Type(value = GetMessage.class, name = "get"),
    @JsonSubTypes.Type(value = SetMessage.class, name = "set")
})
public abstract class KvStoreProtoMessage {
    private final String type;

    @JsonCreator
    protected KvStoreProtoMessage(@JsonProperty("type") String type) {
        this.type = type;
    }

    public static KvStoreProtoMessage createGet(GetMessage get) {
        return new GetKvStoreProtoMessage(get);
    }

    public static KvStoreProtoMessage createSet(SetMessage set) {
        return new SetKvStoreProtoMessage(set);
    }

    @Data
    public static class GetKvStoreProtoMessage extends KvStoreProtoMessage {
        private final GetMessage get;

        @JsonCreator
        public GetKvStoreProtoMessage(@JsonProperty("get") GetMessage get) {
            super("get");
            this.get = get;
        }
    }

    @Data
    public static class SetKvStoreProtoMessage extends KvStoreProtoMessage {
        private final SetMessage set;

        @JsonCreator
        public SetKvStoreProtoMessage(@JsonProperty("set") SetMessage set) {
            super("set");
            this.set = set;
        }
    }
}
