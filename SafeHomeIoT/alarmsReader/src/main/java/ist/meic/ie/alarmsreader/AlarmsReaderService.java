package ist.meic.ie.alarmsreader;

import ist.meic.ie.utils.KafkaConfig;
import org.apache.commons.cli.*;
import org.apache.kafka.clients.producer.KafkaProducer;

public class AlarmsReaderService {

    public static void main(String args[]){
        CommandLine cmd = parseArgs(args);
        System.out.println(cmd.getOptionValue("kafkaip"));
        AlarmReader eventsService = new AlarmReader(cmd.getOptionValue("kafkaip"));
        eventsService.receiveEvents();
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



