package com.steveperkins.tls;

import com.bettercloud.vault.api.pki.Credential;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;

import java.util.Arrays;
import java.util.Properties;

/**
 * TODO: Document
 */
public class ConsumerApp {

    /**
     * TODO: Document
     *
     * @param args
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {
        final Credential credential = TlsUtils.credentials("kafka-consumer");
        TlsUtils.setupStores(credential.getCertificate(), credential.getPrivateKey(), credential.getIssuingCa(), "consumer");

        final Properties props = new Properties();
        props.put("bootstrap.servers", "127.0.0.1:9093");
        props.put("group.id", "sample-consumer");
        props.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        props.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");

        props.put("security.protocol", "SSL");
        props.put("ssl.keystore.location", "consumer-keystore.jks");
        props.put("ssl.keystore.password", "password123");
        props.put("ssl.truststore.location", "consumer-truststore.jks");
        props.put("ssl.truststore.password", "password123");

        try (final KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props)) {
            consumer.subscribe(Arrays.asList("test"));
            while (true) {
                final ConsumerRecords<String, String> records = consumer.poll(1000);
                for (final ConsumerRecord<String, String> record : records) {
                    System.out.println("Receiving message: " + record.value());
                }
            }
        }
    }


}
