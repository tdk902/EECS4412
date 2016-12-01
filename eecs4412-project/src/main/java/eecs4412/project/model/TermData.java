package eecs4412.project.model;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

public class TermData implements Comparable<TermData>, Comparator<TermData>{
    private String term;
    private final Map<String, Integer> frequencyInDocument;
    private int termTotalFrequncy = 0;
    
    public TermData(String term) {
        super();
        this.setTerm(term);
        this.frequencyInDocument = new HashMap<>();
    }
    
    public int getTermFrequencyInDocument(String document) {
        return frequencyInDocument.containsKey(document) ? frequencyInDocument.get(document) : 0;
    }

    public void addTermFrequencyInDocument(String document) {
        termTotalFrequncy++;
        int docFreq = frequencyInDocument.containsKey(document) ? frequencyInDocument.get(document)+1 : 1;
        frequencyInDocument.put(document, docFreq);
    }

    public int getDocumentFrequency() {
        return frequencyInDocument.size();
    }
    
    public int getTermTotalFrequncy() {
        return termTotalFrequncy;
    }

    public String getTerm() {
        return term;
    }

    public void setTerm(String term) {
        this.term = term;
    }

    @Override
    public int compare(TermData o1, TermData o2) {
        return o1.term.compareToIgnoreCase(o2.term);
    }

    @Override
    public int compareTo(TermData o) {
        return this.term.compareToIgnoreCase(o.term);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((term == null) ? 0 : term.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        TermData other = (TermData) obj;
        if (term == null) {
            if (other.term != null)
                return false;
        } else if (!term.equals(other.term))
            return false;
        return true;
    }
}
