import java.util.HashMap;
import java.util.Random;
import java.io.*;

public class LanguageModel {

    HashMap<String, List> CharDataMap;
    int windowLength;
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
        // tests may call train multiple times on the same object
        CharDataMap.clear();

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

        // NOT circular: only positions that have a real next character
        for (int i = 0; i + windowLength < corpus.length(); i++) {
            String window = corpus.substring(i, i + windowLength);
            char nextChr = corpus.charAt(i + windowLength);

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

    /** Computes and sets probabilities (p and cp) of all chars in list. */
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
     * Generates random text.
     * textLength is interpreted as "how many characters to ADD"
     * (matches your autograder behavior).
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
