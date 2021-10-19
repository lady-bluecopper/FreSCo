package fsminer.structures;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Complex {
    
    private Set<Integer> vertices;
    private Set<Simplex> simplices;
    private Map<Integer, Set<Integer>> vertexMap;
    private Map<Integer, Set<Integer>> neighbours;
    
    public Complex(List<Simplex> simplices) {
        this.vertices = Sets.newHashSet();
        this.simplices = Sets.newHashSet();
        this.vertexMap = Maps.newHashMap();
        this.neighbours = Maps.newHashMap();
        initialize(simplices);
    }
    
    private void initialize(List<Simplex> simplices) {
        simplices.stream().forEach(simplex -> {
            this.vertices.addAll(simplex.getVertices());
            this.simplices.add(simplex);
            for (int v : simplex.getVertices()) {
                Set<Integer> memb = this.vertexMap.getOrDefault(v, Sets.newHashSet());
                memb.add(simplex.getId());
                this.vertexMap.put(v, memb);
            }
            if (simplex.getNumVertices() > 1) {
                simplex.getVertices().stream().forEach(v -> {
                    Set<Integer> tmp2 = neighbours.getOrDefault(v, Sets.newHashSet());
                    tmp2.addAll(simplex.getVertices());
                    tmp2.remove(v);
                    neighbours.put(v, tmp2);
                });
            }
        });
    }
    
    public Set<Integer> getNeighborsOf(int v) {
        return neighbours.getOrDefault(v, Sets.newHashSet());
    }
    
    public Set<Integer> getVertices() {
        return vertices;
    }
    
    public Set<Integer> getCandVertices() {
        return neighbours.keySet();
    }
    
    public int getNumVertices() {
        return vertices.size();
    }
    
    public int getNumberOfSimplices() {
        return simplices.size();
    }
    
    public Set<Simplex> getSimplices() {
        return simplices;
    }
    
    // checks ifx a simplex belongs to the complex
    public boolean contains(List<Integer> simplex) {
        Set<Integer> memb = Sets.newHashSet(vertexMap.getOrDefault(simplex.get(0), Sets.newHashSet()));
        for (int i = 1; i < simplex.size(); i++) {
            memb.retainAll(vertexMap.getOrDefault(simplex.get(i), Sets.newHashSet()));
            if (memb.isEmpty()) {
                return false;
            }
        }
        return !memb.isEmpty();
    }

}
