package engine;

import util.BoxFormatter;

import java.util.*;

/// Computes and displays corpus-level statistics: token count, unique vocabulary count, and per-character frequency.
public class AnalyticsDashboard {

    private int totalTokens;
    private int uniqueVocabCount;
    private final Map<Character, Integer> charFrequency;

    public AnalyticsDashboard() {
        this.totalTokens = 0;
        this.uniqueVocabCount = 0;
        this.charFrequency = new HashMap<>();
    }

    /// Recomputes all statistics from the given corpus.
    ///
    /// Time: O(N + L) where N is the total token count (vocabulary/token
    /// pass) and L is the total character length of all sentences (character
    /// frequency pass). Both passes are linear and independent, so this is
    /// effectively O(corpus size) overall.
    /// Space: O(V + C) where V is unique vocabulary size and C is the number
    /// of distinct characters encountered (bounded by the alphabet).
    public void computeStatistics(Corpus corpus) {
        totalTokens = 0;
        uniqueVocabCount = 0;
        charFrequency.clear();

        Set<String> vocabulary = new HashSet<>();
        for (List<String> sentenceTokens : corpus.getTokenizedSentences()) {
            for (String token : sentenceTokens) {
                totalTokens++;
                vocabulary.add(token);
            }
        }
        uniqueVocabCount = vocabulary.size();

        for (String sentence : corpus.getSentences()) {
            for (int i = 0; i < sentence.length(); i++) {
                char c = sentence.charAt(i);
                if (c != ' ') {
                    charFrequency.merge(c, 1, Integer::sum);
                }
            }
        }
    }

    /// Prints a formatted dashboard: a bordered summary box (total tokens,
    /// unique vocabulary count, total non-space character count) followed by
    /// the frequency-ranked characters as a bordered horizontal grid (8 per
    /// row). Sorting happens only here (O(C log C) for C distinct
    /// characters), not during accumulation.
    public void printDashboard() {
        int totalCharacters = 0;
        for (int count : charFrequency.values()) {
            totalCharacters += count;
        }

        List<String[]> summaryRows = List.of(
                new String[] {"Total tokens (words)", String.valueOf(totalTokens)},
                new String[] {"Unique vocabulary count", String.valueOf(uniqueVocabCount)},
                new String[] {"Total characters (no spaces)", String.valueOf(totalCharacters)});

        System.out.println();
        System.out.println(BoxFormatter.labeledBox("Analytics Dashboard", summaryRows));

        System.out.println();
        System.out.println("Character frequency (most frequent first):");

        List<Map.Entry<Character, Integer>> sortedEntries = new ArrayList<>(charFrequency.entrySet());
        sortedEntries.sort(
                Comparator.<Map.Entry<Character, Integer>>comparingInt(Map.Entry::getValue)
                        .reversed()
                        .thenComparing(Map.Entry::getKey));

        if (sortedEntries.isEmpty()) {
            System.out.println("(no characters)");
            return;
        }

        List<String> items = new ArrayList<>(sortedEntries.size());
        for (Map.Entry<Character, Integer> entry : sortedEntries) {
            items.add("'" + entry.getKey() + "':" + entry.getValue());
        }
        System.out.println(BoxFormatter.gridList(items, 8));
    }

    public int getTotalTokens() {
        return totalTokens;
    }

    public int getUniqueVocabCount() {
        return uniqueVocabCount;
    }

    public Map<Character, Integer> getCharFrequency() {
        return charFrequency;
    }

}
