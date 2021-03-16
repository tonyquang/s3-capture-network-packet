package com.facebook.capturepacket;

import com.facebook.capturepacket.controller.StartApp;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.cli.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public class CapturingApplication {

    public static void main(String[] args) {
        Options options = new Options();

        Option input = new Option("k", "kafka", true, "kafka server");
        input.setRequired(true);
        options.addOption(input);

        Option output = new Option("t", "topic", true, "topic name");
        output.setRequired(true);
        options.addOption(output);

        CommandLineParser parser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();
        CommandLine cmd;

        try {
            cmd = parser.parse(options, args);

            String kafkaAddress = cmd.getOptionValue("kafka");
            String topicName = cmd.getOptionValue("topic");

            if(!checkValidKafkaAddress(kafkaAddress)){
                log.error("Kafka Server Address Is Invalid");
                System.exit(1);
            }else{
                StartApp starter = new StartApp();
                starter.startApp(kafkaAddress, topicName);
            }

        } catch (ParseException e) {
            log.error(e.getMessage());
            formatter.printHelp("CapturingApplication", options);
            System.exit(1);
        }

    }

    private static boolean checkValidKafkaAddress(String kafkaAddress){
        Pattern pattern = Pattern.compile("^([0-9]{1,3}(?:\\.[0-9]{1,3}){3}|[a-zA-Z]+):([0-9]{1,5})$");
        Matcher matcher = pattern.matcher(kafkaAddress);
        return matcher.find();
    }

}
