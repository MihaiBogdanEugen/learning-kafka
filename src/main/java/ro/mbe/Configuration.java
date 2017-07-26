package ro.mbe;

import org.apache.kafka.clients.consumer.RangeAssignor;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.apache.kafka.clients.producer.internals.DefaultPartitioner;
import ro.mbe.custom.MessageJsonDeserializer;
import ro.mbe.custom.MessageJsonSerializer;

import java.util.*;
import java.util.stream.Collectors;

class Configuration {

    private static final String[] KafkaServers = new String[] {
            "localhost:19101",
            "localhost:19102",
            "localhost:19103"
    };

    static final Map<String, List<Integer>> TopicsAndPartitions = new HashMap<>();

    static final int PollingTimeout = 1000;
    static final int NoOfRecordsToSend = 100;
    static final int NoOfRecordsToReceive = 100;

    static {

        TopicsAndPartitions.put("sensors.first", Arrays.asList(0));
        TopicsAndPartitions.put("sensors.second", Arrays.asList(0, 1));
        TopicsAndPartitions.put("sensors.third", Arrays.asList(0, 1, 2));
    }

    static Collection<String> getAllTopics() {

        return TopicsAndPartitions.keySet();
    }

    static Collection<TopicPartition> getAllPartitions() {

        return Configuration.TopicsAndPartitions.entrySet()
                .stream()
                .flatMap(entry -> entry.getValue()
                        .stream()
                        .map(partition -> new TopicPartition(entry.getKey(), partition)))
                .collect(Collectors.toList());
    }

    /**
     * https://kafka.apache.org/documentation/#producerconfigs
     */
    static Properties getProducerConfig(String clientId) {

        Properties properties = new Properties();


        /** SETUP SETTINGS **/
        //  A list of host/port pairs to use for establishing the initial connection to the Kafka cluster
        properties.put(KafkaConfig.Producer.BOOTSTRAP_SERVERS, String.join(", ", KafkaServers));

        //  Serializer class for key
        properties.put(KafkaConfig.Producer.KEY_SERIALIZER, StringSerializer.class.getName());

        //  Serializer class for value
        properties.put(KafkaConfig.Producer.VALUE_SERIALIZER, MessageJsonSerializer.class.getName());

        //  An id string to pass to the server when making requests
        properties.put(KafkaConfig.Producer.CLIENT_ID, clientId);

        //  Close idle connections after the number of milliseconds specified by this config
        properties.put(KafkaConfig.Producer.CONNECTIONS_MAX_IDLE_MS, 540000);  //9 minutes

        //  The compression type for all data generated by the producer.
        properties.put(KafkaConfig.Producer.COMPRESSION_TYPE, KafkaConfig.Producer.CompressionType.NONE);

        //  Partitioner class that implements the Partitioner interface
        properties.put(KafkaConfig.Producer.PARTITIONER_CLASS, DefaultPartitioner.class.getName());


        /** BATCHING SETTINGS **/
        //  The default batch size in bytes
        properties.put(KafkaConfig.Producer.BATCH_SIZE, 16384);        //16 KB

        //  The total bytes of memory the producer can use to buffer records waiting to be sent to the server
        properties.put(KafkaConfig.Producer.BUFFER_MEMORY, 33554432);  //32 MB

        //  The configuration controls how long KafkaProducer.send() and KafkaProducer.partitionsFor() will block
        properties.put(KafkaConfig.Producer.MAX_BLOCK_MS, 60000);      // 1 minute

        //  This setting gives the upper bound on the delay for batching: once we get batch.size worth of records for a
        //  partition it will be sent immediately regardless of this setting, however if we have fewer than this many
        //  bytes accumulated for this partition we will 'linger' for the specified time waiting for more records to show up
        properties.put(KafkaConfig.Producer.LINGER_MS, 0);             // no delay


        /** QUALITY OF SERVICE SETTINGS **/
        //  The number of acknowledgments the producer requires the leader to have received before considering a request complete.
        properties.put(KafkaConfig.Producer.ACKS, KafkaConfig.Producer.Acks.LEADER);    //  Only leader acknowledgement

        //  How many times the producer will try to resend any record whose send fails with a potentially transient error
        properties.put(KafkaConfig.Producer.RETRIES, 0);                                // no retries

        //  The amount of time to wait before attempting to retry a failed request to a given topic partition
        properties.put(KafkaConfig.Producer.RETRY_BACKOFF_MS, 100);                     // .1 seconds

        //  The maximum number of unacknowledged requests the client will send on a single connection before blocking
        properties.put(KafkaConfig.Producer.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, 5);

        return properties;
    }

    /**
     * https://kafka.apache.org/documentation/#consumerconfigs
     */
    static Properties getConsumerConfig(String clientId, String groupId) {

        Properties properties = new Properties();


        /** SETUP SETTINGS **/
        // A list of host/port pairs to use for establishing the initial connection to the Kafka cluster
        properties.put(KafkaConfig.Consumer.BOOTSTRAP_SERVERS, String.join(", ", KafkaServers));

        //  Deserializer class for key
        properties.put(KafkaConfig.Consumer.KEY_DESERIALIZER, StringDeserializer.class.getName());

        //  Deserializer class for value
        properties.put(KafkaConfig.Consumer.VALUE_DESERIALIZER, MessageJsonDeserializer.class.getName());

        //  An id string to pass to the server when making requests
        properties.put(KafkaConfig.Consumer.CLIENT_ID, clientId);

        //  Close idle connections after the number of milliseconds specified by this config
        properties.put(KafkaConfig.Consumer.CONNECTIONS_MAX_IDLE_MS, 540000);  //  9 minutes


        /** QUALITY OF SERVICE SETTINGS **/
        //  If true the consumer's offset will be periodically committed in the background
        properties.put(KafkaConfig.Consumer.ENABLE_AUTO_COMMIT, true);

        //  The frequency in milliseconds that the consumer offsets are auto-committed to Kafka if enable.auto.commit is set to true
        properties.put(KafkaConfig.Consumer.AUTO_COMMIT_INTERVAL_MS, 5000);    //  5 seconds

        //  What to do when there is no initial offset in Kafka or if the current offset does not exist any more on the server
        //  Valid values are:
        //  - 'earliest': automatically reset the offset to the earliest offset
        //  - 'latest': automatically reset the offset to the latest offset
        //  - 'none': throw exception to the consumer if no previous offset is found for the consumer's group
        properties.put(KafkaConfig.Consumer.AUTO_OFFSET_RESET, KafkaConfig.Consumer.AutoOffsetReset.LATEST);

        //  The amount of time to wait before attempting to retry a failed request to a given topic partition
        properties.put(KafkaConfig.Consumer.RETRY_BACKOFF_MS, 100);            //  0.1 seconds

        //  Automatically check the CRC32 of the records consumed
        properties.put(KafkaConfig.Consumer.CHECK_CRCS, true);


        /** GROUPING SETTINGS **/
        if (groupId != null && groupId.length() > 0) {
            //  A unique string that identifies the consumer group this consumer belongs to
            properties.put(KafkaConfig.Consumer.GROUP_ID, groupId);

            //  The expected time between heartbeats to the consumer coordinator when using Kafka's group management facilities
            properties.put(KafkaConfig.Consumer.HEARTBEAT_INTERVAL_MS, 3000);  // .33 seconds

            //  The timeout used to detect consumer failures when using Kafka's group management facility
            properties.put(KafkaConfig.Consumer.SESSION_TIMEOUT_MS, 10000);    // 10 seconds

            //  The class name of the partition assignment strategy that the client will use to distribute partition ownership amongst consumer instances when group management is used
            properties.put(KafkaConfig.Consumer.PARTITION_ASSIGNMENT_STRATEGY, RangeAssignor.class.getName());    // 10 seconds

            //  The maximum delay between invocations of poll() when using consumer group management
            properties.put(KafkaConfig.Consumer.MAX_POLL_INTERVAL_MS, 300000);
        }


        /** THROTTLING SETTINGS **/
        //  The minimum amount of data the server should return for a fetch request
        properties.put(KafkaConfig.Consumer.FETCH_MIN_BYTES, 1);                   //  1 byte

        //  The maximum amount of time the server will block before answering the fetch request if there isn't sufficient data to immediately satisfy the requirement given by fetch.min.bytes
        properties.put(KafkaConfig.Consumer.FETCH_MAX_WAIT_MS, 500);               // 0.5 seconds

        //  The maximum amount of data per-partition the server will return
        properties.put(KafkaConfig.Consumer.MAX_PARTITION_FETCH_BYTES, 1048576);   // 1 Mb

        //  The maximum number of records returned in a single call to poll()
        properties.put(KafkaConfig.Consumer.MAX_POLL_RECORDS, 500);

        return properties;
    }
}
