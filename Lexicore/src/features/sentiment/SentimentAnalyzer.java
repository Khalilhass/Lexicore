package features.sentiment;

import engine.Corpus;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SentimentAnalyzer {

    /// How many preceding tokens are checked for a negation marker.
    private static final int NEGATION_WINDOW = 2;

    private final Set<String> positiveLexicon;
    private final Set<String> negativeLexicon;
    private final Set<String> negationWords;

    public SentimentAnalyzer() {
        this.positiveLexicon = new HashSet<>();
        this.negativeLexicon = new HashSet<>();
        this.negationWords = new HashSet<>();
    }

    /// Loads the lexicons from a fixed embedded word list. Called once at
    /// startup — the lexicons do not change per corpus. Note: because the
    /// corpus tokenizer strips punctuation (Phase 1), contractions like
    /// "don't" become "dont" by the time they reach {@link #analyze}, so the
    /// negation list stores the stripped forms.
    public void loadLexicons() {
        positiveLexicon.clear();
        positiveLexicon.addAll(Arrays.asList(
                "good", "great", "excellent", "amazing", "wonderful", "fantastic", "happy",
                "love", "best", "awesome", "positive", "beautiful", "nice", "brilliant",
                "superb", "perfect", "delightful", "pleasant", "enjoyable", "favorite", "fun",
                "glad", "joy", "joyful", "kind", "lovely", "outstanding", "satisfied",
                "terrific", "impressive", "fabulous", "remarkable", "exceptional", "charming",
                "admire", "appreciate", "success", "successful", "grateful", "proud"));

        negativeLexicon.clear();
        negativeLexicon.addAll(Arrays.asList(
                "bad", "terrible", "awful", "horrible", "hate", "poor", "worst", "negative",
                "sad", "angry", "disappointing", "disappointed", "ugly", "boring", "annoying",
                "frustrating", "frustrated", "unpleasant", "disgusting", "dreadful", "pathetic",
                "mediocre", "inferior", "fail", "failure", "broken", "wrong", "problem",
                "worse", "painful", "miserable", "upset", "worried", "worry", "fear", "afraid",
                "unhappy", "dislike", "complain", "regret"));

        negationWords.clear();
        negationWords.addAll(Arrays.asList(
                "not", "never", "no", "none", "nobody", "nothing", "nowhere", "cannot",
                "cant", "dont", "doesnt", "didnt", "isnt", "wasnt", "werent", "wont",
                "wouldnt", "shouldnt", "couldnt", "without", "hardly", "barely", "rarely"));
    }

    /// Classifies a single tokenized sentence (or any token list). For each
    /// sentiment-lexicon hit, checks up to {@link #NEGATION_WINDOW} preceding
    /// tokens for a negation marker and flips its contribution if found.
    ///
    /// Time: O(T) where T is the token count (each token does an O(1)
    /// average pair of set lookups plus a bounded O(1) negation-window scan).
    /// Space: O(1) beyond the fixed-size lexicons.
    public SentimentResult analyze(List<String> tokens) {
        int positiveScore = 0;
        int negativeScore = 0;

        for (int i = 0; i < tokens.size(); i++) {
            String token = tokens.get(i);
            boolean isPositive = positiveLexicon.contains(token);
            boolean isNegative = negativeLexicon.contains(token);
            if (!isPositive && !isNegative) {
                continue;
            }

            boolean negated = isNegatedAt(tokens, i);
            if (isPositive) {
                if (negated) {
                    negativeScore++;
                } else {
                    positiveScore++;
                }
            } else {
                if (negated) {
                    positiveScore++;
                } else {
                    negativeScore++;
                }
            }
        }

        return buildResult(positiveScore, negativeScore);
    }

    /// Classifies the entire corpus by analyzing each sentence independently
    /// and summing the scores. Sentences are kept separate (rather than
    /// flattening the whole corpus into one token list) specifically so the
    /// negation lookback window never crosses a sentence boundary — e.g. the
    /// last word of one sentence being "not" should never negate the first
    /// sentiment word of the next sentence.
    public SentimentResult analyzeCorpus(Corpus corpus) {
        int totalPositive = 0;
        int totalNegative = 0;

        for (List<String> sentenceTokens : corpus.getTokenizedSentences()) {
            SentimentResult sentenceResult = analyze(sentenceTokens);
            totalPositive += sentenceResult.getPositiveScore();
            totalNegative += sentenceResult.getNegativeScore();
        }

        return buildResult(totalPositive, totalNegative);
    }

    private boolean isNegatedAt(List<String> tokens, int index) {
        int windowStart = Math.max(0, index - NEGATION_WINDOW);
        for (int i = windowStart; i < index; i++) {
            if (negationWords.contains(tokens.get(i))) {
                return true;
            }
        }
        return false;
    }

    private SentimentResult buildResult(int positiveScore, int negativeScore) {
        SentimentResult.Label label;
        if (positiveScore > negativeScore) {
            label = SentimentResult.Label.POSITIVE;
        } else if (negativeScore > positiveScore) {
            label = SentimentResult.Label.NEGATIVE;
        } else {
            label = SentimentResult.Label.NEUTRAL;
        }
        return new SentimentResult(label, positiveScore, negativeScore);
    }

    /// Immutable classification result.
    public static final class SentimentResult {
        public enum Label { POSITIVE, NEGATIVE, NEUTRAL }

        private final Label label;
        private final int positiveScore;
        private final int negativeScore;

        public SentimentResult(Label label, int positiveScore, int negativeScore) {
            this.label = label;
            this.positiveScore = positiveScore;
            this.negativeScore = negativeScore;
        }

        public Label getLabel() {
            return label;
        }

        public int getPositiveScore() {
            return positiveScore;
        }

        public int getNegativeScore() {
            return negativeScore;
        }
    }
}