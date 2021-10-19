package fsminer.utils;

public class Settings {
    
    public static String dataFolder;
    public static String outputFolder;
    public static String dataFile;
    public static int minDim;
    public static int maxSize;
    public static int minFreq;
    // whether you want to find all the occurrences of the simplets
    public static boolean allMatches;
    public static String supportMeasure = "mni";
    // to use if supportMeasure is mis
    public static boolean harmful = false;
    // whether you nees to save memory
    public static boolean limited;
    // max amount of time you want to spend on a candidate
    public static long timeout;
    // whether you want to write on disk the image sets
    public static boolean storeOccMap;
}
