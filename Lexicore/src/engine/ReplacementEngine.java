package engine;

import java.util.List;

public class ReplacementEngine {

    /// Replaces every occurrence of {@code target} with {@code replacement}
    /// in the given corpus, patching the search index incrementally.
    /// {@code target}/{@code replacement} are normalized (trimmed,
    /// lowercased) to match the tokenizer's normalization, consistent with
    /// how {@link SearchIndex#search(String)} normalizes queries.
    ///
    /// Time: O(1) average index lookup + O(K) to mutate and re-index K
    /// affected occurrences. Space: O(K) for the temporary occurrence list.
    public ReplacementResult replace(Corpus corpus, SearchIndex index, String target, String replacement) {
        long startNanos = System.nanoTime();

        String normalizedTarget = normalize(target);
        String normalizedReplacement = normalize(replacement);

        List<WordOccurrence> affected = index.search(normalizedTarget);
        int mutationCount = affected.size();

        for (WordOccurrence occurrence : affected) {
            corpus.setToken(occurrence.getSentenceIndex(), occurrence.getTokenIndex(), normalizedReplacement);
        }

        index.updateIndexOnReplace(normalizedTarget, normalizedReplacement, affected);

        long elapsedMillis = (System.nanoTime() - startNanos) / 1_000_000L;
        return new ReplacementResult(mutationCount, elapsedMillis);
    }

    private String normalize(String word) {
        return (word == null) ? "" : word.trim().toLowerCase();
    }

    /// Immutable result of a replacement operation: how many changes, how long it took.
        public record ReplacementResult(int mutationCount, long elapsedMillis) {
    }
}
