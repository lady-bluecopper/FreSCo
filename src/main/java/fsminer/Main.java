package fsminer;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import fsminer.search.Miner;
import fsminer.structures.Complex;
import fsminer.structures.Simplex;
import fsminer.utils.CMDLParser;
import fsminer.utils.Pair;
import fsminer.utils.Settings;
import fsminer.utils.StopWatch;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Set;

public class Main {
    
    static StopWatch watch;

    public static void main(String[] args) throws Exception {
        //parse the command line arguments
        CMDLParser.parse(args);
        
        watch = new StopWatch();
        watch.start();
        if (Settings.dataFile.contains("temporal")) {
            runCCs(Settings.dataFolder + Settings.dataFile, Sets.newHashSet());
        } else {
            Complex complex = loadComplex(Settings.dataFolder + Settings.dataFile);
            System.out.println("Vertices=" + complex.getNumVertices() + " Simplices=" + complex.getNumberOfSimplices());
            List<String> results = run(complex, Settings.limited, Settings.timeout, -1);
            System.out.println("TIME: " + watch.getElapsedTimeInSec() + " Frequent simplets found: " + results.size());
            System.out.println("Writing output to disk...");
            writeResults(results);
        }
    }
    
    // run algorithm for each connected component in ccs (or all components if ccs is empty)
    private static void runCCs(String fileName, Set<Integer> ccs) throws IOException {
        List<Complex> complexes = loadComplexes(fileName);
        for (int i = 0; i< complexes.size(); i++) {
            if (ccs.isEmpty() || ccs.contains(i)) {
                List<String> results = run(complexes.get(i), Settings.limited, Settings.timeout, i);
                writeResults(results, i);
            }
        }
    }
    
    private static List run(Complex complex, boolean limited, long timeout, int i) throws IOException {
        Miner miner = new Miner();
        List fps = miner.mine(complex, Settings.minFreq, Settings.minDim, Settings.maxSize, limited, timeout);
        if (Settings.storeOccMap) {
            writeOccMap(miner.getOccMap(), i);
        }
        return fps;
    }
    
    private static Complex loadComplex(String fileName) throws IOException {
        final BufferedReader rows = new BufferedReader(new FileReader(fileName));
        List<Simplex> simplices = Lists.newArrayList();
        String line;
        int counter = 0;

        while ((line = rows.readLine()) != null) {
            String[] parts = line.split(" ");
            Set<Integer> tmp = Sets.newHashSet();
            for (String p : parts) {
                tmp.add(Integer.parseInt(p));
            }
            Simplex s = new Simplex(counter, tmp, true);
            simplices.add(s);
            counter++;
        }
        rows.close();
        return new Complex(simplices);
    } 
    
    // Load connected components
    private static List<Complex> loadComplexes(String fileName) throws IOException {
        final BufferedReader rows = new BufferedReader(new FileReader(fileName));
        List<Complex> complexes = Lists.newArrayList();
        String line;
        int counter = 0;

        while ((line = rows.readLine()) != null) {
            List<Simplex> simplices = Lists.newArrayList();
            String[] parts = line.split("\t\t");
            for (String p : parts) {
                Set<Integer> tmp = Sets.newHashSet();
                String[] els = p.substring(1, p.length()-1).split(",");
                for (String el : els) {
                    if (el.length() > 0) {
                        tmp.add(Integer.parseInt(el.strip()));
                    }
                }
                Simplex s = new Simplex(counter, tmp, true);
                simplices.add(s);
                counter++;
            }
            if (simplices.size() > 1) {
                Complex complex = new Complex(simplices);
                complexes.add(complex);
            }
        }
        rows.close();
        return complexes;
    }
    
    private static void writeStats(int numPatterns) {
        try {
            FileWriter fw = new FileWriter(Settings.outputFolder + "statistics.csv", true);
            fw.write(String.format("%s\t%s\t%f\t%d\t%d\t%d\t%d\t%s\t%s\n",
                    Settings.dataFile,
                    new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss").format(new Date()),
                    watch.getElapsedTime() / 1000.0D,
                    numPatterns,
                    Settings.minFreq,
                    Settings.minDim,
                    Settings.maxSize,
                    Settings.allMatches,
                    Settings.supportMeasure.equalsIgnoreCase("mni") ? Settings.supportMeasure : 
                            (Settings.supportMeasure + "-" + Settings.harmful)));
            fw.close();
        } catch (IOException ex) {
        }
    }
    
    private static void writeResults(List results) throws IOException {
        writeStats(results.size());
        try {
            String fName = Settings.dataFile
                    + "_F" + Settings.minFreq
                    + "m" + Settings.minDim
                    + "M" + Settings.maxSize
                    + "ALL" + Settings.allMatches
                    + "S" + (Settings.supportMeasure.equalsIgnoreCase("mni") ? Settings.supportMeasure : 
                            (Settings.supportMeasure + "-" + Settings.harmful))
                    + ".txt";
            FileWriter fwP = new FileWriter(Settings.outputFolder + fName);
            for (Object s : results) {
                fwP.write(s.toString() + "\n");
            }
            fwP.close();
        } catch (IOException ex) {
        }
    }
    
    // For each node, write in which frequent pattern it appears
    private static void writeOccMap(List<Pair<String, Integer>> results, int i) throws IOException {
        try {
            String fName;
            if (i < 0) {
                fName = Settings.dataFile
                    + "_F" + Settings.minFreq
                    + "m" + Settings.minDim
                    + "M" + Settings.maxSize
                    + "ALL" + Settings.allMatches
                    + "S" + (Settings.supportMeasure.equalsIgnoreCase("mni") ? Settings.supportMeasure : 
                            (Settings.supportMeasure + "-" + Settings.harmful))
                    + "_OM.txt";
            } else {
                fName = Settings.dataFile
                    + '_' + i + '_'
                    + "_F" + Settings.minFreq
                    + "m" + Settings.minDim
                    + "M" + Settings.maxSize
                    + "ALL" + Settings.allMatches
                    + "S" + (Settings.supportMeasure.equalsIgnoreCase("mni") ? Settings.supportMeasure : 
                            (Settings.supportMeasure + "-" + Settings.harmful))
                    + "_OM.txt";
            }
            FileWriter fwP = new FileWriter(Settings.outputFolder + fName);
            for (Pair<String, Integer> s : results) {
                fwP.write(s.getA() + "\t" + s.getB() + "\n");
            }
            fwP.close();
        } catch (IOException ex) {
        }
    }
    
    // Save Results when examining the connected components
    private static void writeResults(List results, int c) throws IOException {
        writeStats(results.size());
        try {
            String fName = Settings.dataFile
                    + '_' + c + '_'
                    + "_F" + Settings.minFreq
                    + "m" + Settings.minDim
                    + "M" + Settings.maxSize
                    + "ALL" + Settings.allMatches
                    + "S" + (Settings.supportMeasure.equalsIgnoreCase("mni") ? Settings.supportMeasure : 
                            (Settings.supportMeasure + "-" + Settings.harmful))
                    + ".txt";
            FileWriter fwP = new FileWriter(Settings.outputFolder + fName);
            for (Object s : results) {
                fwP.write(s.toString() + "\n");
            }
            fwP.close();
        } catch (IOException ex) {
        }
    }
    
}
