package eecs4412.project.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.DirectoryStream.Filter;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

public final class Util {
    
    public static final String STOP_WORDS_FILE_ARG = "s";
    public static final String TRAIN_FILE_ARG = "t";
    public static final String TRAIN_OUT_FILE_ARG = "to";
    public static final String TEST_OUT_FILE_ARG = "To";
    public static final String TEST_FILE_ARG = "T";
    public static final String UPPER_PERCENT_ARG = "u";
    public static final String LOWER_PERCENT_ARG = "l";

    public static final double DEFAULT_UPPER_PERCENTILE = 0.59;
    public static final double DEFAULT_LOWER_PERCENTILE = 0.01;
    public static final String DEFAULT_OUT_PATH = System.getProperty("user.dir");

    public static final Consumer<Object> PRINTER = System.out::println;
    public static final Filter<Path> FILES_ONLY = new Filter<Path>(){
        @Override
        public boolean accept(Path entry) throws IOException {
            return Files.isRegularFile(entry, LinkOption.NOFOLLOW_LINKS);
        }
    };

    /**
     * 
     * @param args
     * @return
     */
    public static Map<String, String> parseArgs(String... args) {
        // command line options
        Options options = new Options();
        Option stopWordsFormat = new Option( STOP_WORDS_FILE_ARG, "stop-words file" );
        Option trainFileRawFormat = new Option( TRAIN_FILE_ARG, "train-data file" );
        Option testFileRawFormat = new Option( TEST_FILE_ARG, "test-data file" );
        Option trainOutFormat = new Option( TRAIN_OUT_FILE_ARG, "train-output file" );
        Option testOutFormat = new Option( TEST_OUT_FILE_ARG, "test-output file" );
        Option upperPercntileFormat = new Option( UPPER_PERCENT_ARG, "frequeny-upper percintle [0.0 - 1.0]" );
        Option lowerPercntileFormat = new Option( LOWER_PERCENT_ARG, "frequeny-lower percintle [0.0 - 1.0]" );
       
        stopWordsFormat.setRequired(false);
        stopWordsFormat.setArgs(1);
        stopWordsFormat.setLongOpt("[optional] path to file containg stop words [csv or space-separated]");
        trainFileRawFormat.setRequired(true);
        trainFileRawFormat.setArgs(1);
        trainFileRawFormat.setLongOpt("path to file/directory containg training emails");
        testFileRawFormat.setRequired(true);
        testFileRawFormat.setArgs(1);
        testFileRawFormat.setLongOpt("path to file/directory containg raw test emails");
        trainOutFormat.setRequired(false);
        trainOutFormat.setArgs(1);
        trainOutFormat.setLongOpt("[optional] path to output directory containg trains dataset [ARFF format]");
        testOutFormat.setRequired(false);
        testOutFormat.setArgs(1);
        testOutFormat.setLongOpt("[optional] path to output directory containg test dataset [ARFF format]");
        upperPercntileFormat.setRequired(false);
        upperPercntileFormat.setArgs(1);
        upperPercntileFormat.setLongOpt("[optional] upper percentile for frequency selection [defualt 0.59]");
        lowerPercntileFormat.setRequired(false);
        lowerPercntileFormat.setArgs(1);
        lowerPercntileFormat.setLongOpt("[optional] lower percentile for frequency selection [defualt 0.01]");

        options.addOption(trainFileRawFormat);
        options.addOption(testFileRawFormat);
        options.addOption(stopWordsFormat);
        options.addOption(trainOutFormat);
        options.addOption(testOutFormat);
        options.addOption(upperPercntileFormat);
        options.addOption(lowerPercntileFormat);

        // handle command arguments
        CommandLine commandline = null;
        CommandLineParser parser = new DefaultParser();
        try {
            commandline = parser.parse(options, args);
        } catch (ParseException e) {
            printUsageAndExit(options);
        }
        Objects.requireNonNull(commandline);
        if(!Files.isRegularFile(Paths.get(commandline.getOptionValue(TRAIN_FILE_ARG))) &&
           !Files.isDirectory(Paths.get(commandline.getOptionValue(TRAIN_FILE_ARG)))){
            printUsageAndExit(options);
        }
           
         if(!Files.isRegularFile(Paths.get(commandline.getOptionValue(TEST_FILE_ARG))) && 
            !Files.isDirectory(Paths.get(commandline.getOptionValue(TEST_FILE_ARG)))){
            printUsageAndExit(options);
         }
        
        // optional 
        try{
            double upperPercintile = DEFAULT_UPPER_PERCENTILE;
            double lowerPercintile = DEFAULT_LOWER_PERCENTILE;
            if(commandline.getOptionValue(UPPER_PERCENT_ARG) != null){
                upperPercintile = Double.parseDouble(commandline.getOptionValue(UPPER_PERCENT_ARG));
            }
            if(commandline.getOptionValue(LOWER_PERCENT_ARG) != null){
                lowerPercintile = Double.parseDouble(commandline.getOptionValue(LOWER_PERCENT_ARG));
            }
            if(Double.compare(1d, upperPercintile) < 0
                    || Double.compare(0d, upperPercintile) > 0){
                throw new Exception();
            }
            if(Double.compare(1d, lowerPercintile) < 0
                    || Double.compare(0d, lowerPercintile) > 0){
                throw new Exception();
            }
            if(Double.compare(upperPercintile, lowerPercintile) <= 0){
                throw new Exception();
            }
        } catch (Exception ex){
            printUsageAndExit(options);
        }
        
        Map<String, String> retValue = new HashMap<>(10);
        retValue.put(TRAIN_FILE_ARG, commandline.getOptionValue(TRAIN_FILE_ARG));
        retValue.put(TEST_FILE_ARG, commandline.getOptionValue(TEST_FILE_ARG));
        retValue.put(TRAIN_OUT_FILE_ARG, commandline.getOptionValue(TRAIN_OUT_FILE_ARG));
        retValue.put(TEST_OUT_FILE_ARG, commandline.getOptionValue(TEST_OUT_FILE_ARG));
        retValue.put(STOP_WORDS_FILE_ARG, commandline.getOptionValue(STOP_WORDS_FILE_ARG));
        retValue.put(UPPER_PERCENT_ARG, commandline.getOptionValue(UPPER_PERCENT_ARG));
        retValue.put(LOWER_PERCENT_ARG, commandline.getOptionValue(LOWER_PERCENT_ARG));
        return retValue;
    }

    // *** helper method 
    private static void printUsageAndExit(Options options) {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("mail-filter", options, true);
        System.exit(1);
    }
}
