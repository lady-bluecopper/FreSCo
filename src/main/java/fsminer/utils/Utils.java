package fsminer.utils;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import fsminer.structures.Simpl;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Utils {
    
    public static Collection<? extends Simpl> deepCopy(Collection<? extends Simpl> o) {
        if (o instanceof Set) {
            Set<Simpl> newSet = Sets.newHashSet();
            o.stream().forEach(e -> newSet.add(e.copy()));
            return newSet;
        } else if (o instanceof List) {
            List<Simpl> newList = Lists.newArrayList();
            o.stream().forEach(e -> newList.add(e.copy()));
            return newList;
        } else {
            throw new UnsupportedOperationException();
        }
    }
    
    public static Map<Integer, Set<Integer>> copyMap(Map<Integer, Set<Integer>> map) {
        Map<Integer, Set<Integer>> newMap = Maps.newHashMap();
        map.entrySet().stream().forEach(e -> newMap.put(e.getKey(), Sets.newHashSet(e.getValue())));
        return newMap;
    }
    
    private static void helper(List<int[]> combinations, int data[], int start, int end, int index) {
        if (index == data.length) {
            int[] combination = data.clone();
            combinations.add(combination);
        } else if (start <= end) {
            data[index] = start;
            helper(combinations, data, start + 1, end, index + 1);
            helper(combinations, data, start + 1, end, index);
        }
    }

    public static List<int[]> generate(int n, int r) {
        List<int[]> combinations = Lists.newArrayList();
        helper(combinations, new int[r], 0, n - 1, 0);
        return combinations;
    }
    
    public static void customSort(List<Integer> cands, Set<Integer> image) {
        if (image.isEmpty()) {
            return;
        }
        Collections.sort(cands, (Integer o1, Integer o2) -> {
            if (image.contains(o1)) {
                if (image.contains(o2)) {
                    return 0;
                }
                return -1;
            } else {
                if (image.contains(o2)) {
                    return 1;
                }
                return Integer.compare(o1, o2);
            }
        });
    }
    
}
