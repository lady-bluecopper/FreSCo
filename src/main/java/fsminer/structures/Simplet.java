package fsminer.structures;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import fi.tkk.ics.jbliss.pattern.JBlissPattern;
import fsminer.utils.Pair;
import fsminer.utils.Utils;
import static fsminer.utils.Utils.generate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class Simplet extends Simpl {

    private Map<Integer, List<Simplex>> simplices; // simplices in the simplet: dimension -> list of simplices with that dimension
    private Map<Integer, Set<Integer>> neighbours; // neighbours of each vertex: VID -> vertices belonging to a common simplex;
    private Map<String, List<Integer>> cofaceSimplexMap; // for each coface, it gives the positions in the simplex list of the simplices with that coface
    private HashBasedTable<Integer, Integer, Set<Integer>> simplexNeighbours; //(dimension, simplex pos) -> positions in the simplex list of simplices sharing a coface
    private Map<Integer, Set<Integer>> images; // image sets associated to the vertices: VID -> complex vertices mapped to VID
    private Map<Integer, Set<Integer>> nonCands; // mappings not valid found during the single match search
    private JBlissPattern canForm; // canonical form of the simplet
    private JBlissPattern graphProj; // canonical form of the underlying graph
    private Map<Integer, Set<Integer>> orbitRepresentatives; // orbit representatives of the simplet
    private Map<Integer, Integer> orbitMemberships; // VID -> orbit representative
    private double freq;
    private int dimension;
    private int incrId;

    public Simplet(int id) {
        super(id);
        this.simplices = Maps.newHashMap();
        this.neighbours = Maps.newHashMap();
        this.cofaceSimplexMap = Maps.newHashMap();
        this.simplexNeighbours = HashBasedTable.create();
        this.images = Maps.newHashMap();
        this.nonCands = Maps.newHashMap();
        this.dimension = 0;
        this.incrId = 1;
        initializeStructures();
    }

    public Simplet(int id, Simplet s, boolean allMatches) {
        super(id, Sets.newHashSet(s.getVertices()));
        if (allMatches) {
            this.images = Utils.copyMap(s.getImages());
        } else {
            this.images = Maps.newHashMap();
        }
        initializeStructures();
        initializeFromSimplet(s);
    }

    private void initializeStructures() {
        this.canForm = null;
        this.graphProj = null;
        this.orbitRepresentatives = Maps.newHashMap();
        this.orbitMemberships = Maps.newHashMap();
    }

    private void initializeFromSimplet(Simplet s) {
        this.simplices = Maps.newHashMap();
        s.getSimplexMap().entrySet()
                .forEach(e -> this.simplices.put(e.getKey(), (List<Simplex>) Utils.deepCopy(e.getValue())));
        this.neighbours = Utils.copyMap(s.getNeighbors());
        this.cofaceSimplexMap = Maps.newHashMap();
        s.getCofaceIndex().entrySet()
                .forEach(e -> this.cofaceSimplexMap.put(e.getKey(), Lists.newArrayList(e.getValue())));
        this.simplexNeighbours = HashBasedTable.create();
        s.getSimplexNeighbours().cellSet().forEach(cell
                -> this.simplexNeighbours.put(cell.getRowKey(), cell.getColumnKey(), Sets.newHashSet(cell.getValue())));
        this.dimension = s.getDimension();
        this.incrId = s.getIncrId();
    }

    // adds vertex and corresponding 0-simplex
    public void add0Simplex(int v, int uID) {
        addVertex(v);
        Simplex zeroSimpl = new Simplex(incrId, v);
        incrId++;
        List<Simplex> tmp = simplices.getOrDefault(1, Lists.newArrayList());
        tmp.add(zeroSimpl);
        simplices.put(1, tmp);
        if (zeroSimpl.getNumVertices() > dimension) {
            dimension = zeroSimpl.getNumVertices();
        }
        // add vertex in coface index and update neighbours
        List<Integer> tmpSimpList = cofaceSimplexMap.getOrDefault("-", Lists.newArrayList());
        Set<Integer> tmpSimpNBS = Sets.newHashSet(tmpSimpList);
        tmpSimpNBS.remove(uID);
        simplexNeighbours.put(1, simplices.get(1).size() - 1, Sets.newHashSet(tmpSimpList));
        for (int ot : tmpSimpNBS) {
            Set<Integer> tmpOTSimpNBS = simplexNeighbours.get(1, ot);
            tmpOTSimpNBS.add(simplices.get(1).size() - 1);
            simplexNeighbours.put(1, ot, tmpOTSimpNBS);
        }
        tmpSimpList.add(simplices.get(1).size() - 1);
        this.cofaceSimplexMap.put("-", tmpSimpList);
    }

    // adds 1-simplex and 0-simplex of new vertex
    public void add1Simplex(int newV, int oldV, Simplex edge) {
        add0Simplex(newV, oldV);
        addkSimplex(edge);
        incrId++;
    }

    // adds k-simplex
    public void addkSimplex(Simplex s) {
        int simplexDim = s.getNumVertices();
        // update simplex map
        addVertices(s.getVertices());
        List<Simplex> tmp = simplices.getOrDefault(simplexDim, Lists.newArrayList());
        tmp.add(s);
        simplices.put(simplexDim, tmp);
        // update neighbour map
        for (int v : s.getVertices()) {
            Set<Integer> ngb = Sets.newHashSet(s.getVertices());
            ngb.remove(v);
            Set<Integer> tmp2 = neighbours.getOrDefault(v, Sets.newHashSet());
            tmp2.addAll(ngb);
            neighbours.put(v, tmp2);
        }
        // update dimension
        if (simplexDim > dimension) {
            dimension = simplexDim;
        }
        // set sub-simplices to maximal=false
        simplices.getOrDefault(simplexDim - 1, Lists.newArrayList())
                .stream()
                .filter(sub -> sub.isMaximal() && s.containsFace(sub))
                .forEach(sub -> sub.setMaximal(false));
    }
    
    public void updateCofaceMap(Simplex s) {
        // update coface map
        int simplexDim = s.getNumVertices();
        List<String> cofaces = generateCofaces(s);
        int idx = simplices.get(simplexDim).size() - 1;
        for (String code : cofaces) {
            if (!simplexNeighbours.contains(simplexDim, idx)) {
                simplexNeighbours.put(simplexDim, idx, Sets.newHashSet());
            }
            Set<Integer> newNeigh = Sets.newHashSet(cofaceSimplexMap.getOrDefault(code, Lists.newArrayList()));
            if (!newNeigh.isEmpty()) {
                Set<Integer> tmpNeigh = simplexNeighbours.get(simplexDim, idx);
                tmpNeigh.addAll(newNeigh);
                simplexNeighbours.put(simplexDim, idx, tmpNeigh);
            }
            for (int ot : newNeigh) {
                Set<Integer> tmpNeighOt = simplexNeighbours.get(simplexDim, ot);
                tmpNeighOt.add(idx);
                simplexNeighbours.put(simplexDim, ot, tmpNeighOt);
            }
            List<Integer> tmp2 = cofaceSimplexMap.getOrDefault(code, Lists.newArrayList());
            tmp2.add(idx);
            cofaceSimplexMap.put(code, tmp2);
        }
    }
    
    public Set<Integer> getNeighborsOf(int v) {
        return neighbours.get(v);
    }

    public Map<Integer, Set<Integer>> getNeighbors() {
        return neighbours;
    }

    public HashBasedTable<Integer, Integer, Set<Integer>> getSimplexNeighbours() {
        return simplexNeighbours;
    }

    public boolean areNeighbors(int u, int v) {
        return neighbours.get(u).contains(v) || neighbours.get(v).contains(u);
    }

    public void setNeighbours(Map<Integer, Set<Integer>> neighbours) {
        this.neighbours = neighbours;
    }

    public List<Simplex> getSimplicesOfDim(int dim) {
        return simplices.get(dim);
    }

    public Set<Simplex> getAllHDSimplices() {
        return simplices.values().stream()
                .flatMap(s -> s.stream().filter(simp -> simp.isMaximal())).collect(Collectors.toSet());
    }

    public Set<Simplex> getAllSimplices() {
        return simplices.values().stream().flatMap(s -> s.stream()).collect(Collectors.toSet());
    }

    public Map<Integer, List<Simplex>> getSimplexMap() {
        return simplices;
    }

    public void insertSimplices(int dim, List<Simplex> simplices) {
        this.simplices.put(dim, simplices);
    }

    public int getNumSimplices() {
        return simplices.values().stream().mapToInt(s -> s.size()).sum();
    }
    
    @Override
    public boolean containsFace(Simplex ot) {
        return simplices.getOrDefault(ot.getNumVertices(), Lists.newArrayList()).stream().anyMatch(s -> s.containsFace(ot));
    }

    @Override
    public boolean containsFace(Set<Integer> ot) {
        return simplices.getOrDefault(ot.size(), Lists.newArrayList()).stream().anyMatch(s -> s.containsFace(ot));
    }

    public int getIncrId() {
        return incrId++;
    }

    public void setIncrId(int incrId) {
        this.incrId = incrId;
    }

    public void setImages(Map<Integer, Set<Integer>> ubs) {
        if (ubs.isEmpty()) {
            this.images = Collections.EMPTY_MAP;
            return;
        }
        ubs.entrySet().forEach(e -> {
            this.images.put(e.getKey(), Sets.newHashSet(e.getValue()));
        });
    }
    
    // used when the simplet cannot frequent
    public void emptyImageMap() {
        this.images = Collections.EMPTY_MAP;
    }

    public void addUBImage(int v, Set<Integer> image) {
        images.put(v, Sets.newHashSet(image));
    }

    public Map<Integer, Set<Integer>> getImages() {
        return images;
    }

    public Set<Integer> getImageOf(int v) {
        return images.get(v);
    }
    
    public Map<Integer, Set<Integer>> getNonCands() {
        return nonCands;
    }
    
    public void setNonCands(Map<Integer, Set<Integer>> nonCands) {
        this.nonCands = nonCands;
    }

    public double computeFrequency(String supportMeasure) {
        if (images.isEmpty()) {
            return 0;
        }
        if (supportMeasure.equalsIgnoreCase("mni")) {
            freq = images.values().stream().mapToInt(s -> s.size()).min().orElse(0);
        }
        return freq;
    }

    public double getFreq() {
        return freq;
    }

    public void setFreq(double freq) {
        this.freq = freq;
    }

    public int getDimension() {
        return dimension;
    }

    public void setDimension(int dimension) {
        this.dimension = dimension;
    }

    private List<String> generateCofaces(Simplex s) {
        List<Integer> V = Lists.newArrayList(s.getVertices());
        List<int[]> combinations = generate(s.getNumVertices(), s.getNumVertices() - 1);
        return combinations.stream()
                .map(combination -> {
                    int[] subset = new int[combination.length];
                    for (int i = 0; i < combination.length; i++) {
                        subset[i] = V.get(combination[i]);
                    }
                    Arrays.sort(subset);
                    return Arrays.toString(subset);
                })
                .collect(Collectors.toList());
    }

    public Map<String, List<Integer>> getCofaceIndex() {
        return cofaceSimplexMap;
    }

    // compute canonical form of the simplet
    public JBlissPattern computeCanonicalForm() {
        JBlissPattern p = new JBlissPattern(getAllHDSimplices(), true);
        p.turnCanonical();
        orbitRepresentatives = p.getOrbitRepresentatives();
//        printSimplet();
//        System.out.println("OR: " + orbitRepresentatives.toString());
        orbitRepresentatives.entrySet().stream()
                .filter(e -> !e.getValue().isEmpty())
                .forEach(e -> e.getValue().stream().forEach(v -> orbitMemberships.put(v, e.getKey())));
        return p;
    }

    // compute canonical form of underlying graph
    public JBlissPattern computeGraphCanonicalForm() {
        JBlissPattern p = new JBlissPattern(getAllHDSimplices(), false);
        p.turnCanonical();
        return p;
    }

    public Set<Integer> getOrbitRepresentatives() {
        if (orbitRepresentatives.isEmpty()) {
            computeCanonicalForm();
        }
        return orbitRepresentatives.keySet();
    }

    public Set<Integer> getOrbitOf(int v) {
        if (orbitRepresentatives.isEmpty()) {
            computeCanonicalForm();
        }
        return orbitRepresentatives.get(orbitMemberships.get(v));
    }

    public List<Pair<Set<Integer>, Set<Integer>>> validateJoists(List<Simplex> C, int level) {
        Map<Integer, Set<Integer>> inv_idx = Maps.newHashMap();
        for (int idx = 0; idx < C.size(); idx++) {
            for (int v : C.get(idx).getVertices()) {
                Set<Integer> tmp = inv_idx.getOrDefault(v, Sets.newHashSet());
                tmp.add(idx);
                inv_idx.put(v, tmp);
            }
        }
        List<Pair<Set<Integer>, Set<Integer>>> joists = Lists.newArrayList();
        for (int s : simplexNeighbours.row(level).keySet()) {
            // vertices of all the nbs of simplex s
            Set<Integer> v_set = Sets.newHashSet();
            simplexNeighbours.get(level, s).forEach(x -> v_set.addAll(C.get(x).getVertices()));
            v_set.removeAll(C.get(s).getVertices());
            int maxVID = C.get(s).getVertices().stream().mapToInt(x -> x).max().getAsInt();
            v_set.stream()
                    .filter(v -> v > maxVID)
                    .forEach(v -> {
                        Set<Integer> cand_joist = Sets.newHashSet(inv_idx.get(v));
                        cand_joist.retainAll(simplexNeighbours.get(level, s));
                        if (cand_joist.size() == C.get(s).getNumVertices()) {
                            Set<Integer> joist = Sets.newHashSet(C.get(s).getVertices());
                            joist.add(v);
                            cand_joist.add(s);
                            joists.add(new Pair<>(joist, cand_joist));
                        }
                    });
        }
        return joists;
    }
    
    // update simplex neighbours map used to find the joists
    public void updateSimplexNeighbours(Set<Integer> simplexIDs, int level) {
        Map<Integer, Set<Integer>> tmpMap = simplexNeighbours.row(level);
        for (int sID : simplexIDs) {
            Set<Integer> tmpNeigh = tmpMap.get(sID);
            tmpNeigh.removeAll(simplexIDs);
            simplexNeighbours.put(level, sID, tmpNeigh);
        }
    }

    public JBlissPattern getCanonicalForm() {
        if (canForm == null) {
            canForm = computeCanonicalForm();
        }
        return canForm;
    }

    public JBlissPattern getGraphProj() {
        if (graphProj == null) {
            graphProj = computeGraphCanonicalForm();
        }
        return graphProj;
    }

    public boolean equals(Simplet o) {
        return this.getCanonicalForm().equals(o.getCanonicalForm());
    }

    // creates simplex-count and vertex-count sequences; 
    // used to store the simplices in the examined map
    public Pair<int[], int[]> computeFingerPrint() {
        int[] simplexCounts = new int[dimension];
        int[] vertexCounts = new int[getNumVertices()];
        simplices.entrySet().stream().forEach(e -> {
            List<Simplex> maximal = e.getValue().stream()
                    .filter(s -> s.isMaximal())
                    .collect(Collectors.toList());
            simplexCounts[e.getKey() - 1] += maximal.size();
            maximal.forEach(simpl -> simpl.getVertices().forEach(v -> vertexCounts[v]++));
        });
        Arrays.sort(vertexCounts);
        return new Pair<>(simplexCounts, vertexCounts);
    }
    
    public List<Integer> dfs(List<Integer> visited, int v) {
        for (int w : getNeighborsOf(v)) {
            if (!visited.contains(w)) {
                visited.add(w);
                dfs(visited, w);
            }
        }
        return visited;
    }

    @Override
    public String toString() {
        String out = String.valueOf(freq);
        for (Simplex s : getAllHDSimplices()) {
            out += "-" + s.toString();
        }
        return out;
    }

    public void printSimplet() {
        for (Simplex s : getAllHDSimplices()) {
            System.out.print(s.toString() + "-");
        }
        System.out.println("D" + dimension);
    }

    @Override
    public Simpl copy() {
        Simplet s = new Simplet(getId());
        s.setVertices(Sets.newHashSet(getVertices()));
        s.setImages(getImages());
        getSimplexMap().entrySet()
                .forEach(e -> s.insertSimplices(e.getKey(), (List<Simplex>) Utils.deepCopy(e.getValue())));
        s.setIncrId(getIncrId());
        s.setDimension(getDimension());
        s.setNeighbours(Utils.copyMap(getNeighbors()));
        return s;
    }

    // used when computation is orbit based, to set images of other vertices in the same orbit
//    public void updateAndPropagate(Map<Integer, Set<Integer>> I) {
//        I.entrySet().forEach(e -> {
//            this.images.put(e.getKey(), Sets.newHashSet(e.getValue()));
//            Set<Integer> toPropagate = Sets.newHashSet();
//            if (orbitRepresentatives.containsKey(e.getKey())) {
//                toPropagate = orbitRepresentatives.get(e.getKey());
//                // this can happen when we applied rule2 and we examined all the vertices of the new simplex added    
//            } else {
//                toPropagate.add(orbitMemberships.get(e.getKey()));
//                toPropagate.addAll(orbitRepresentatives.get(orbitMemberships.get(e.getKey()))
//                        .stream()
//                        .filter(ot -> ot != e.getKey())
//                        .collect(Collectors.toSet()));
//            }
//            for (int ot : toPropagate) {
//                this.images.put(ot, Sets.newHashSet(e.getValue()));
//            }
//        });
//    }
//    
        // get open joists that can be filled
//    public List<Joist> getOpenJoists() {
//        return simplices.entrySet().stream()
//                .filter(e -> e.getValue().size() >= e.getKey() + 1)
//                .flatMap(e -> extractJoists(e.getValue()).stream())
//                .collect(Collectors.toList());
//    }

//    private List<Joist> extractJoists(List<Simplex> cands) {
//        // VID -> ids of joists containing VID
//        Map<Integer, List<Integer>> joistMapping = Maps.newHashMap();
//        // joist ID -> joist
//        Map<Integer, Joist> joists = Maps.newHashMap();
//        for (int i = 0; i < cands.size(); i++) {
//            for (int j = i + 1; j < cands.size(); j++) {
//                // if the two simplices share all but one vertex and 
//                // the larger simplex does not belong to the simplet
//                if (canFormAJoist(cands.get(i), cands.get(j))) {
//                    insertSimplexInJoist(cands.get(j), cands.get(i), joistMapping, joists);
//                }
//            }
//        }
//        // return only the open joists
//        return joists.values().stream().filter(j -> j.isOpen()).collect(Collectors.toList());
//    }

//    private boolean canFormAJoist(Simplex first, Simplex second) {
//        Set<Integer> firstVertices = Sets.newHashSet(first.getVertices());
//        firstVertices.retainAll(second.getVertices());
//        if (firstVertices.size() == first.getNumVertices() - 1) {
//            Set<Integer> union = Sets.newHashSet(first.getVertices());
//            union.addAll(second.getVertices());
//            return simplices.getOrDefault(union.size(), Lists.newArrayList())
//                    .stream()
//                    .noneMatch(ot -> ot.containsAll(union));
//        }
//        return false;
//    }

//    private void insertSimplexInJoist(Simplex ot, Simplex s,
//            Map<Integer, List<Integer>> joistMapping,
//            Map<Integer, Joist> joists) {
//        // check if ot can be added to an existing joist; otherwise create a new joist
//        for (int jID : joistMapping.getOrDefault(s.getId(), Lists.newArrayList())) {
//            if (!joists.get(jID).isOpen()) {
//                Set<Integer> jVertices = Sets.newHashSet(joists.get(jID).getVertices());
//                jVertices.removeAll(ot.getVertices());
//                // if the joist shares all but one vertex with ot, ot can be added to the joist 
//                if (jVertices.size() == 1) {
//                    joists.get(jID).addSimplex(ot);
//                    List<Integer> tmp = joistMapping.getOrDefault(ot.getId(), Lists.newArrayList());
//                    tmp.add(jID);
//                    joistMapping.put(ot.getId(), tmp);
//                    return;
//                }
//            }
//        }
//        // create new joist
//        int jID = joists.size();
//        Joist newJoist = new Joist(jID, s);
//        newJoist.addSimplex(ot);
//        joists.put(jID, newJoist);
//        List<Integer> tmp = joistMapping.getOrDefault(ot.getId(), Lists.newArrayList());
//        tmp.add(jID);
//        joistMapping.put(ot.getId(), tmp);
//        tmp = joistMapping.getOrDefault(s.getId(), Lists.newArrayList());
//        tmp.add(jID);
//        joistMapping.put(s.getId(), tmp);
//    }
}
