package features.autocomplete;

import java.util.*;

import util.Constants;

public class PredictiveTrie {

    private final TrieNode root;

    public PredictiveTrie() {
        this.root = new TrieNode();
    }

    /// Inserts a word into the Trie, creating nodes as needed and
    /// incrementing the terminal node's frequency (so repeated words in the
    /// corpus rank higher in suggestions).
    ///
    /// Time: O(L) where L is the word length. Space: O(L) worst case (all
    /// new nodes) per insertion.
    public void insert(String word) {
        if (word == null || word.isEmpty()) {
            return;
        }
        TrieNode current = root;
        for (int i = 0; i < word.length(); i++) {
            char c = word.charAt(i);
            current = current.getChildren().computeIfAbsent(c, k -> new TrieNode());
        }
        current.setWordEnd(true);
        current.incrementFrequency();
    }

    /// Returns up to {@link Constants#DEFAULT_AUTOCOMPLETE_TOP_K} suggestions for the given prefix.
    public List<String> suggest(String prefix) {
        return suggest(prefix, Constants.DEFAULT_AUTOCOMPLETE_TOP_K);
    }

    /// Returns up to {@code topK} frequency-ranked suggestions for the given
    /// prefix (most frequent first; ties broken alphabetically).
    ///
    /// Time: O(L) to walk the prefix + O(R) to collect R matching words in
    /// the subtree + O(R log K) to select the top K via a bounded min-heap.
    /// Space: O(R) for the collected candidates.
    public List<String> suggest(String prefix, int topK) {
        List<String> emptyResult = new ArrayList<>();
        if (prefix == null || prefix.isEmpty() || topK <= 0) {
            return emptyResult;
        }

        TrieNode current = root;
        for (int i = 0; i < prefix.length(); i++) {
            TrieNode next = current.getChildren().get(prefix.charAt(i));
            if (next == null) {
                return emptyResult; // no word in the Trie has this prefix
            }
            current = next;
        }

        List<Map.Entry<String, Integer>> allMatches = new ArrayList<>();
        collectWords(current, new StringBuilder(prefix), allMatches);

        PriorityQueue<Map.Entry<String, Integer>> minHeap =
                new PriorityQueue<>(Comparator.comparingInt(Map.Entry::getValue));
        for (Map.Entry<String, Integer> match : allMatches) {
            minHeap.offer(match);
            if (minHeap.size() > topK) {
                minHeap.poll();
            }
        }

        List<Map.Entry<String, Integer>> topMatches = new ArrayList<>(minHeap);
        topMatches.sort(
                Comparator.<Map.Entry<String, Integer>>comparingInt(Map.Entry::getValue)
                        .reversed()
                        .thenComparing(Map.Entry::getKey));

        List<String> suggestions = new ArrayList<>(topMatches.size());
        for (Map.Entry<String, Integer> entry : topMatches) {
            suggestions.add(entry.getKey());
        }
        return suggestions;
    }

    /// Depth-first collection of every complete word reachable from
    /// {@code node}, paired with its frequency.
    private void collectWords(TrieNode node, StringBuilder prefixSoFar, List<Map.Entry<String, Integer>> results) {
        if (node.isWordEnd()) {
            results.add(new AbstractMap.SimpleEntry<>(prefixSoFar.toString(), node.getFrequency()));
        }
        for (Map.Entry<Character, TrieNode> child : node.getChildren().entrySet()) {
            prefixSoFar.append(child.getKey());
            collectWords(child.getValue(), prefixSoFar, results);
            prefixSoFar.deleteCharAt(prefixSoFar.length() - 1);
        }
    }

    /// Discards all inserted words, returning the Trie to an empty state.
    /// Used when reloading a new corpus so stale vocabulary from a previous
    /// load doesn't leak into suggestions.
    public void reset() {
        root.getChildren().clear();
    }

    TrieNode getRoot() {
        return root;
    }
}

