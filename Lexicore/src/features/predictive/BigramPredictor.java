package features.predictive;

import engine.Corpus;
import util.Constants;

import java.util.*;

public class BigramPredictor {

    private final Map<String, Map<String, Integer>> bigramMap;

    public BigramPredictor() {
        this.bigramMap = new HashMap<>();
    }

    /// Builds the bigram map from every adjacent token pair within each
    /// sentence of the corpus, replacing any previously trained data.
    ///
    /// Time: O(N) where N is the total token count. Space: O(unique bigrams).
    public void train(Corpus corpus) {
        bigramMap.clear();
        for (List<String> sentenceTokens : corpus.getTokenizedSentences()) {
            for (int i = 0; i < sentenceTokens.size() - 1; i++) {
                String current = sentenceTokens.get(i);
                String next = sentenceTokens.get(i + 1);
                bigramMap
                        .computeIfAbsent(current, k -> new HashMap<>())
                        .merge(next, 1, Integer::sum);
            }
        }
    }

    /// Returns the single most probable next word for the given current
    /// word, or {@code null} if the word was never observed or has no
    /// recorded successors. Ties are broken alphabetically for determinism.
    ///
    /// Time: O(1) average lookup + O(K) scan of K distinct successors.
    public String predictNext(String currentWord) {
        List<String> top = predictTopK(currentWord, 1);
        return top.isEmpty() ? null : top.getFirst();
    }

    /// Returns up to {@code topK} most probable next words, ranked by
    /// observed count (descending), ties broken alphabetically.
    ///
    /// Time: O(1) average lookup + O(K log K) to sort K distinct successors
    /// (K is bounded by that word's successor count, not total vocabulary).
    public List<String> predictTopK(String currentWord, int topK) {
        List<String> emptyResult = new ArrayList<>();
        if (currentWord == null || currentWord.trim().isEmpty() || topK <= 0) {
            return emptyResult;
        }

        String normalized = currentWord.trim().toLowerCase();
        Map<String, Integer> successors = bigramMap.get(normalized);
        if (successors == null || successors.isEmpty()) {
            return emptyResult;
        }

        List<Map.Entry<String, Integer>> entries = new ArrayList<>(successors.entrySet());
        entries.sort(
                Comparator.<Map.Entry<String, Integer>>comparingInt(Map.Entry::getValue)
                        .reversed()
                        .thenComparing(Map.Entry::getKey));

        List<String> result = new ArrayList<>();
        for (int i = 0; i < Math.min(topK, entries.size()); i++) {
            result.add(entries.get(i).getKey());
        }
        return result;
    }

    /// Convenience overload using {@link Constants#DEFAULT_PREDICTION_TOP_K}.
    public List<String> predictTopK(String currentWord) {
        return predictTopK(currentWord, Constants.DEFAULT_PREDICTION_TOP_K);
    }
}
