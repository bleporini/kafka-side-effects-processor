package blep;

import blep.Tryable.Retryable;
import blep.Tryable.TryOnce;
import blep.WithKafkaConfiguration.WithLocalUnsecureKafka;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.vavr.concurrent.Future;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.*;

import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;

import static blep.Tryable.Status.TO_TRY;

public class MainExample implements WithLocalUnsecureKafka, WithRuntimeExceptionWrapper {

    public static void main(String[] args) throws ExecutionException, InterruptedException {
        new MainExample();

    }

    private Properties conf = configuration();
    private static final ObjectMapper mapper = new ObjectMapper();

    public static class TryOnceDeserializer implements Deserializer<Tryable<String>>, WithRuntimeExceptionWrapper{

        @Override
        public Tryable<String> deserialize(String topic, byte[] data) {
            JavaType type = mapper.getTypeFactory().constructParametricType(Tryable.class, String.class);
            return toUnchecked(() ->
                    mapper.readValue(data, type)
            );
        }
    }

    public static class TryOnceSerializer implements Serializer<Tryable<String>>, WithRuntimeExceptionWrapper{

        @Override
        public byte[] serialize(String topic, Tryable<String> data) {
            return toUnchecked(() ->
                    mapper.writeValueAsBytes(data)
            );
        }
    }



    public MainExample() throws ExecutionException, InterruptedException {
        KafkaProducer<String, byte[]> producer = new KafkaProducer<>(conf, new StringSerializer(),new ByteArraySerializer());

        Deserializer<Tryable<String>>tryableDeserializer = new TryOnceDeserializer() ;

        var tryOnceSerializer = new TryOnceSerializer() ;
        var consumer = new KafkaConsumer<>(
                conf,
                new StringDeserializer(),
                tryableDeserializer
        );

        producer.send(
                new ProducerRecord<>(
                        "requests",
                        "key",
                        tryOnceSerializer.serialize("requests", new TryOnce<>("test", true, TO_TRY))
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
                "rejections"
        ).start();

    }
}
