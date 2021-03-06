package fi.tkk.ics.jbliss;

import java.util.Map;

/**
 * The default reporter for found generator automorphisms. Outputs the
 * automorphisms in the cycle format in a {@link java.io.PrintStream}.
 */
public class DefaultReporter implements Reporter {

    /**
     * The stream in which the generator automorphisms are output.
     */
    public java.io.PrintStream stream;
    /**
     * The string that is printed in front of each generator.
     */
    public String prefix;
    /**
     * The string that is printed after each generator.
     */
    public String postfix;

    public DefaultReporter() {
        prefix = "Aut gen: ";
        postfix = "\n";
        stream = System.out;
    }

    /**
     * Print the argument automorphism.
     *
     * @param aut An automorphism
     * @param user_parameter A parameter provided by the user
     */
    public void report(Map<Integer, Integer> aut, Object user_parameter) {
        stream.print(prefix);
        Utils.print_labeling(stream, aut);
        stream.print(postfix);
    }
}
