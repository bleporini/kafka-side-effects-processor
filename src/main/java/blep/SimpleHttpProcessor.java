package blep;

import blep.SideEffectProcessor.AsyncPayloadProcessor;
import blep.SimpleHttpRequestSerde.SimpleHttpRequestDeserializer;
import blep.SimpleHttpRequestSerde.SimpleHttpRequestSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.vavr.concurrent.Future;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.Serializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;

import java.net.http.HttpClient;
import java.net.http.HttpResponse;
import java.util.Properties;

@Slf4j
public abstract class SimpleHttpProcessor implements WithKafkaConfiguration{
    private final Properties conf = configuration();
    private final KafkaSideEffectProcessor<String, SimpleHttpRequest, HttpResponse<String>> processor;

    public SimpleHttpProcessor(
            String requestTopicName,
            String responseTopicName,
            String rejectionTopicName,
            RetryPolicy<SimpleHttpRequest, HttpResponse<String>> policy) {

        KafkaProducer<String, byte[]> producer = new KafkaProducer<>(conf, new StringSerializer(),new ByteArraySerializer());

        var consumer = new KafkaConsumer<>(
                conf,
                new StringDeserializer(),
                new SimpleHttpRequestDeserializer()
        );

        processor = new KafkaSideEffectProcessor<>(
                consumer,
                producer,
                new SimpleHttpRequestSerializer(),
                new HttpResponseSerializer(),
                (id, triable, v) -> {
                    if(v.statusCode() % 100 == 2)
                        return true;
                    else {
                        log.error("Processing request #{} returned HTTP Status code {} --> Failed", id, v.statusCode());
                        return false;
                    }
                },
                new HttpPayloadProcessor(),
                requestTopicName,
                responseTopicName,
                rejectionTopicName,
                policy
        );
    }

    public static class HttpResponseSerializer implements Serializer<HttpResponse<String>>, WithRuntimeExceptionWrapper{
        private static ObjectMapper mapper = new ObjectMapper();
        @Override
        public byte[] serialize(String topic, HttpResponse<String> data) {
            return toUnchecked(() ->
                    mapper.writeValueAsBytes(
                            SimpleStringResponse.builder()
                                    .status(data.statusCode())
                                    .body(data.body())
                                    .build()
                    )
            );
        }
    }

    @AllArgsConstructor
    @Getter
    @Builder
    public static class SimpleStringResponse{
        private final int status;
        private final String body;
    }

    public static class HttpPayloadProcessor implements AsyncPayloadProcessor<SimpleHttpRequest, HttpResponse<String>> {
        private static final HttpClient client = HttpClient.newHttpClient();
        @Override
        public Future<HttpResponse<String>> process(SimpleHttpRequest payload) {
            return Future.fromJavaFuture(
                    client.sendAsync(
                            payload.asJavaHttpRequest(),
                            HttpResponse.BodyHandlers.ofString()
                    )
            );
        }
    }

    public void start() {
        processor.start();
    }
}
