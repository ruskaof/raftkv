package ru.itmo.rusinov.consensus.raft.store;

import lombok.extern.slf4j.Slf4j;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.HTreeMap;
import org.mapdb.Serializer;

import java.nio.file.Path;

@Slf4j
public class MapDbKvDatabase implements KvDatabase {

    private DB db;
    private HTreeMap<byte[], byte[]> map;

    @Override
    public void initialize(Path databaseFilePath) {
        log.info("Initializing mapDB storage on path {}", databaseFilePath);
        db = DBMaker.fileDB(databaseFilePath.toFile())
                .fileMmapEnable()
                .transactionEnable()
                .make();

        map = db.hashMap("myMap")
                .keySerializer(Serializer.BYTE_ARRAY)
                .valueSerializer(Serializer.BYTE_ARRAY)
                .createOrOpen();
    }

    @Override
    public byte[] get(byte[] key) {
        // Retrieve the value associated with the key
        return map.get(key);
    }

    @Override
    public void put(byte[] key, byte[] value) {
        map.put(key, value);
        db.commit();
    }

    public void close() {
        if (db != null && !db.isClosed()) {
            db.close();
        }
    }
}