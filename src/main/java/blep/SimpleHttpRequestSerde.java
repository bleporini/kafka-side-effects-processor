package blep;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serializer;

public class SimpleHttpRequestSerde implements Serde<Retryable<SimpleHttpRequest>> {
    private static final ObjectMapper mapper = new ObjectMapper();


    @Override
    public Serializer<Retryable<SimpleHttpRequest>> serializer() {
        return new SimpleHttpRequestSerializer();
    }

    @Override
    public Deserializer<Retryable<SimpleHttpRequest>> deserializer() {
        return new SimpleHttpRequestDeserializer();
    }

    public static class SimpleHttpRequestDeserializer implements Deserializer<Retryable<SimpleHttpRequest>>, WithRuntimeExceptionWrapper {

        @Override
        public Retryable<SimpleHttpRequest> deserialize(String topic, byte[] data) {
            JavaType javaType = mapper.getTypeFactory().constructParametricType(Retryable.class, SimpleHttpRequest.class);

            return toUnchecked(() -> mapper.readValue(data, javaType));

        }
    }

    public static class SimpleHttpRequestSerializer implements Serializer<Retryable<SimpleHttpRequest>>, WithRuntimeExceptionWrapper{

        @Override
        public byte[] serialize(String topic, Retryable<SimpleHttpRequest> data) {
            return toUnchecked(() -> mapper.writeValueAsBytes(data));
        }
    }
}
