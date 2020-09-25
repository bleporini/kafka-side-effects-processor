package blep;

import org.apache.kafka.common.serialization.StringSerializer;

import java.util.Properties;

public interface WithKafkaConfiguration {

    Properties loadProperties();

    default Properties configuration() {
        return postConfigure(loadProperties());
    }

    default Properties postConfigure(Properties properties) {
        return properties;
    }


    interface WithLocalUnsecureKafka extends WithKafkaConfiguration{
        @Override
        default Properties loadProperties(){
            Properties config = new Properties();
            config.put("group.id", groupName());
            config.put("bootstrap.servers", "localhost:9092");
            config.put("key.serializer", StringSerializer.class.getName());
            config.put("value.serializer", StringSerializer.class.getName());
            config.put("acks", "all");

            return config;
        }

        default String groupName(){
            return "default-group-name";
        }
    }



}
