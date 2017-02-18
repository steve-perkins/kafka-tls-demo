package com.steveperkins.tls;

import com.bettercloud.vault.api.pki.Credential;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;

import java.util.Date;
import java.util.Properties;

/**
 * TODO: Document
 */
public class ProducerApp {

    public static void main(String[] args) throws Exception {
        final Credential credential = TlsUtils.credentials("kafka-producer");
        TlsUtils.setupStores(credential.getCertificate(), credential.getPrivateKey(), credential.getIssuingCa(), "producer");

        final Properties props = new Properties();
        props.put("bootstrap.servers", "127.0.0.1:9093");
        props.put("acks", "all");
        props.put("retries", 0);
        props.put("batch.size", 16384);
        props.put("linger.ms", 1);
        props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");

        props.put("security.protocol", "SSL");
        props.put("ssl.keystore.location", "producer-keystore.jks");
        props.put("ssl.keystore.password", "password123");
        props.put("ssl.truststore.location", "producer-truststore.jks");
        props.put("ssl.truststore.password", "password123");

        try (final Producer<String, String> producer = new KafkaProducer<>(props)) {
            while (true) {
                final String date = new Date().toString();
                System.out.println("Sending message: " + date);
                producer.send(new ProducerRecord<>("test", "date", date));
                Thread.sleep(2000);
            }
        }
    }

}
