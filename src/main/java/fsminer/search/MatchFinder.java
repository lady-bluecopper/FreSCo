package fsminer.search;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import fsminer.mis.OverlapGraph;
import fsminer.structures.Complex;
import fsminer.structures.Simplet;
import fsminer.structures.Simplex;
import fsminer.utils.Pair;
import fsminer.utils.Settings;
import fsminer.utils.StopWatch;
import fsminer.utils.Utils;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class MatchFinder {
    
    Complex complex;
    Simplet simplet;
    int minFreq;
    
    public MatchFinder(Complex complex, Simplet simplet, int minFreq) {
        this.complex = complex;
        this.simplet = simplet;
        this.minFreq = minFreq;
    }
    
    // Find all the occurrences of a pattern
    public void examine() {
        Map<Integer, Set<Integer>> images = Maps.newHashMap();
        if (simplet.getImages().isEmpty()) {
            simplet.emptyImageMap();
            return;
        }
        OverlapGraph og = new OverlapGraph(Settings.harmful);
        // order the vertices according to size of image sets
        List<Integer> ordered_vertices = Lists.newArrayList(simplet.getVertices());
        Collections.sort(ordered_vertices, (Integer e1, Integer e2)
                -> Integer.compare(simplet.getImageOf(e1).size(), simplet.getImageOf(e2).size()));
        // initial set of valid matches
        Set<Integer> initial = Sets.newHashSet();
        for (Simplex s : complex.getSimplices()) {
            if (s.getNumVertices() >= simplet.getNumVertices()) {
                initial.addAll(s.getVertices());
            }
        }
        for (int v : ordered_vertices) {
            if (simplet.getImageOf(v).size() < minFreq) {
                simplet.emptyImageMap();
                return;
            }
            Set<Integer> partialImageSet = Sets.newHashSet(initial);
            partialImageSet.addAll(images.getOrDefault(v, Sets.newHashSet()));
            Set<Integer> candidates = Sets.newHashSet(simplet.getImageOf(v));
            candidates.removeAll(partialImageSet);
            int c = 0;
            // sort vertices using a dfs
            List<Integer> vertex_ordering = Lists.newArrayList();
            vertex_ordering.add(v);
            simplet.dfs(vertex_ordering, v);
                
            for (Integer n : candidates) {
                c += 1;
                Map<Integer, Integer> M = Maps.newHashMap();
                M.put(v, n);
                if (Settings.supportMeasure.equalsIgnoreCase("mis")) {
                    // call recursive function to get all the matches
                    Map<Integer, Set<Integer>> allMatches = findAllMatches(og, M, 0, vertex_ordering);
                    // update image sets with the valid matches
                    updateImageSets(images, allMatches);
                    if (!allMatches.isEmpty()) {
                        partialImageSet.add(n);
                    }
                } else {
                    Map<Integer, Integer> match = findMatch(M, 0, vertex_ordering);
                    if (match.size() == simplet.getNumVertices()) {
                        Set<Pair<Integer, Integer>> edgeSet = Sets.newHashSet();
                        updateAndPropagateImageSets(images, match);
                        partialImageSet.add(n);
                    }
                }
                // early stop if the simplet cannot be frequent
                if (candidates.size() - c + partialImageSet.size() < minFreq) {
                    simplet.emptyImageMap();
                    return;
                }
            }
            Set<Integer> imageV = images.getOrDefault(v, Sets.newHashSet());
            imageV.addAll(partialImageSet);
            images.put(v, imageV);
        }
        simplet.setImages(images);
        if (Settings.supportMeasure.equalsIgnoreCase("mis")) {
            System.out.println("Calculating MIS");
            StopWatch watch = new StopWatch();
            watch.start();
            simplet.setFreq(og.getMISSize(false));
            System.out.println("Done in " + watch.getElapsedTimeInSec());
        }
    }

    // Find the minimum number of occurrences needed to determine if the pattern is frequent
    public void examineSingle(
            Map<Integer, Set<Integer>> parent, 
            Map<Integer, Set<Integer>> pNonCands,
            long timeout) {
        Map<Integer, Set<Integer>> images = Maps.newHashMap();
        Map<Integer, Set<Integer>> nonCands = Maps.newHashMap();
        pNonCands.entrySet().stream().forEach(e -> {
            nonCands.put(e.getKey(), Sets.newHashSet(e.getValue()));
        });
        // order the vertices according to size of image sets
        // find enough matches for each vertex
        // initialization of images
        Set<Integer> initial = Sets.newHashSet();
        for (Simplex s : complex.getSimplices()) {
            if (s.getNumVertices() >= simplet.getNumVertices()) {
                initial.addAll(s.getVertices());
            } 
        }
        for (int v : simplet.getVertices()) {
            Set<Integer> partialImageSet = Sets.newHashSet(initial);
            partialImageSet.addAll(images.getOrDefault(v, Sets.newHashSet()));
            // if we have enough matches for this vertex, we don't need to examine it
            if (partialImageSet.size() < minFreq) {
                List<Integer> ordered_vertices = Lists.newArrayList();
                ordered_vertices.add(v);
                simplet.dfs(ordered_vertices, v);
                List<Integer> candidates = Lists.newArrayList(complex.getCandVertices());
                if (!parent.isEmpty()) {
                    Utils.customSort(candidates, parent.getOrDefault(v, Collections.EMPTY_SET));
                }
                int c = partialImageSet.size();
                List<Integer> toResume = Lists.newArrayList();
                boolean resume = true;
                if (candidates.size() < minFreq) {
                    simplet.emptyImageMap();
                    return;
                }
                for (Integer n : candidates) {
                    if (partialImageSet.contains(n)) {
                        continue;
                    }
                    c += 1;
                    if (nonCands.getOrDefault(v, Sets.newHashSet()).contains(n)) {
                        continue;
                    }
                    if (complex.getNeighborsOf(n).size() < simplet.getNeighborsOf(v).size()) {
                        continue;
                    }
                    Map<Integer, Integer> M = Maps.newHashMap();
                    M.put(v, n);
                    // call recursive function to get a match
                    Map<Integer, Integer> match = findMatch(M, 0, ordered_vertices, System.currentTimeMillis(), timeout);
                    if (match.size() == simplet.getNumVertices()) {
                        // update image sets with the valid matches
                        updateAndPropagateImageSets(images, match);
                        partialImageSet.add(n);
                    } else if (match.isEmpty()) {
                        toResume.add(n);
                        c -= 1;
                    } else {
                        Set<Integer> tmp = nonCands.getOrDefault(v, Sets.newHashSet());
                        tmp.add(n);
                        nonCands.put(v, tmp);
                    }
                    // early stop if the simplet cannot be frequent
                    if (candidates.size() - c + partialImageSet.size() < minFreq) {
                        simplet.emptyImageMap();
                        return;
                    } else if (partialImageSet.size() >= minFreq) {
                        resume = false;
                        break;
                    }
                }
                if (resume) {
                    c = 0;
                    for (int n : toResume) {
                        if (partialImageSet.contains(n)) {
                            continue;
                        }
                        c += 1;
                        if (nonCands.getOrDefault(v, Sets.newHashSet()).contains(n)) {
                            continue;
                        }
                        Map<Integer, Integer> M = Maps.newHashMap();
                        M.put(v, n);
                        // call recursive function to get a match
                        Map<Integer, Integer> match = findMatch(M, 0, ordered_vertices, System.currentTimeMillis(), -1);
                        if (match.size() == simplet.getNumVertices()) {
                            // update image sets with the valid matches
                            updateAndPropagateImageSets(images, match);
                            partialImageSet.add(n);
                        } else {
                            Set<Integer> tmp = nonCands.getOrDefault(v, Sets.newHashSet());
                            tmp.add(n);
                            nonCands.put(v, tmp);
                        }
                        // early stop if the simplet cannot be frequent
                        if (toResume.size() - c + partialImageSet.size() < minFreq) {
                            simplet.emptyImageMap();
                            return;
                        } else if (partialImageSet.size() >= minFreq) {
                            break;
                        }
                    }
                }
            }
            Set<Integer> imageV = images.getOrDefault(v, Sets.newHashSet());
            for (int m : partialImageSet) {
                if (imageV.size() >= minFreq) {
                    break;
                }
                imageV.add(m);
            }
            images.put(v, imageV);
        }
        simplet.setImages(images);
        simplet.setNonCands(nonCands);
    }

    // EXACT
    private Map<Integer, Integer> findMatch(Map<Integer, Integer> M,
            int vertexID,
            List<Integer> vertexOrder) {
        // M is a valid match
        if (M.size() == vertexOrder.size()) {
            return M;
        }
        // find next vertex to examine
        while (M.containsKey(vertexOrder.get(vertexID))) {
            vertexID++;
        }
        if (vertexID > vertexOrder.size()) {
            return M;
        }
        int w = vertexOrder.get(vertexID);
        // the set of candidates is the intersection among:
        // 1. upper-bound to the image set
        // 2. vertices not already assigned to a simplet vertex
        // 3. neighbours of vertices assigned to simplex vertices that are neighbours of w 
        // 4. the assignment is valid only if it preserves the simplex memberships
        
        Set<Integer> ngbs = simplet.getNeighborsOf(w);
        Set<Integer> candidates = Sets.newHashSet(simplet.getImageOf(w));
        for (int ngb : ngbs) {
            if (M.containsKey(ngb)) {
                Set<Integer> ngbs_c = complex.getNeighborsOf(M.get(ngb));
                candidates.retainAll(ngbs_c);
            }
        }
        for (int n: candidates) {
                // the assignment is valid only if it preserves the simplex memberships
                if (satisfiesConstraints(M, w, n)) {
                    Map<Integer, Integer> newM = Maps.newHashMap(M);
                    newM.put(w, n);
                    Map<Integer, Integer> updM = findMatch(newM, vertexID + 1, vertexOrder);
                    if (updM.size() ==  vertexOrder.size()) {
                        return updM;
                    }
                }
        }
        return M;
    }
    
    // MIN-BASED
    private Map<Integer, Integer> findMatch(Map<Integer, Integer> M,
            int vertexID,
            List<Integer> vertexOrder,
            long startTime, 
            long timeout) {
        if (timeout > -1 && (System.currentTimeMillis() - startTime > timeout)) {
            return Collections.EMPTY_MAP;
        }
        // M is a valid match
        if (M.size() == vertexOrder.size()) {
            return M;
        }
        // find next vertex to examine
        while (M.containsKey(vertexOrder.get(vertexID))) {
            vertexID++;
        }
        if (vertexID > vertexOrder.size()) {
            return M;
        }
        int w = vertexOrder.get(vertexID);
        // the set of candidates is the intersection among:
        // 1. upper-bound to the image set
        // 2. vertices not already assigned to a simplet vertex
        // 3. neighbours of vertices assigned to simplex vertices that are neighbours of w 
        // 4. the assignment is valid only if it preserves the simplex memberships
        Set<Integer> ngbs = simplet.getNeighborsOf(w);
        Set<Integer> candidates = Sets.newHashSet();
        boolean start = true;
        for (int ngb : ngbs) {
            if (M.containsKey(ngb)) {
                Set<Integer> ngbs_c = complex.getNeighborsOf(M.get(ngb));
                if (start) {
                    start = false;
                    candidates.addAll(ngbs_c);
                } else {
                    candidates.retainAll(ngbs_c);
                }
            }
        }
        if (start) {
            candidates.addAll(complex.getCandVertices());
        }
        for (int n : candidates) {    
            if (satisfiesConstraints(M, w, n)) {
                Map<Integer, Integer> newM = Maps.newHashMap(M);
                newM.put(w, n);
                Map<Integer, Integer> updM = findMatch(newM, vertexID + 1, vertexOrder, startTime, timeout);
                if (updM.size() ==  vertexOrder.size() || updM.isEmpty()) {
                    return updM;
                } 
            } else if (timeout > -1 && (System.currentTimeMillis() - startTime > timeout)) {
                return Collections.EMPTY_MAP;
            }
        }
        return M;
    }
    
    private Map<Integer, Set<Integer>> findAllMatches(OverlapGraph og,
            Map<Integer, Integer> M,
            int vertexID,
            List<Integer> vertexOrder) {

        Map<Integer, Set<Integer>> allMatches = Maps.newHashMap();
        if (M.size() == vertexOrder.size()) {
            og.add(M);
            for (int k : M.keySet()) {
                Set<Integer> tmp = Sets.newHashSet();
                tmp.add(M.get(k));
                allMatches.put(k, tmp);
            }
            return allMatches;
        }
        // find next vertex to examine
        while (M.containsKey(vertexOrder.get(vertexID))) {
            vertexID++;
        }
        if (vertexID > vertexOrder.size()) {
            return allMatches;
        }
        int w = vertexOrder.get(vertexID);
        final int vID = vertexID;
        // the set of candidates is the intersection among:
        // 1. upper-bound to the image set
        // 2. vertices not already assigned to a simplet vertex
        // 3. neighbours of vertices assigned to simplex vertices that are neighbours of w 
        simplet.getImageOf(w).stream()
                // the assignment is valid only if it preserves the simplex memberships
                .filter(n -> satisfiesConstraints(M, w, n))
                .forEach(n -> {
                    Map<Integer, Integer> newM = Maps.newHashMap(M);
                    newM.put(w, n);
                    Map<Integer, Set<Integer>> currMatches = findAllMatches(og, newM, vID + 1, 
                            vertexOrder);
                    for (int k : currMatches.keySet()) {
                        Set<Integer> tmp = allMatches.getOrDefault(k, Sets.newHashSet());
                        tmp.addAll(currMatches.get(k));
                        allMatches.put(k, tmp);
                    }
                }); 
        return allMatches;
    }

    private boolean satisfiesConstraints(Map<Integer, Integer> M, int w, int n) {
        if (complex.getNeighborsOf(n).size() < simplet.getNeighborsOf(w).size() ||
                M.entrySet().stream().anyMatch(e -> (e.getValue() == n) || 
                (simplet.areNeighbors(e.getKey(), w) && !complex.getNeighborsOf(e.getValue()).contains(n)))) {
            return false;
        }
        for (Simplex s : simplet.getAllHDSimplices()) {
            // for all the simplices containing w
            if (s.contains(w) && s.getNumVertices() > 2) {
                List<Integer> simplex = Lists.newArrayList();
                simplex.add(n);
                M.keySet().stream()
                        .filter(v -> (s.contains(v)))
                        .forEach(v -> simplex.add(M.get(v)));
                //the complex vertices matched to simplet vertices in s must form a simplex in the complex 
                if (!complex.contains(simplex)) {
                    return false;
                }
            }
        }
        return true;
    }

    private void updateImageSets(Map<Integer, Set<Integer>> images, Map<Integer, Set<Integer>> matches) {
        matches.entrySet().stream().forEach(e -> {
            Set<Integer> tmp = images.getOrDefault(e.getKey(), Sets.newHashSet());
            tmp.addAll(e.getValue());
            images.put(e.getKey(), tmp);
        });
    }

    private void updateAndPropagateImageSets(Map<Integer, Set<Integer>> images, Map<Integer, Integer> match) {
        match.entrySet().stream().forEach(e -> {
            for (int ot : simplet.getOrbitOf(e.getKey())) {
                Set<Integer> tmp = images.getOrDefault(ot, Sets.newHashSet());
                tmp.add(e.getValue());
                images.put(ot, tmp);
            }
        });
    }
    
}
