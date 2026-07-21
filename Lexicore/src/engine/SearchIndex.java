package engine;

import java.util.*;

/// Inverted index (word -> list of occurrences)
/// built once at load time so that repeated searches are O(1) average instead of O(N) per query.
public class SearchIndex {

    private final Map<String, List<WordOccurrence>> invertedIndex;
    private Corpus indexedCorpus;

    public SearchIndex() {
        this.invertedIndex = new HashMap<>();
        this.indexedCorpus = null;
    }

    /// Builds the full inverted index from the given corpus, replacing and previously built index.
    /// Time: O(N) where N is the total token count across all sentences.
    /// Space: O(N) for the index (one {@link WordOccurrence} per token).
    public  void buildIndex(Corpus corpus){
        invertedIndex.clear();
        this.indexedCorpus = corpus;

        List<List<String>> tokenizedSentences = corpus.getTokenizedSentences();
        for (int sentenceIndex = 0; sentenceIndex < tokenizedSentences.size(); sentenceIndex++){
            List<String> tokens = tokenizedSentences.get(sentenceIndex);
            for (int tokenIndex = 0; tokenIndex < tokens.size(); tokenIndex ++){
                String word = tokens.get(tokenIndex);
                invertedIndex.computeIfAbsent(word,k-> new ArrayList<>())
                        .add(new WordOccurrence(sentenceIndex, tokenIndex, word));
            }
        }
    }

    /// Returns all occurrences of the given word (case-insensitive), or an empty list if it never occurs.
    /// Time: O(1) average lookup + O(M) to copy M matches.
    public List<WordOccurrence> search(String word){
        if (word == null || word.trim().isEmpty()) {
            return Collections.emptyList();
        }

        String normalized = word.trim().toLowerCase(Locale.ROOT);
        List<WordOccurrence> matches = invertedIndex.get(normalized);
        if (matches == null){
            return Collections.emptyList();
        }
        return List.copyOf(matches);
    }

    /// Returns the starting occurrence of every place the given phrase (sequence of words) appears in order within a single sentence.
    public List<WordOccurrence> searchPhrase(List<String> phraseTokens) {
        if (phraseTokens == null || phraseTokens.isEmpty()) {
            return Collections.emptyList();
        }

        List<String> normalizedPhrase = new ArrayList<>(phraseTokens.size());
        for (String token : phraseTokens){
            if (token == null || token.trim().isEmpty()) {
                return Collections.emptyList();
            }
            normalizedPhrase.add(token.trim().toLowerCase());
        }

        String firstWord = normalizedPhrase.getFirst();
        List<WordOccurrence> candidates = invertedIndex.getOrDefault(firstWord, Collections.emptyList());
        List<WordOccurrence> results = new ArrayList<>();

        for (WordOccurrence candidate : candidates) {
            if (matchesPhraseAt(candidate.getSentenceIndex(), candidate.getTokenIndex(), normalizedPhrase)) {
                results.add(candidate);
            }
        }
        return Collections.unmodifiableList(results);


    }


     /// Checks whether {@code normalizedPhrase} occurs, token-for-token,
     /// starting at {@code startTokenIndex} within the given sentence.
    private boolean matchesPhraseAt(int sentenceIndex, int startTokenIndex, List<String> normalizedPhrase) {
        List<List<String>> tokenizedSentences = indexedCorpus.getTokenizedSentences();
        if (sentenceIndex < 0 || sentenceIndex >= tokenizedSentences.size()) {
            return false;
        }

        List<String> tokens = tokenizedSentences.get(sentenceIndex);
        if (startTokenIndex + normalizedPhrase.size() > tokens.size()) {
            return false;
        }

        for (int i = 0; i < normalizedPhrase.size(); i++) {
            if (!tokens.get(startTokenIndex + i).equals(normalizedPhrase.get(i))) {
                return false;
            }
        }
        return true;
    }



    public void updateIndexOnReplace(String oldWord, String newWord, List<WordOccurrence> affected) {
        if (affected == null || affected.isEmpty()) {
            return;
        }

        List<WordOccurrence> oldList = invertedIndex.get(oldWord);
        if(oldList != null){
            oldList.removeAll(affected);
            if (oldList.isEmpty()) {
                invertedIndex.remove(oldWord);
            }
        }

        List<WordOccurrence> newList = invertedIndex.computeIfAbsent(newWord, k -> new ArrayList<>());
        for (WordOccurrence occurrence : affected) {
            newList.add(new WordOccurrence(occurrence.getSentenceIndex(), occurrence.getTokenIndex(), newWord));
        }
    }
}
