package ro.mbe.learn.kafka.producers;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ro.mbe.learn.kafka.commons.Constants;
import ro.mbe.learn.kafka.commons.KafkaConfig;
import ro.mbe.learn.kafka.commons.Setup;

import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

public class SimpleProducerApp {

    private static final Logger LOGGER = LoggerFactory.getLogger(SimpleProducerApp.class);

    public static void main(String[] args) {

        String clientId = (args != null && args.length > 0 && args[0].length() > 0)
                ? args[0]
                : UUID.randomUUID().toString();

        Properties properties = getProducerProperties(clientId);

        try (KafkaProducer<String, String> producer = new KafkaProducer<>(properties)) {
            for (int index = 0; index < Constants.NoOfRecordsToSend; index ++) {
                for (Map.Entry<String, List<Integer>> entry : Setup.TopicsAndPartitions.entrySet()) {

                    int noOfPartitions = entry.getValue().size();

                    String topic = entry.getKey();
                    Integer partition = index % noOfPartitions;
                    String key = UUID.randomUUID().toString();
                    String value = UUID.randomUUID().toString();

                    ProducerRecord<String, String> record = (noOfPartitions == 1)
                            ? new ProducerRecord<>(topic, key, value)
                            : new ProducerRecord<>(topic, partition, key, value);

                    producer.send(record);
                }
            }
        } catch (Exception error) {
            LOGGER.error(error.getMessage(), error);
        }

        System.out.println("DONE!");
    }

    private static Properties getProducerProperties(String clientId) {

        Properties properties = new Properties();

        /**
         * A list of host/port pairs to use for establishing the initial connection to the Kafka cluster. The client will
         * make use of all servers irrespective of which servers are specified here for bootstrapping—this list only
         * impacts the initial hosts used to discover the full set of servers. This list should be in the form
         * host1:port1,host2:port2,.... Since these servers are just used for the initial connection to discover the full
         * cluster membership (which may change dynamically), this list need not contain the full set of servers (you
         * may want more than one, though, in case a server is down).
         */
        properties.put(KafkaConfig.Producer.BOOTSTRAP_SERVERS, String.join(", ", Setup.KafkaServers));

        /**
         * Serializer class for key that implements the Serializer interface.
         */
        properties.put(KafkaConfig.Producer.KEY_SERIALIZER, StringSerializer.class.getName());

        /**
         * Serializer class for value that implements the Serializer interface.
         */
        properties.put(KafkaConfig.Producer.VALUE_SERIALIZER, StringSerializer.class.getName());

        /**
         * An id string to pass to the server when making requests. The purpose of this is to be able to track the source
         * of requests beyond just ip/port by allowing a logical application name to be included in server-side request
         * logging.
         */
        properties.put(KafkaConfig.Producer.CLIENT_ID, clientId);

        return properties;
    }
}
