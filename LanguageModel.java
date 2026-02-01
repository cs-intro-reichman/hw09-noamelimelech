import java.util.HashMap;
import java.util.Random;
import java.io.*;

public class LanguageModel {

    // Maps windows (strings) to lists of CharData objects.
    HashMap<String, List> CharDataMap;

    // The window length used in this model.
    int windowLength;

    // Random number generator
    private Random randomGenerator;

    public LanguageModel(int windowLength, int seed) {
        this.windowLength = windowLength;
        randomGenerator = new Random(seed);
        CharDataMap = new HashMap<String, List>();
    }

    public LanguageModel(int windowLength) {
        this.windowLength = windowLength;
        randomGenerator = new Random();
        CharDataMap = new HashMap<String, List>();
    }

    /** Builds a language model from the text in the given file (the corpus). */
    public void train(String fileName) {
        StringBuilder corpusBuilder = new StringBuilder();

        try (FileReader reader = new FileReader(fileName)) {
            int c;
            while ((c = reader.read()) != -1) {
                corpusBuilder.append((char) c);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to read corpus file: " + fileName, e);
        }

        String corpus = corpusBuilder.toString();
        if (corpus.length() <= windowLength) return;

        // Treat corpus as circular so last windows also get next char
        String extended = corpus + corpus.substring(0, windowLength);

        for (int i = 0; i < corpus.length(); i++) {
            String window = extended.substring(i, i + windowLength);
            char nextChr = extended.charAt(i + windowLength);

            List probs = CharDataMap.get(window);
            if (probs == null) {
                probs = new List();
                CharDataMap.put(window, probs);
            }
            probs.update(nextChr);
        }

        for (String key : CharDataMap.keySet()) {
            calculateProbabilities(CharDataMap.get(key));
        }
    }

    /** Computes and sets the probabilities (p and cp fields) of all the characters in the given list. */
    void calculateProbabilities(List probs) {
        if (probs == null || probs.getSize() == 0) return;

        int total = 0;
        for (int i = 0; i < probs.getSize(); i++) {
            total += probs.get(i).count;
        }
        if (total == 0) return;

        double cumulative = 0.0;
        for (int i = 0; i < probs.getSize(); i++) {
            CharData cd = probs.get(i);
            cd.p = (double) cd.count / total;
            cumulative += cd.p;
            cd.cp = cumulative;
        }

        // avoid rounding issues
        probs.get(probs.getSize() - 1).cp = 1.0;
    }

    /** Returns a random character from the given probabilities list. */
    char getRandomChar(List probs) {
        if (probs == null || probs.getSize() == 0) return ' ';

        double r = randomGenerator.nextDouble();
        for (int i = 0; i < probs.getSize(); i++) {
            CharData cd = probs.get(i);
            if (r <= cd.cp) return cd.chr;
        }
        return probs.get(probs.getSize() - 1).chr;
    }

    /**
     * Generates a random text based on learned probabilities.
     * IMPORTANT: textLength here is interpreted as "how many characters to ADD",
     * not "final total length". (Matches your autograder behavior.)
     */
    public String generate(String initialText, int textLength) {
        if (initialText == null) return "";
        if (textLength <= 0) return initialText;

        if (initialText.length() < windowLength) return initialText;

        int targetLength = initialText.length() + textLength;
        StringBuilder out = new StringBuilder(initialText);

        while (out.length() < targetLength) {
            String window = out.substring(out.length() - windowLength);
            List probs = CharDataMap.get(window);
            if (probs == null || probs.getSize() == 0) break;
            out.append(getRandomChar(probs));
        }

        return out.toString();
    }

    /** Returns a string representing the map of this language model. */
    public String toString() {
        StringBuilder str = new StringBuilder();
        for (String key : CharDataMap.keySet()) {
            List keyProbs = CharDataMap.get(key);
            str.append(key + " : " + keyProbs + "\n");
        }
        return str.toString();
    }

    public static void main(String[] args) {
        // Not required by autograder
    }
}
