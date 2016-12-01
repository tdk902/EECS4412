package eecs4412.project.model;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class InvertedIndexFile{

    private Map<String, TermData> termMap;
    private Set<String> documents;

    public InvertedIndexFile() {
        super();
        this.termMap = new HashMap<>();
        this.documents = new HashSet<>();
    }

    public InvertedIndexFile(Map<String, TermData> termMap, Set<String> documents) {
        this();
        this.termMap = termMap;
        this.documents = documents;
    }

    /**
     * maps a term to a document
     * @param term
     * @param document
     */
    public void mapTermToDoc(String term, String document) {
        TermData termData = termMap.get(term);
        if(termData == null){
            termData = new TermData(term);
            termMap.put(term, termData);
        }
        termData.addTermFrequencyInDocument(document);
        documents.add(document);
    }

    /**
     * 
     * @param term
     * @return
     */
    public int getTermTotalFrequncy(String term) {
        return termMap.containsKey(term) ? termMap.get(term).getTermTotalFrequncy() : 0;
    }

    /**
     * 
     * @param term
     * @param document
     * @return
     */
    public int getTermFrequencyInDocument(String term, String document) {
        return termMap.containsKey(term) ? termMap.get(term).getTermFrequencyInDocument(document) : 0;
    }

    /**
     * 
     * @param term
     * @return
     */
    public int getDocumentFrequency(String term) {
        return termMap.containsKey(term) ? termMap.get(term).getDocumentFrequency() : 0;
    }

    /**
     * 
     * @param min
     * @param max
     * @return
     */
    public Collection<TermData> getInnerWords(int min, int max) {
        return 
                termMap.entrySet()
                .stream()
                .map(entry -> entry.getValue())
                .filter(termData -> termData.getDocumentFrequency()>= min && termData.getDocumentFrequency()<= max)
                .collect(Collectors.toList());
    }

    /**
     * 
     * @param min
     * @param max
     * @return
     */
    public Collection<TermData> getOuterWords(int min, int max) {
        return 
                termMap.entrySet()
                .stream()
                .map(entry -> entry.getValue())
                .filter(termData -> termData.getDocumentFrequency()< min || termData.getDocumentFrequency()>max)
                .collect(Collectors.toList());
    }

    /**
     * 
     * @param min
     * @param max
     * @return
     */
    public int trimIndex(int min, int max) {
        Collection<TermData> trash = getOuterWords(min, max);
        trash.stream()
        .forEach(garbage -> termMap.remove(garbage.getTerm()));
        return trash.size();
    }

    /**
     * 
     * @return
     */
    public int getMaxTermFrequency() {
        int max = -1;
        if(!termMap.isEmpty()){
            max = termMap.entrySet()
                    .stream()
                    .map(entry -> entry.getValue().getTermTotalFrequncy())
                    .max(Integer::max)
                    .get();
        }
        return max;
    }

    /**
     * 
     * @return
     */
    public int getMinTermFrequency() {
        int min = -1;
        if(!termMap.isEmpty()){
            min = termMap.entrySet()
                    .stream()
                    .map(entry -> entry.getValue().getTermTotalFrequncy())
                    .min(Integer::min)
                    .get();
        }
        return min;
    }

    /**
     * 
     * @return
     */
    public Collection<String> getTerms() {
        return termMap.keySet();
    }
    
    /**
     * 
     * @return
     */
    public int getTermCount() {
        return termMap.size();
    }

    /**
     * 
     * @param term
     * @return
     */
    public boolean containsTerm(String term) {
        return termMap.containsKey(term);
    }

    /**
     * 
     * @return
     */
    public int getDocumentCount() {
        return documents.size();
    }

    @Override
    public String toString() {
        return termMap.entrySet()
        .stream()
        .map(entry -> {
            TermData termData = entry.getValue();
            StringBuffer sb = new StringBuffer();
            documents.stream()
            .filter(doc -> termData.getTermFrequencyInDocument(doc) > 0)
            .forEach(doc -> sb.append(String.format("%-20s %-20s %-20d %s", termData.getTerm(), doc, termData.getTermFrequencyInDocument(doc), System.lineSeparator())));
            return sb.toString();   
        })
        .sorted()
        .collect(Collectors.joining());
    }

    public Map<String, TermData> getTermMap() {
        return termMap;
    }

    public void setTermMap(Map<String, TermData> termMap) {
        this.termMap = termMap;
    }

    public Set<String> getDocuments() {
        return documents;
    }

    public void setDocuments(Set<String> documents) {
        this.documents = documents;
    }
}