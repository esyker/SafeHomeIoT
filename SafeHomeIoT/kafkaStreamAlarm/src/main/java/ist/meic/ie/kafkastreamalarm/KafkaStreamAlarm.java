package ist.meic.ie.kafkastreamalarm;

import ist.meic.ie.events.exceptions.InvalidEventTypeException;
import ist.meic.ie.utils.KafkaConfig;
import org.apache.commons.cli.*;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.DescribeClusterResult;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.utils.Bytes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.kstream.*;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.state.WindowStore;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;

import java.util.Arrays;
import java.util.Collection;
import java.util.Properties;
import java.util.concurrent.*;

import ist.meic.ie.events.EventItem;
import model.Event;
import ist.meic.ie.utils.KafkaConfig.*;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import serde.JsonSerializer;
import serde.JsonDeserializer;
import serde.WrapperSerde;


public class KafkaStreamAlarm {

    static public final class EventItemSerde extends WrapperSerde<EventItem> {
        public EventItemSerde() {
            super(new JsonSerializer<EventItem>(), new JsonDeserializer<EventItem>(EventItem.class));
        }
    }
    /*

    public static void main(String[] args) throws ParseException, InvalidEventTypeException, InterruptedException {
        CommandLine cmd = parseArgs(args);
        //input stream
        Properties streamProps = KafkaConfig.createKafkaStreamProps(cmd.getOptionValue("kafkaip"), "alarm-stream8");
        streamProps.put("event.serializer", "serde.JsonSerializer");
        streamProps.put("event.deserializer","serde.JsonDeserializer");
        StreamsBuilder builder = new StreamsBuilder();
        String inputTopic ="events-messages";
        Serde<Event> eventSerde = Serdes.serdeFrom(new JsonSerializer<Event>(),new JsonDeserializer<Event>());
        KStream<String, Event> messagesStream = builder.stream(inputTopic,Consumed.with(Serdes.String(),eventSerde));
        messagesStream.filter((id,event)->((event.getType().equals("video")||event.getType().equals("motion")||event.getType().equals("image"))
                        &&event.getDescription().equals("intrusion")) ||
                        (event.getType().equals("smoke") && (event.getMeasurement()>1000 || event.getMeasurement()<1000)) ||
                        (event.getType().equals("temperature") && (event.getMeasurement()>1000 || event.getMeasurement()<1000))
                ).to("alarmTopic");

        KafkaStreams streams = new KafkaStreams(builder.build(), streamProps);
        streams.cleanUp();
        streams.start();
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            @Override
            public void run() {
                streams.close();
            }
        }));

    }
    */


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
