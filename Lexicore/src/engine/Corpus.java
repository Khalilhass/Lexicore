package engine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/// Single owner of all text state: the raw input, and the normalized,
/// sentence- and word-tokenized representation used by every engine.
/// No other class should hold an independent copy of the corpus text (single source of truth).
public class Corpus {

    private String rawText;

    private final List<String> sentences;

    private final List<List<String>> tokenizedSentences;

    public Corpus(){
        this.rawText="";
        this.sentences= new ArrayList<>();
        this.tokenizedSentences = new ArrayList<>();
    }

    public void setRawText(String rawText) {
        this.rawText = rawText;
    }

    public void setSentences(List<String>sentences){
        this.sentences.clear();
        this.sentences.addAll(sentences);
    }

    public void setTokenizedSentences(List<List<String>> newTokenizedSentences){
        this.tokenizedSentences.clear();
        for (List<String> sentenceTokens : newTokenizedSentences) {
            this.tokenizedSentences.add(new ArrayList<>(sentenceTokens));
        }
    }

    /// Mutates a single token in place (used by {@link ReplacementEngine}).
    /// @param sentenceIndex index into the sentence list
    /// @param tokenIndex    index into that sentence's token list
    /// @param newWord       replacement word
    public void setToken(int sentenceIndex, int tokenIndex, String newWord){
        List<String> tokens = tokenizedSentences.get(sentenceIndex);
        tokens.set(tokenIndex, newWord);
    }

    public String getRawText() {
        return rawText;
    }

    public List<String> getSentences() {
        return Collections.unmodifiableList(sentences);

    }

    public List<List<String>> getTokenizedSentences() {
        return Collections.unmodifiableList(tokenizedSentences);
    }

    /// Returns a deep copy of this corpus, used by {@link HistoryManager} for snapshots.
    public Corpus deepCopy() {
        Corpus copy = new Corpus();
        copy.rawText = this.rawText;
        copy.sentences.addAll(this.sentences);
        for (List<String> sentenceTokens : this.tokenizedSentences) {
            copy.tokenizedSentences.add(new ArrayList<>(sentenceTokens));
        }
        return copy;
    }

    /// Reconstructs a human-readable version of the current corpus state for display.
    public String toDisplayText() {
        throw new UnsupportedOperationException("Implement alongside dashboard/display work.");
    }

    public boolean isEmpty() {
        return sentences.isEmpty();
    }
}
