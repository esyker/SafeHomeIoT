package ist.meic.ie.kafkastreamalarm;

import ist.meic.ie.events.exceptions.InvalidEventTypeException;
import ist.meic.ie.utils.KafkaConfig;
import org.apache.commons.cli.*;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import java.util.*;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;


public class AlarmConsumerProducer {

    public static void main(String[] args) {
        CommandLine cmd = parseArgs(args);
        //input stream
        String kafkaIP=cmd.getOptionValue("kafkaip");
        String inputTopic ="events-messages";
        String outputTopic = "alarmTopic";

        List<String> topics = new ArrayList<>(Arrays.asList(inputTopic));
        System.out.println(topics);

        KafkaConsumer<String, String> consumer = KafkaConfig.createKafkaConsumer(kafkaIP,
                "alarm-stream-alternative", topics);
        KafkaProducer<String, String> producer = KafkaConfig.createKafkaProducer(kafkaIP);
        int counter = 0;
        while (true) {
            ConsumerRecords<String, String> records = consumer.poll(100);
            for (ConsumerRecord<String, String> record : records) {
                try {
                    System.out.println(record.value());

                    JSONParser parser = new JSONParser();
                    JSONObject event = (JSONObject) parser.parse(record.value());
                    String type = (String) event.get("type");
                    if (type == null)
                        throw new InvalidEventTypeException(event.toJSONString());
                    if(checkIsAlarm(type, event)){
                        ProducerRecord<String, String> producerRecord = new ProducerRecord<>(outputTopic,type, record.value());
                        producer.send(producerRecord);
                    }
                } catch (InvalidEventTypeException | org.json.simple.parser.ParseException e) {
                    e.printStackTrace();
                }
                counter++;
            }
            System.out.println("Messages received: "  + counter);
        }
    }

    private static boolean checkIsAlarm(String type, JSONObject event)
    {
        if(type.equals("video")||type.equals("motion")||type.equals("image")){
            String description=(String) event.get("description");
            if(description==null){
                System.out.println("Description has null value!");
            }
            else if(description.equals("intrusion")){
                return true;
            }
        }
        else
        {
            Double measurement = (Double) event.get("measurement");
            if(measurement==null){
                System.out.println("Measurement has null value!");
            }
            else if(measurement>1000||measurement<-1000)
                return true;
        }
        return false;
    }

    private static CommandLine parseArgs(String[] args) {
        Options options = new Options();
        Option input1 = new Option("kafkaip", "kafkaip", true, "ip:port of the kafka server");
        input1.setRequired(true);
        options.addOption(input1);

        CommandLineParser parser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();
        CommandLine cmd = null;

        try {
            cmd = parser.parse(options, args);
        } catch (org.apache.commons.cli.ParseException e) {
            System.out.println(e.getMessage());
            formatter.printHelp("utility-name", options);

            System.exit(1);
        }
        return cmd;
    }

}
