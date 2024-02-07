package ist.meic.ie.testconsumer;

import ist.meic.ie.events.exceptions.InvalidEventTypeException;
import ist.meic.ie.utils.KafkaConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.util.Collections;

public class TestConsumer {
    public static void main(String[] args) {
        KafkaConsumer<String,String> consumer = KafkaConfig.createKafkaConsumer("3.93.180.58:9092", "topic-consumer1", Collections.singletonList("alarm-topic"));

        while (true) {
            ConsumerRecords<String, String> records = consumer.poll(100);
            for (ConsumerRecord<String, String> record : records) {
                System.out.println(record.value());
            }
        }
    }
}