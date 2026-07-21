package engine;

///Immutable structural wrapper for a single word's position in the corpus:
/// which sentence it appears in, and its token position within that sentence.
/// Used by {@link SearchIndex} to answer positional search queries.
public class WordOccurrence {

    private final int sentenceIndex;
    private final int tokenIndex;
    private final String word;

    public WordOccurrence(int sentenceIndex, int tokenIndex, String word) {
        this.sentenceIndex = sentenceIndex;
        this.tokenIndex = tokenIndex;
        this.word = word;
    }

    public int getSentenceIndex() {
        return sentenceIndex;
    }

    public int getTokenIndex() {
        return tokenIndex;
    }

    public String getWord() {
        return word;
    }


    @Override
    public String toString() {
        return "\"" + word + "\" [sentence " + sentenceIndex + ", position " + tokenIndex + "]";
    }
}
