package eecs4412.project.main;

import static eecs4412.project.util.Util.*;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.Charset;
import java.nio.file.DirectoryStream.Filter;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import eecs4412.project.model.ArffDataModel;
import eecs4412.project.model.InvertedIndexFile;
import eecs4412.project.model.TermData;
import eecs4412.project.util.PorterStemmer;

public class Main {
    
    public static final Consumer<Object> PRINTER = System.out::println;
    public static final Filter<Path> FILES_ONLY = new Filter<Path>(){
        @Override
        public boolean accept(Path entry) throws IOException {
            return Files.isRegularFile(entry, LinkOption.NOFOLLOW_LINKS);
        }
    };

    // *** Shared resource
    private InvertedIndexFile invertedIndexFile;
    private Set<String> stopWords = new HashSet<>();
    private double upperPercentile = DEFAULT_UPPER_PERCENTILE;
    private double lowerPercentile = DEFAULT_LOWER_PERCENTILE;
    private ArffDataModel arffDataModel;
    private Set<String> selectedAttributes = new TreeSet<>();
    
    /**
     * 
     * @param inputPath
     * @throws IOException
     */
    public void test(Path inputPath, Path outputPath) throws IOException{
        invertedIndexFile = new InvertedIndexFile();
        arffDataModel = new ArffDataModel("email-filter-test");
        if(Files.isDirectory(inputPath)){
            Files.newDirectoryStream(inputPath, FILES_ONLY)
            .forEach(filePreprocessor());
        } else {
            filePreprocessor().accept(inputPath);
        }
        arffBuilder().accept(invertedIndexFile);
        fileProducer(outputPath).accept(arffDataModel.toString());
        fileProducer(outputPath.getParent().resolve("testInvertedIndexFile.txt")).accept(invertedIndexFile.toString());
    }

    
    /**
     * Main function that is run in this class
     * @param inputPath
     * @param stopWordsPath
     * @throws IOException
     */
    public void train(Path inputPath, Path outputPath) throws IOException{
        invertedIndexFile = new InvertedIndexFile();
        arffDataModel = new ArffDataModel("email-filter-train");
        if(Files.isDirectory(inputPath)){
            Files.newDirectoryStream(inputPath, FILES_ONLY)
            .forEach(filePreprocessor());
        } else {
            filePreprocessor().accept(inputPath);
        }
        wordSelector().accept(invertedIndexFile);
        arffBuilder().accept(invertedIndexFile);
        fileProducer(outputPath).accept(arffDataModel.toString());
        fileProducer(outputPath.getParent().resolve("trainInvertedIndexFile.txt")).accept(invertedIndexFile.toString());
    }
    
    /**
     * 
     * @return
     */
    public Consumer<InvertedIndexFile> arffBuilder() {
        return (indexFile)->{
            Map<String, TermData> terms = indexFile.getTermMap();
            Collection<String> docs = indexFile.getDocuments();
            final double maxTermFrequency = (double)indexFile.getMaxTermFrequency();
            final double documentCount = (double)indexFile.getDocumentCount();
            docs.stream()
                .forEach(doc -> {
                    TreeMap<String, Object> row = new TreeMap<>();
                    selectedAttributes.stream()
                         .forEach(term -> {
                             TermData termData = terms.get(term); 
                             double termWeight = tfidf(termData.getTermFrequencyInDocument(doc), 
                                     termData.getDocumentFrequency(), maxTermFrequency, documentCount);
                             row.put(String.format("'%s'", termData.getTerm()), termWeight);
                         });
                    row.put("class", doc.toLowerCase().contains("ham") ? "H" : "S");
                    arffDataModel.addRow(row);
                });
        };
    }

    /**
     * Consume the passed in PATH
     * @return
     */
    public Consumer<Path> filePreprocessor(){
        return (file) ->{
            Collection<String> fileAsList = null;
            try {
                fileAsList = Files.readAllLines(file, Charset.forName("ISO-8859-1"));
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
            getPreProcessor(file.getFileName().toString(), stopWords).apply(fileAsList);
        };
    }

    /**
     * Selects words based on their frequencies from the inverted Index File
     * @return
     */
    public Consumer<InvertedIndexFile> wordSelector() {
        return  (invertedIndexFile) ->{
            int minDocFrequency = (int) (lowerPercentile * invertedIndexFile.getDocumentCount());
            int maxDocFrequency = (int) (upperPercentile * invertedIndexFile.getDocumentCount());
            invertedIndexFile.trimIndex(minDocFrequency, maxDocFrequency);
            selectedAttributes.addAll(invertedIndexFile.getTerms());
        };
    }

    /**
     * Read the stop-words dictionary to in memory
     * @param filePath
     * @throws IOException
     */
    public void readStopWords(Path filePath) throws IOException {
        stopWords = Files.readAllLines(filePath)
                .stream()
                .map(line -> line.split("[,|\\s+]"))
                .flatMap(Arrays :: stream)
                .map(word -> word.toLowerCase())
                .filter(word -> !word.trim().isEmpty())
                .collect(Collectors.toSet());
    }

    /**
     * PreProcessor returns a composite function that takes a raw data as Collection<String>
     * then pre-process the data and return the final result 
     * @param stopWords
     * @return
     */
    public Function<Collection<String>, Collection<String>> getPreProcessor(String filename, Collection<String> stopWords){
        return extractStemmedNonStopWords(filename, stopWords); 
//              .andThen(buildIndexDocument(filename));
    }

    /**
     * This function extract distinct words in a file and sort them in alphabetic order
     * @param filePath
     * @return
     * @throws IOException
     */
    public Function<Collection<String>, Collection<String>> extractStemmedNonStopWords(String document, Collection<String> dictionary){
        Function<Collection<String>, Collection<String>> extractor = (inputFile) -> {
            final PorterStemmer stemmer = new PorterStemmer();
            Collection<String> words = inputFile
                    .stream()
                    .map(line -> line.split("[^A-Za-z]+"))
                    .flatMap(Arrays::stream)
                    .map(word ->stemmer.stem(word.toLowerCase()))
                    .filter(term -> {
                        if(!term.trim().isEmpty() && !dictionary.contains(term)){
                            invertedIndexFile.mapTermToDoc(term, document);
                            return true;
                        }
                        return false;
                     })
                    .collect(Collectors.toList());
            return words;
        };
        return extractor;
    }
    
    /**
     * Inverse Term frequency in a document 
     * @param termFrequency
     * @param documentFrequency
     * @param maxDocumentTermFrequency
     * @param numberOfDocuments
     * @return
     */
    public double tfidf(double termFrequency, double documentFrequency, 
            double maxDocumentTermFrequency, double numberOfDocuments){
        double tf  = termFrequency / (double) maxDocumentTermFrequency;
        double idf =  Math.log((double) numberOfDocuments / documentFrequency);
        return  tf * idf;
    }

    /**
     * 
     * @param filePath
     * @return
     */
    public Path generateTrainingData(Path filePath){
        PRINTER.accept("generateTrainingData("+filePath+") start...");
        PRINTER.accept("generateTrainingData("+filePath+") end with("+filePath+")");
        return filePath;
    }

    /**
     * 
     * @param filePath
     * @return
     */
    public Path generateTestingData(Path filePath){
        PRINTER.accept("generateTestingData("+filePath+") start...");
        PRINTER.accept("generateTestingData("+filePath+") end with("+filePath+")");
        return filePath;
    }

    /**
     * This consumer writes the collection to filename
     * @param filename
     * @return
     */
    public Consumer<Collection<String>> listWriter(Path filename) {
        Consumer<Collection<String>> writer = (file) ->{
            String fileAsString = file.stream()
                    .collect(Collectors.joining(System.lineSeparator()));
            fileProducer(filename).accept(fileAsString);
        };
        return writer;
    }
    
    /**
     * 
     * @param fname
     * @return
     */
    public Consumer<String> fileProducer(Path fname){
        Consumer<String> writer = (content) ->{
            try(BufferedWriter w = Files.newBufferedWriter(fname)){
                w.write(content);
            }catch(IOException ex){
                throw new UncheckedIOException(ex);
            }
        };
        return writer;
    }
    
    /**
     * Main function
     * @param args
     * @throws IOException
     */
    public static void main(String[] args) throws IOException {
        PRINTER.accept("Preprocessing...");
        Map<String, String> parsed = parseArgs(args);
        Main instance = new Main();
        if(parsed.get(STOP_WORDS_FILE_ARG) != null){
           instance.readStopWords(Paths.get(parsed.get(STOP_WORDS_FILE_ARG)));
        }
        if(parsed.get(UPPER_PERCENT_ARG) != null){
            instance.upperPercentile = Double.parseDouble(parsed.get(UPPER_PERCENT_ARG));
         }
        if(parsed.get(LOWER_PERCENT_ARG) != null){
            instance.lowerPercentile = Double.parseDouble(parsed.get(LOWER_PERCENT_ARG));
         }
        String trainOutPath = DEFAULT_OUT_PATH; 
        String testOutPath = DEFAULT_OUT_PATH;
        if(parsed.get(TRAIN_OUT_FILE_ARG) != null){
            trainOutPath = parsed.get(TRAIN_OUT_FILE_ARG);
         }
        if(parsed.get(TEST_OUT_FILE_ARG) != null){
            testOutPath = parsed.get(TEST_OUT_FILE_ARG);
         }
        instance.train(Paths.get(parsed.get(TRAIN_FILE_ARG)), Paths.get(trainOutPath).resolve("train.arff"));
        instance.test(Paths.get(parsed.get(TEST_FILE_ARG)), Paths.get(testOutPath).resolve("test.arff"));
        PRINTER.accept("Done.");
    }
}
