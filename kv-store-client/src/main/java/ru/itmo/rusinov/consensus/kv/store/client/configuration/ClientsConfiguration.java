package ru.itmo.rusinov.consensus.kv.store.client.configuration;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.itmo.rusinov.consensus.kv.store.client.raft.RaftClient;
import ru.itmo.rusinov.consensus.raft.network.SimpleEnvironmentClient;

import java.util.stream.Collectors;

@Configuration
@EnableConfigurationProperties(PeerConfigurationProperties.class)
public class ClientsConfiguration {


    @Bean
    public RaftClient raftClient(PeerConfigurationProperties peerConfigurationProperties) {
        var destinations = peerConfigurationProperties.peers()
                .stream()
                .collect(Collectors.toMap(
                        PeerConfigurationProperties.PeerConfiguration::id,
                        PeerConfigurationProperties.PeerConfiguration::address
                ));

        var envClient = new SimpleEnvironmentClient(destinations, 100000);
        envClient.initialize();

        return new RaftClient(destinations.keySet().stream().toList(), envClient);
    }
}
