package ru.itmo.rusinov.consensus.raft.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;

public class JacksonObjectMapper {
    private static final ObjectMapper INSTANCE = JsonMapper.builder()
            .findAndAddModules()
            .build();

    public static ObjectMapper getInstance() {
        return INSTANCE;
    }

    public static byte[] serialize(Object obj) {
        try {
            return INSTANCE.writeValueAsBytes(obj);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize object", e);
        }
    }

    public static <T> T deserialize(byte[] bytes, Class<T> clazz) {
        try {
            return INSTANCE.readValue(bytes, clazz);
        } catch (Exception e) {
            throw new RuntimeException("Failed to deserialize object", e);
        }
    }
}
