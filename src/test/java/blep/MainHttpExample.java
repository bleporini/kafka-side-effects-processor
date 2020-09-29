package blep;

import blep.SimpleHttpRequestSerde.SimpleHttpRequestSerializer;
import blep.WithKafkaConfiguration.WithLocalUnsecureKafka;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.StringSerializer;

import java.util.Properties;
import java.util.concurrent.ExecutionException;

import static blep.SimpleHttpRequest.Method.GET;

public class MainHttpExample extends SimpleHttpProcessor implements WithLocalUnsecureKafka {
    private final Properties conf = configuration();

    public MainHttpExample(String requestTopicName, String responseTopicName, String rejectionTopicName) throws ExecutionException, InterruptedException {
        super(requestTopicName, responseTopicName, rejectionTopicName, RetryPolicy.backoff(5000));
        KafkaProducer<String, byte[]> producer = new KafkaProducer<>(conf, new StringSerializer(),new ByteArraySerializer());
        producer.send(
                new ProducerRecord<>(
                        requestTopicName,
                        "key",
                        new SimpleHttpRequestSerializer()
                                .serialize(
                                        requestTopicName,
                                        Retryable.init(
                                                SimpleHttpRequest.builder()
                                                        .method(GET)
                                                        .url("https://www.google.fr/test")
                                                        .build(),
                                                3
                                        )
                                )
//                        tryOnceSerializer.serialize("requests",Retryable.init("Test", 3))
                )
        ).get();
    }

    public static void main(String[] args) throws ExecutionException, InterruptedException {
        new MainHttpExample(
                "httpRequests",
                "httpResponses",
                "httpRejections"
        ).start();
    }
}
