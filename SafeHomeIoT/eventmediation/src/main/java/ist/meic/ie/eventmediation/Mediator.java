package ist.meic.ie.eventmediation;

import ist.meic.ie.events.EventItem;
import ist.meic.ie.events.exceptions.InvalidEventTypeException;
import ist.meic.ie.utils.Constants;
import ist.meic.ie.utils.DatabaseConfig;
import ist.meic.ie.utils.KafkaConfig;
import org.apache.commons.cli.*;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.json.simple.parser.ParseException;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class Mediator {
    public static void main(String[] args) throws SQLException {
        CommandLine cmd = parseArgs(args);
        List<String> topics = new ArrayList<String>(Arrays.asList(cmd.getOptionValue("topics").split(":")));
        System.out.println(topics);

        KafkaConsumer<String, String> consumer = KafkaConfig.createKafkaConsumer(cmd.getOptionValue("kafkaip"), "group-id-test", topics);
        DatabaseConfig config = new DatabaseConfig(Constants.MEDIATION_DB, "SafeHomeIoTEvents", Constants.MEDIATION_DB_USER, Constants.MEDIATION_DB_PASSWORD);
        while (true) {
            ConsumerRecords<String, String> records = consumer.poll(100);
            for (ConsumerRecord<String, String> record : records) {
                try {
                    EventItem eventItem =  new EventItem(record.value());
                    eventItem.getEvent().insertToDb(config);
                    System.out.println(eventItem.getEvent());
                } catch (InvalidEventTypeException | ParseException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private static CommandLine parseArgs(String[] args) {
        Options options = new Options();
        Option input1 = new Option("kafkaip", "kafkaip", true, "ip:port of the kafka server");
        input1.setRequired(true);
        options.addOption(input1);

        Option input2 = new Option("awsip", "awsip", true, "endpoint of AWS RDS");
        input2.setRequired(false);
        options.addOption(input2);

        Option input3 = new Option("dbname", "dbname", true, "name of the AWS RD databse");
        input3.setRequired(false);
        options.addOption(input3);

        Option input4 = new Option("username", "masterusername", true, "master username to access the database");
        input4.setRequired(false);
        options.addOption(input4);

        Option input5 = new Option("password", "password", true, "password to access the database");
        input5.setRequired(false);
        options.addOption(input5);

        Option input6 = new Option("topics", "topics", true, "Kafka topics to subscribe to");
        input5.setRequired(false);
        options.addOption(input6);

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