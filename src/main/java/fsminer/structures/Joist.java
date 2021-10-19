package fsminer.structures;

import com.google.common.collect.Sets;
import java.util.Set;

public class Joist {
    
    private int id;
    private Set<Integer> vertices; // vertices of the simplex associated to the joist
    private Set<Integer> ids; // ids of the simplices in the joist
    private boolean isOpen; // true if it contains all the cofaces of a simplex
    
    public Joist(int id, Simplex s) {
        this.id = id;
        this.vertices = Sets.newHashSet(s.getVertices());
        this.ids = Sets.newHashSet();
        this.ids.add(s.getId());
        if (this.vertices.size() > 1 && this.vertices.size() == this.ids.size()) {
            isOpen = true;
        } else {
            this.isOpen = false;
        }
    }
    
    public void addSimplex(Simplex s) {
        this.vertices.addAll(s.getVertices());
        this.ids.add(s.getId());
        if (this.vertices.size() == this.ids.size()) {
            isOpen = true;
        }
    }
    
    public int getID() {
        return id;
    }
    
    public Set<Integer> getVertices() {
        return vertices;
    }
    
    public boolean isOpen() {
        return isOpen;
    }
    
    public boolean containsSimplex(int id) {
        return ids.contains(id);
    }
    
    public Set<Integer> getSimplexIds() {
        return ids;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Joist that = (Joist) o;
        return ((this.ids.equals(that.ids)) 
                && (this.vertices.equals(that.vertices)));
    }
    
    @Override
    public String toString() {
        return "<<" + vertices.toString() 
                + "><" + ids.toString() 
                + "><" + isOpen + ">>";
    }
    
    public void printVertices() {
        System.out.println("<<" + vertices.toString() + ">>");
    }

}
