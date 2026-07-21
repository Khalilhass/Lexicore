package engine;

import util.Constants;

import java.util.ArrayList;
import java.util.List;

/// Stateless preprocessing engine: normalization, sentence tokenization, and
/// word tokenization. Produces a populated {@link Corpus} from raw input.
public class TextEngine {

    ///Runs the full preprocessing pipeline on raw input text and returns a populated {@link Corpus}.
    public Corpus preprocess(String rawInput){
        if (rawInput == null) {
            rawInput = "";
        }

        String normalized = normalizeWhitespace(rawInput);
        List<String> sentences = splitSentences(normalized);

        List<List<String>> tokenizedSentences = new ArrayList<>(sentences.size());
        for (String sentence: sentences){
            tokenizedSentences.add(tokenizeWords(sentence));
        }

        Corpus corpus = new Corpus();
        corpus.setRawText(rawInput);
        corpus.setSentences(sentences);
        corpus.setTokenizedSentences(tokenizedSentences);
        return corpus;
    }

    /// Splits normalized text into sentences on '.', '!', '?'.
    /// Time: O(L) where L is the length of normalizedText. Space: O(L).
    public List<String> splitSentences(String normalizedText){
        List<String> sentences = new ArrayList<>();
        if (normalizedText == null || normalizedText.isEmpty()) {
            return sentences;
        }

        StringBuilder current = new StringBuilder();
        for (int i = 0; i < normalizedText.length(); i++) {
            char index = normalizedText.charAt(i);
            if (isSentenceTerminator(index)) {
                String sentence = current.toString().trim();
                if (!sentence.isEmpty()) {
                    sentences.add(sentence);
                }
                current.setLength(0);
            } else {
                current.append(index);
            }
        }

        // Capture any trailing text that wasn't closed by a terminator.
        String trailing = current.toString().trim();
        if (!trailing.isEmpty()) {
            sentences.add(trailing);
        }

        return sentences;

    }

    /// Splits a single sentence into word tokens on
    /// whitespace, stripping punctuation from each token and discarding tokens that become empty
    /// Time: O(S) where S is the length of the sentence. Space: O(S).
    public List<String> tokenizeWords(String sentence){
        List<String> tokens = new ArrayList<>();

        if (sentence == null || sentence.isEmpty()){
            return tokens;
        }

        String[] rawTokens = sentence.trim().split("\\s+");
        for (String rawToken : rawTokens) {
            String cleaned = stripPunctuation(rawToken);
            if (!cleaned.isEmpty()) {
                tokens.add(cleaned);
            }
        }
        return tokens;
    }

    /// Removes punctuation from a token, per the project's punctuation rules.
    /// Time: O(T) where T is the token length. Space: O(T).
    public String stripPunctuation(String token) {
        if (token == null || token.isEmpty()){
            return "";
        }

        StringBuilder cleaned = new StringBuilder(token.length());
        for (int i = 0; i < token.length(); i++) {
            char index = token.charAt(i);
            if (Character.isLetterOrDigit(index)) {
                cleaned.append(index);
            }
        }
        return cleaned.toString();

    }

    /// Collapses consecutive whitespace/newlines/tabs into single spaces
    /// Time: O(N) where N is the length of text. Space: O(N).
    public String normalizeWhitespace(String text) {
        if (text == null || text.isEmpty()){
            return "";
        }

        StringBuilder result = new StringBuilder(text.length());
        boolean lastWasWhitespace = false;

        for (int i = 0; i < text.length(); i++) {
            char charIndex = text.charAt(i);
            if (Character.isWhitespace(charIndex)) {
                if (!lastWasWhitespace && !result.isEmpty()) {
                    result.append(' ');
                }
                lastWasWhitespace = true;
            } else {
                result.append(Character.toLowerCase(charIndex));
                lastWasWhitespace = false;
            }
        }

        if (!result.isEmpty() && result.charAt(result.length() - 1) == ' ') {
            result.setLength(result.length() - 1);
        }

        return result.toString();
    }

    private boolean isSentenceTerminator(char terminatorChar){
        for(char terminator : Constants.SENTENCE_TERMINATORS){
            if(terminatorChar == terminator){
                return true;
            }
        }
        return false;
    }
}
