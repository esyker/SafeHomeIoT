package ist.meic.ie.alarmsreader;

import ist.meic.ie.events.exceptions.InvalidEventTypeException;
import ist.meic.ie.utils.Constants;
import ist.meic.ie.utils.DatabaseConfig;
import ist.meic.ie.utils.HTTPMessages;

import java.awt.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.*;
import java.util.List;
import java.util.List.*;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.json.simple.JSONObject;
import org.json.simple.JSONArray;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import ist.meic.ie.utils.Constants;
import ist.meic.ie.utils.*;
import ist.meic.ie.events.*;

public class AlarmReader {
    private String kafkaIP;
    private List<String> topic;

    public AlarmReader(String kafkaIP)
    {
        this.kafkaIP=kafkaIP;
        topic = new ArrayList<>(Arrays.asList("alarmTopic"));
    }

    public void receiveEvents(){
        KafkaConsumer<String, String> consumer = KafkaConfig.createKafkaConsumer(kafkaIP, "alarm-reader", topic);
        int counter = 0;
        while (true) {
            ConsumerRecords<String, String> records = consumer.poll(100);
            for (ConsumerRecord<String, String> record : records) {
                try {
                    System.out.println(record.value());
                    JSONParser parser = new JSONParser();
                    JSONObject event = (JSONObject) parser.parse(record.value());
                    if (event.get("type") == null)
                        throw new InvalidEventTypeException(event.toJSONString());
                    //Call Camunda Alarm Business process endpoint
                    //HTTPMessages.postMsg(event,"application/json","camunda-alarm.com");
                    System.out.println(record.value());
                } catch (InvalidEventTypeException | org.json.simple.parser.ParseException e) {
                    e.printStackTrace();
                }
                counter++;
            }
            System.out.println("Messages received: "  + counter);
        }
    }
}
