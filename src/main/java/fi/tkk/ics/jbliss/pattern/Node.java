package fi.tkk.ics.jbliss.pattern;


import java.io.Serializable;
import java.util.Objects;

public class Node implements Serializable {

    private int index;

    public Node(int index) {
        this.index = index;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Node node = (Node) o;
        return index == node.index;
    }

    @Override
    public int hashCode() {
        return Objects.hash(index);
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    @Override
    public String toString() {
        return "(" + index + ")";
    }

}
