package fsminer.structures;

import com.google.common.collect.Sets;
import java.util.Set;

public abstract class Simpl extends Object {
    
    private int id;
    private Set<Integer> vertices;
    
    public Simpl() {
        this.vertices = Sets.newHashSet();
    }
    
    public Simpl(int id) {
        this.id = id;
        this.vertices = Sets.newHashSet();
    }
    
    public Simpl(int id, Set<Integer> vertices) {
        this.id = id;
        this.vertices = vertices;
    }
    
    public abstract Simpl copy();
    
    public Set<Integer> getVertices() {
        return vertices;
    }
    
    public void setVertices(Set<Integer> vertices) {
        this.vertices = vertices;
    }
    
    public void addVertex(int id) {
        vertices.add(id);
    }
    
    public void addVertices(Set<Integer> vertices) {
        this.vertices.addAll(vertices);
    }
    
    public boolean contains(int v) {
        return vertices.contains(v);
    }
    
    public boolean containsAll(Set<Integer> vertices) {
        return this.vertices.containsAll(vertices);
    }
    
    public abstract boolean containsFace(Simplex ot);
    
    public abstract boolean containsFace(Set<Integer> ot);
    
    public int getNumVertices() {
        return vertices.size();
    }
    
    public void setId(int id) {
        this.id = id;
    }
    
    public int getId() {
        return id;
    }
    
}
