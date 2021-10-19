package fsminer.search;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import fsminer.structures.Complex;
import fsminer.structures.Simplet;
import fsminer.structures.Simplex;
import fsminer.utils.Pair;
import fsminer.utils.Settings;
import fsminer.utils.StopWatch;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class Miner {

    private int incrId; // simplets generated
    private HashBasedTable<String, String, Set<Simplet>> examined; // simplets examined
    private List<Pair<String, Integer>> occMap;

    public Miner() {
        this.incrId = 0;
        this.examined = HashBasedTable.create();
        this.occMap = Lists.newArrayList();
    }

    public List mine(Complex complex, int minFreq, int minSize, int maxSize, boolean limited, long timeout) {
        // create 0-simplex
        Simplet simplet = new Simplet(incrId);
        simplet.add0Simplex(0, -1);
        incrId++;
        Map<Integer, Set<Integer>> images = Maps.newHashMap();
        images.put(0, complex.getVertices());
        simplet.setImages(images);
        // start extension
        List FS = extend(complex, simplet, minFreq, minSize, maxSize, limited, timeout);
        return FS;
    }

    private List<Simplet> extend(Complex complex, Simplet simplet,
            int minFreq, int minSize, int maxSize, boolean limited, long timeout) {
        StopWatch watch = new StopWatch();
        watch.start();
        List<Simplet> extensions = Lists.newArrayList();
        List frequents = Lists.newArrayList();
        int u = simplet.getNumVertices();
        // extend only if the number of vertices in the simplet is below the max size threshold
        if (u < maxSize) {
            // add 1-simplex in each possible position
            for (int v = 0; v < u; v++) {
                Simplet ext = new Simplet(incrId, simplet, Settings.allMatches);
                Simplex simplex = new Simplex(ext.getIncrId(), Sets.newHashSet(u, v), true);
                ext.add1Simplex(u, v, simplex);
                if (!hasBeenExamined(ext)) {
                    ext.addUBImage(u, complex.getCandVertices());
                    ext.updateCofaceMap(simplex);
                    // not examined, so it can be added to the extension set
                    extensions.add(ext);
                    incrId++;
                }
            }
        }
        simplet.getSimplexMap()
                .entrySet()
                .stream()
                .forEach(entry -> {
                    List<Pair<Set<Integer>, Set<Integer>>> joists = simplet.validateJoists(entry.getValue(), entry.getKey());
                    for (Pair<Set<Integer>, Set<Integer>> joist : joists) {
                        Simplet ext = new Simplet(incrId, simplet, Settings.allMatches);
                        Simplex simplex = new Simplex(ext.getIncrId(), joist.getA(), true);
                        ext.addkSimplex(simplex);
                        if (!hasBeenExamined(ext)) {
                            ext.updateCofaceMap(simplex);
                            ext.updateSimplexNeighbours(joist.getB(), entry.getKey());
                            // not examined, so it can be added to the extension set
                            extensions.add(ext);
                            incrId++;
                        }
                    }
                });
        
        extensions.parallelStream().forEach(ext -> {
            MatchFinder matcher = new MatchFinder(complex, ext, minFreq);
            if (Settings.allMatches) {
                matcher.examine();
            } else {
                matcher.examineSingle(simplet.getImages(), simplet.getNonCands(), timeout);
            }
            ext.computeFrequency(Settings.supportMeasure);
        });
        extensions.stream().forEach(ext -> {
            if (ext.getFreq() >= minFreq) {
                if (Settings.storeOccMap) {
                    Set<Integer> vp = ext.getImages().values().stream().flatMap(v -> v.stream()).collect(Collectors.toSet());
                    for (int v : vp) {
                        occMap.add(new Pair<String, Integer>(ext.toString(), v));
                    }
                }
                // the simplet is added to the output only if the dimension > min dimension threshold
                if (ext.getDimension() >= minSize) {
                    if (limited) {
                        frequents.add(ext.toString());
                    } else {
                        frequents.add(ext);
                    }
                }
                // the simplet is frequent, and so we extend it
                frequents.addAll(extend(complex, ext, minFreq, minSize, maxSize, limited, timeout));
            }
        });
        if (limited) {
            simplet.emptyImageMap();
            simplet.setNonCands(Collections.EMPTY_MAP);
        }
        return frequents;
    }

    private boolean hasBeenExamined(Simplet s) {
        Pair<int[], int[]> p = s.computeFingerPrint();
        String hashCodeA = Arrays.toString(p.getA());
        String hashCodeB = Arrays.toString(p.getB());
        if (examined.contains(hashCodeB, hashCodeA)) {
            if (examined.get(hashCodeB, hashCodeA).stream()
                    .anyMatch(other -> (s.getDimension() == other.getDimension()
                    // check number of simplices
                    && s.getNumSimplices() == other.getNumSimplices()
                    // check graph projection
                    && s.getGraphProj().equals(other.getGraphProj())
                    // check simplet canonical form
                    && s.getCanonicalForm().equals(other.getCanonicalForm())))) {
                return true;
            } // check dimension
        } else {
            examined.put(hashCodeB, hashCodeA, Sets.newHashSet());
        }
        Set<Simplet> tmp = examined.get(hashCodeB, hashCodeA);
        tmp.add(s);
        examined.put(hashCodeB, hashCodeA, tmp);
        return false;
    }
    
    public List<Pair<String, Integer>> getOccMap() {
        return occMap;
    }
}
