package java.graphoenix.meta.dto;

public class GraphQLPath {

    private String operation;

    private String selection;

    private Integer index;

    private GraphQLPath parent;

    public String getOperation() {
        return operation;
    }

    public void setOperation(String operation) {
        this.operation = operation;
    }

    public String getSelection() {
        return selection;
    }

    public void setSelection(String selection) {
        this.selection = selection;
    }

    public Integer getIndex() {
        return index;
    }

    public void setIndex(Integer index) {
        this.index = index;
    }

    public GraphQLPath getParent() {
        return parent;
    }

    public void setParent(GraphQLPath parent) {
        this.parent = parent;
    }
}
