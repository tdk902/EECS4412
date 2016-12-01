package eecs4412.project.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.StringJoiner;
import java.util.TreeMap;
import java.util.stream.Collectors;

public class ArffDataModel {
    
    private List<TreeMap<String, Object>> rows;
    private String relationName;
    
    public ArffDataModel(String relationName) {
        super();
        rows = new ArrayList<>();
        this.relationName = relationName;
    }
    
    public void addRow(TreeMap<String, Object> row){
        rows.add(row);
    }
    
    public List<TreeMap<String, Object>> getRows() {
        return rows;
    }

    public void setRows(List<TreeMap<String, Object>> rows) {
        this.rows = rows;
    }

    public String getRelationName() {
        return relationName;
    }

    public void setRelationName(String relationName) {
        this.relationName = relationName;
    }
    
    public String getHeader(){
        Collection<String> header = rows.stream().findAny().get().keySet();
        return header.stream()
                     .map(entry -> {
                              String col = "";
                              if("class".equals(entry)){
                                  col = String.format("@Attribute %s {H,S}", entry);
                              } else if ("'class'".equals(entry)) {
                                  col = String.format("@Attribute %s numeric", "'_class'"); // for WEKA
                              } else {
                                  col = String.format("@Attribute %s numeric", entry); 
                              }
                              return col ;
                     })
                     .collect(Collectors.joining(System.lineSeparator()));
    }

    public String getData() {
        return rows.stream()
                   .map(row -> csv(row.values()))
                   .collect(Collectors.joining(System.lineSeparator()));
    }

    @Override
    public String toString() {
        StringJoiner file = new StringJoiner(System.lineSeparator());
        file.add(String.format("@Relation %s", relationName))
            .add(getHeader())
            .add("@Data")
            .add(getData());
        return file.toString();
    }
    
    private String csv (Collection<Object> col) {
        return col.stream()
                  .map(obj -> obj.toString())
                  .collect(Collectors.joining(","));
    }
}
