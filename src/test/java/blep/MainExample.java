package blep;

import blep.WithKafkaConfiguration.WithLocalUnsecureKafka;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.vavr.concurrent.Future;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.*;

import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;

public class MainExample implements WithLocalUnsecureKafka, WithRuntimeExceptionWrapper {

    public static void main(String[] args) throws ExecutionException, InterruptedException {
        new MainExample();

    }

    private Properties conf = configuration();
    private static final ObjectMapper mapper = new ObjectMapper();

    public static class StringRetryableDeserializer implements Deserializer<Retryable<String>>, WithRuntimeExceptionWrapper{

        @Override
        public Retryable<String> deserialize(String topic, byte[] data) {
            JavaType type = mapper.getTypeFactory().constructParametricType(Retryable.class, String.class);
            return toUnchecked(() ->
                    mapper.readValue(data, type)
            );
        }
    }

    public static class StringRetryableSerializer implements Serializer<Retryable<String>>, WithRuntimeExceptionWrapper{

        @Override
        public byte[] serialize(String topic, Retryable<String> data) {
            return toUnchecked(() ->
                    mapper.writeValueAsBytes(data)
            );
        }
    }



    public MainExample() throws ExecutionException, InterruptedException {
        KafkaProducer<String, byte[]> producer = new KafkaProducer<>(conf, new StringSerializer(),new ByteArraySerializer());

        Deserializer<Retryable<String>>retryableDeserializer = new StringRetryableDeserializer() ;

        var tryOnceSerializer = new StringRetryableSerializer() ;
        var consumer = new KafkaConsumer<>(
                conf,
                new StringDeserializer(),
                retryableDeserializer
        );

        producer.send(
                new ProducerRecord<>(
                        "requests",
                        "key",
                        tryOnceSerializer.serialize("requests", Retryable.init("test", 1))
//                        tryOnceSerializer.serialize("requests",Retryable.init("Test", 3))
                )
        ).get();

        AtomicBoolean willSucceed = new AtomicBoolean(false);
        new KafkaSideEffectProcessor<>(
                consumer,
                producer,
                tryOnceSerializer,
                new StringSerializer(),
                (id, triable, v) -> willSucceed.getAndSet(! willSucceed.get()),
                p -> Future.successful("OK " + p),
                "requests",
                "responses",
                "rejections",
                RetryPolicy.immediate()
        ).start();

    }
}
