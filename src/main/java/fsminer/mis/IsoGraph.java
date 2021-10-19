package fsminer.mis;

import com.google.common.collect.Sets;
import fi.tkk.ics.jbliss.pattern.Edge;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.jgrapht.alg.interfaces.VertexCoverAlgorithm.VertexCover;
import org.jgrapht.graph.SimpleGraph;
import org.jgrapht.alg.vertexcover.RecursiveExactVCImpl;
import org.jgrapht.alg.independentset.ChordalGraphIndependentSetFinder;

public class IsoGraph {

    SimpleGraph g;

    public IsoGraph() {
        g = new SimpleGraph<>(Edge.class);
        g.addVertex(1);
        g.addVertex(2);
        g.addVertex(3);
        g.addVertex(4);
        g.addVertex(5);
        g.addVertex(6);
        g.addEdge(1, 2);
        g.addEdge(2, 3);
        g.addEdge(3, 4);
        g.addEdge(5, 6);
    }

    public IsoGraph(List<Map<Integer, Integer>> isomorphisms) {
        g = new SimpleGraph<>(Edge.class);

    }

    public Set<Integer> findMIS() {
        RecursiveExactVCImpl vc = new RecursiveExactVCImpl(g);
        ChordalGraphIndependentSetFinder is = new ChordalGraphIndependentSetFinder(g);
        VertexCover<Integer> vertexCover = vc.getVertexCover();
        System.out.println(vertexCover.toString() + " -- " + is.getIndependentSet().toString());
        Set<Integer> vertices = Sets.newHashSet(g.vertexSet());
        for (int v : vertexCover) {
            vertices.remove(v);
        }
        return vertices;
    }

    public static void main(String[] args) {
        IsoGraph mis = new IsoGraph();
        System.out.println(mis.findMIS().toString());
    }

}