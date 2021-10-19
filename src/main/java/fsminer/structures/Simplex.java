package fsminer.structures;

import com.google.common.collect.Sets;
import java.util.Set;

public class Simplex extends Simpl {
    
    boolean isMaximal;
    
    //dummy simplex
    public Simplex(Set<Integer> vertices) {
        super(-1, vertices);
    }
    
    public Simplex(int id, Set<Integer> vertices, boolean isMaximal) {
        super(id, vertices);
        this.isMaximal = isMaximal;
    }
    
    public Simplex(int id, int v) {
        super(id);
        addVertex(v);
        this.isMaximal = true;
    }
    
    public void setMaximal(boolean isMaximal) {
        this.isMaximal = isMaximal;
    }
    
    public boolean isMaximal() {
        return this.isMaximal;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Simplex that = (Simplex) o;
        return getVertices().equals(that.getVertices());
    }

    @Override
    public Simpl copy() {
        Set<Integer> newV = Sets.newHashSet(getVertices());
        return new Simplex(getId(), newV, this.isMaximal);
    }
    
    @Override
    public String toString() {
        return getVertices().toString();
    }

    @Override
    public boolean containsFace(Simplex ot) {
        return getVertices().containsAll(ot.getVertices());
    }
    
    public boolean containsFace(Set<Integer> ot) {
        return getVertices().containsAll(ot);
    }
    
}
