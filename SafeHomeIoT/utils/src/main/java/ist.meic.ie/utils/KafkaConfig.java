package ist.meic.ie.utils;

import kafka.admin.AdminUtils;
import kafka.admin.RackAwareMode;
import kafka.utils.ZkUtils;
import org.I0Itec.zkclient.ZkClient;
import org.I0Itec.zkclient.ZkConnection;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
/*import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.StreamsBuilder;*/

import java.util.List;
import java.util.Properties;

public class KafkaConfig {

    private static String KEY_SERIALIZER = "org.apache.kafka.common.serialization.StringSerializer";
    private static String KEY_DESERIALIZER = "org.apache.kafka.common.serialization.StringDeserializer";
    private static String VALUE_SERIALIZER = "org.apache.kafka.common.serialization.StringSerializer";
    private static String VALUE_DESERIALIZER = "org.apache.kafka.common.serialization.StringDeserializer";

    public static void createTopic(ZookeeperConfig zookeeperConfig, boolean isSecureKafkaCluster, String topicname, int numPartitions, int numReplicas, Properties topicConfig) {
        ZkClient zkClient = zookeeperConfig.openZkClient();
        ZkConnection zkConnection = zookeeperConfig.connect();
        ZkUtils zkUtils = new ZkUtils(zkClient, zkConnection, isSecureKafkaCluster);
        AdminUtils.createTopic(zkUtils, topicname, numPartitions, numReplicas, topicConfig, RackAwareMode.Enforced$.MODULE$);
        zkClient.close();
    }

    public static boolean topicExists(ZookeeperConfig zookeeperConfig, boolean isSecureKafkaCluster, String topicname) {
        ZkClient zkClient = zookeeperConfig.openZkClient();
        ZkConnection zkConnection = zookeeperConfig.connect();
        ZkUtils zkUtils = new ZkUtils(zkClient, zkConnection, isSecureKafkaCluster);
        boolean exists = AdminUtils.topicExists(zkUtils, topicname);
        zkClient.close();
        return exists;
    }


    public static KafkaConsumer<String, String> createKafkaConsumer(String bootstrapServers, String groupId, List<String> topicsToSubscribe) {
        Properties consumerProperties = new Properties();
        System.out.println("KafkaConsumer "+consumerProperties+" "+bootstrapServers+" "+groupId+" "+topicsToSubscribe);
        consumerProperties.put("bootstrap.servers", bootstrapServers);
        consumerProperties.put("group.id", groupId);
        consumerProperties.put("key.deserializer", KEY_DESERIALIZER);
        consumerProperties.put("value.deserializer", VALUE_DESERIALIZER);
        KafkaConsumer<String, String> consumer = new KafkaConsumer<>(consumerProperties);
        consumer.subscribe(topicsToSubscribe);
        return consumer;
    }

    public static KafkaProducer<String, String> createKafkaProducer(String bootstrapServers) {
        Properties producerProperties = new Properties();
        producerProperties.put("bootstrap.servers", bootstrapServers);
        producerProperties.put("key.serializer", KEY_SERIALIZER);
        producerProperties.put("value.serializer",VALUE_SERIALIZER);
        return new KafkaProducer<>(producerProperties);
    }
    /*
    public static Properties createKafkaStreamProps(String bootstrapServers, String appID) {
        Properties streamProperties = new Properties();
        streamProperties.put("application.id", appID);
        streamProperties.put("bootstrap.servers", bootstrapServers);
        // setting offset reset to earliest
        streamProperties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        return streamProperties;
    }
    */

}
