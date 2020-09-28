package blep;

import blep.SideEffectProcessor.AsyncPayloadProcessor;
import blep.SideEffectProcessor.ReturnedValueChecker;
import io.vavr.concurrent.Future;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.Serializer;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.stream.StreamSupport;

import static blep.Retryable.Status.TO_TRY;
import static io.vavr.concurrent.Future.fromJavaFuture;

public class KafkaSideEffectProcessor<K,P,V> {

    private final KafkaConsumer<K, Retryable<P>> requestConsumer;
    private final KafkaProducer<K, byte[]> responseProducer;
    private final Serializer<Retryable<P>> payloadSerializer;
    private final Serializer<V> returnedValueSerializer;
    private final ReturnedValueChecker<K,P,V> valueChecker;
    private final AsyncPayloadProcessor<P, V> asyncProcessor;
    private final SideEffectProcessor<K, P, V> sideEffectProcessor;
    private final String requestTopicName;
    private final String responseTopicName;
    private final String rejectionTopicName;



    public KafkaSideEffectProcessor(
            KafkaConsumer<K, Retryable<P>> requestConsumer,
            KafkaProducer<K, byte[]> responseProducer,
            Serializer<Retryable<P>> payloadSerializer,
            Serializer<V> returnedValueSerializer,
            ReturnedValueChecker<K,P,V> valueChecker,
            AsyncPayloadProcessor<P, V> asyncProcessor,
            String requestTopicName,
            String responseTopicName,
            String rejectionTopicName) {

        this.requestConsumer = requestConsumer;
        this.responseProducer = responseProducer;
        this.payloadSerializer = payloadSerializer;
        this.returnedValueSerializer = returnedValueSerializer;
        this.valueChecker = valueChecker;
        this.asyncProcessor = asyncProcessor;
        this.requestTopicName = requestTopicName;
        this.responseTopicName = responseTopicName;
        this.rejectionTopicName = rejectionTopicName;

        sideEffectProcessor = new SideEffectProcessor<K,P,V>(
                asyncProcessor,
                this::sendSuccess,
                this::sendFailure,
                this::sendReject,
                valueChecker
        );
    }

    public void start() {
        requestConsumer.subscribe(List.of(requestTopicName));

        while (true) {
            ConsumerRecords<K, Retryable<P>> requests = requestConsumer.poll(Duration.ofMillis(100));

            StreamSupport.stream(requests.spliterator(), false)
                    .filter(r ->
                            TO_TRY == r.value().getStatus()
                    ).forEach(r ->
                        sideEffectProcessor.process(r.key(), r.value())
                    );

        }
    }

    private Future<Object> sendReject(K k, Retryable<P> pRetryable) {
        return fromJavaFuture(
                responseProducer.send(
                        new ProducerRecord<>(
                                rejectionTopicName,
                                k,
                                payloadSerializer.serialize(rejectionTopicName, pRetryable)
                        )
                )

        ).map(r -> new Object()); // TODO: check if there's not a more fancy way to comply with the returned type
    }

    private Future<Object> sendFailure(K k, Retryable<P> pRetryable) {
        return fromJavaFuture(
                responseProducer.send(
                        new ProducerRecord<>(
                                requestTopicName,
                                k,
                                payloadSerializer.serialize(requestTopicName, pRetryable)
                        )
                )
        ).map(r -> new Object()); // TODO: check if there's not a more fancy way to comply with the returned type

    }

    private Future<Object> sendSuccess(K k, Retryable<P> pRetryable, V v) {
        return Future.reduce(
                Arrays.asList(
                        fromJavaFuture(
                                responseProducer.send(
                                        new ProducerRecord<>(
                                                requestTopicName,
                                                k,
                                                payloadSerializer.serialize(requestTopicName, pRetryable)
                                        )
                                )
                        ),
                        fromJavaFuture(
                                responseProducer.send(
                                        new ProducerRecord<>(
                                                responseTopicName,
                                                k,
                                                returnedValueSerializer.serialize(requestTopicName, v)
                                        )
                                )
                        )
                ),
                (f1, f2) -> f2
        );
    }
}
