package util;

public class Constants {

    /// Line that terminates direct keyboard input in the console stream.
    public static final String INPUT_SENTINEL = "$$END_TEXT$$";

    /// Maximum number of states retained in the undo/redo history stacks.
    public static final int MAX_HISTORY_DEPTH = 10;

    /// Default number of ranked suggestions returned by autocomplete.
    public static final int DEFAULT_AUTOCOMPLETE_TOP_K = 5;

    /// Default number of ranked next-word predictions returned.
    public static final int DEFAULT_PREDICTION_TOP_K = 3;

    /// Sentence-terminating punctuation used by the sentence tokenizer.
    public static final char[] SENTENCE_TERMINATORS = {'.', '!', '?'};

    public static final int MAX_INPUT_LINES = 50_000;

}
