package ist.meic.ie.statusmediation;

import ist.meic.ie.utils.Constants;
import ist.meic.ie.utils.DatabaseConfig;
import ist.meic.ie.utils.KafkaConfig;
import org.apache.commons.cli.*;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;

public class Mediator {
    public static void main(String[] args) throws SQLException {
        CommandLine cmd = parseArgs(args);
        //dbname=MSISDNStatus
        DatabaseConfig statusDBConfig =new DatabaseConfig(Constants.MEDIATION_DB, "MSISDNStatus", Constants.MEDIATION_DB_USER, Constants.MEDIATION_DB_PASSWORD);

        KafkaConsumer<String, String> consumer = KafkaConfig.createKafkaConsumer(cmd.getOptionValue("kafkaip"), "statusmediator", Collections.singletonList("StatusSIMCARD"));
        while (true) {
            ConsumerRecords<String, String> records = consumer.poll(100);
            for (ConsumerRecord<String, String> record : records) {
                try {
                    System.out.println(record.value());
                    JSONParser parser = new JSONParser();
                    JSONObject event = (JSONObject) parser.parse(record.value());
                    JSONObject statussimcard= (JSONObject) event.get("StatusSIMCARD");
                    JSONObject SIMCARD = (JSONObject) statussimcard.get("SIMCARD");

                    for(Iterator iterator = SIMCARD.entrySet().iterator(); iterator.hasNext();) {
                        Map.Entry<String,String> pair = (Map.Entry<String,String>)iterator.next();
                        System.out.println(pair.getKey());
                        System.out.println(pair.getValue());
                        int simcard = Integer.parseInt(pair.getKey());
                        int status = Integer.parseInt(pair.getValue());
                        PreparedStatement insertStatus;
                        insertStatus = statusDBConfig.getConnection().prepareStatement ("insert into Status" + " (SIMCARD, Status) values(?,?)");
                        insertStatus.setInt(1,simcard);
                        insertStatus.setInt(2,status);
                        insertStatus.executeUpdate();
                        insertStatus.close();
                    }
                } catch (org.json.simple.parser.ParseException e) {
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


