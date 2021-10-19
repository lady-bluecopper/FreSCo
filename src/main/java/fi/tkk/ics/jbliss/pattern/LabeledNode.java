package fi.tkk.ics.jbliss.pattern;

public class LabeledNode extends Node {
    
    private int label;
    
    public LabeledNode(int id, int label) {
        super(id);
        this.label = label;
    }
    
    public int getLabel() {
        return label;
    }

    public void setLabel(int label) {
        this.label = label;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LabeledNode node = (LabeledNode) o;
        return this.getIndex() == node.getIndex() && this.getLabel() == node.getLabel();
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 23 * hash + this.label;
        return hash;
    }
    
    @Override
    public String toString() {
        return "(" + this.getIndex() + "," + label + ")";
    }
}
