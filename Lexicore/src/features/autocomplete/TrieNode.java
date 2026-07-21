package features.autocomplete;

import java.util.HashMap;
import java.util.Map;

/// A single node in the autocomplete Trie. Each node holds its children keyed
/// by character, a flag marking whether it terminates a full word, and a
/// frequency counter used to rank suggestions.
public class TrieNode {

    private final Map<Character, TrieNode> children;
    private boolean isWordEnd;
    private int frequency;

    public TrieNode() {
        this.children = new HashMap<>();
        this.isWordEnd = false;
        this.frequency = 0;
    }

    public Map<Character, TrieNode> getChildren() {
        return children;
    }

    public boolean isWordEnd() {
        return isWordEnd;
    }

    public void setWordEnd(boolean wordEnd) {
        this.isWordEnd = wordEnd;
    }

    public int getFrequency() {
        return frequency;
    }

    public void incrementFrequency() {
        this.frequency++;
    }
}
