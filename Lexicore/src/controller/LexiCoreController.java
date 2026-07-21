package controller;

import engine.*;
import features.autocomplete.PredictiveTrie;
import features.predictive.BigramPredictor;
import features.sentiment.SentimentAnalyzer;
import ui.ConsoleMenu;
import util.BoxFormatter;
import util.Constants;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


/// orchestrates the full use-case flow: owns references to every engine and
 ///the menu, and is the only class that coordinates across features. No
 ///engine class should depend on this controller or on {@link ConsoleMenu}.
public class LexiCoreController {

    private final ConsoleMenu menu;
    private final TextEngine textEngine;
    private final AnalyticsDashboard dashboard;
    private final SearchIndex searchIndex;
    private final ReplacementEngine replacementEngine;
    private final HistoryManager historyManager;
    private final PredictiveTrie predictiveTrie;
    private final BigramPredictor bigramPredictor;
    private final SentimentAnalyzer sentimentAnalyzer;

    private Corpus corpus;
    private boolean corpusLoaded;
    private boolean running;

    public LexiCoreController() {
        this.menu = new ConsoleMenu();
        this.textEngine = new TextEngine();
        this.dashboard = new AnalyticsDashboard();
        this.searchIndex = new SearchIndex();
        this.replacementEngine = new ReplacementEngine();
        this.historyManager = new HistoryManager();
        this.predictiveTrie = new PredictiveTrie();
        this.bigramPredictor = new BigramPredictor();
        this.sentimentAnalyzer = new SentimentAnalyzer();
        this.sentimentAnalyzer.loadLexicons();
        this.corpus = new Corpus();
        this.corpusLoaded = false;
        this.running = false;
    }

    /// Starts the main service loop. Dispatches each menu choice to its
    /// handler, catching exceptions at a single central boundary (NFR4) so
    /// that no bad input, unimplemented handler, or unexpected error ever
    /// crashes the loop.
    public void run() {
        running = true;
        while (running) {
            try {
                menu.displayMainMenu();
                int choice = menu.readMenuChoice();
                dispatch(choice);
            } catch (java.util.NoSuchElementException | IllegalStateException e) {
                menu.displayMessage("Input stream closed. Shutting down.");
                running = false;
            } catch (UnsupportedOperationException e) {
                menu.displayMessage("That feature isn't implemented yet.");
            } catch (Exception e) {
                menu.displayError("Unexpected error: " + e.getMessage());
            }
        }
        // Safe to call even if handleShutdown() already closed the menu
        // (e.g. the EOF path above never reaches handleShutdown()) —
        // Scanner.close() is a no-op on an already-closed Scanner.
        menu.close();
    }

    private void dispatch(int choice) {
        switch (choice) {
            case 1:
                handleLoadText();
                break;
            case 2:
                handleDashboard();
                break;
            case 3:
                handleSearch();
                break;
            case 4:
                handleReplace();
                break;
            case 5:
                handleUndo();
                break;
            case 6:
                handleRedo();
                break;
            case 7:
                handleAutocomplete();
                break;
            case 8:
                handleNextWordPredict();
                break;
            case 9:
                handleSentiment();
                break;
            case 0:
                handleShutdown();
                break;
            default:
                menu.displayError("Invalid choice. Please select a number from the menu.");
        }
    }

    /// FR1/FR2/FR3: acquires raw text (typed stream or local file), then
    /// hands it to {@link TextEngine#preprocess} to populate the corpus.
    /// On any failure (invalid choice, missing/unreadable file, empty text)
    /// this method reports the problem via the menu and leaves the previous
    /// corpus state untouched — it never leaves the controller in a
    /// half-loaded state.
    public void handleLoadText() {
        menu.displayInputSourceMenu();
        int choice = menu.readMenuChoice();

        String rawText;
        switch (choice) {
            case 1:
                rawText = menu.readMultilineUntilSentinel(Constants.INPUT_SENTINEL);
                break;
            case 2:
                String pathInput = menu.readLine("Enter absolute file path: ");
                try {
                    rawText = loadRawTextFromFile(pathInput);
                } catch (IOException e) {
                    menu.displayError("Could not read file: " + e.getMessage());
                    return;
                }
                break;
            default:
                menu.displayError("Invalid choice. Please select 1 or 2.");
                return;
        }

        if (rawText == null || rawText.trim().isEmpty()) {
            menu.displayError("No text provided. Nothing was loaded.");
            return;
        }

        this.corpus = textEngine.preprocess(rawText);
        this.corpusLoaded = true;
        this.searchIndex.buildIndex(this.corpus);
        this.historyManager.clear();
        retrainPredictiveFeatures();

        List<String[]> summary = List.<String[]>of(
                new String[] {"Sentences loaded", String.valueOf(corpus.getSentences().size())});
        menu.displayMessage(BoxFormatter.labeledBox("Text Loaded", summary));
    }

    /// Rebuilds the Trie and bigram predictor from the current corpus.
    /// Called after every load/undo/redo, since all three change which
    /// corpus is "current" and Phase 5's predictive features must never
    /// suggest vocabulary or bigrams from a corpus state that's no longer
    /// loaded (the same staleness bug already fixed for undo/redo history —
    /// see {@link HistoryManager#clear()}).
    private void retrainPredictiveFeatures() {
        predictiveTrie.reset();
        for (List<String> sentenceTokens : corpus.getTokenizedSentences()) {
            for (String token : sentenceTokens) {
                predictiveTrie.insert(token);
            }
        }
        bigramPredictor.train(corpus);
    }

    /// Streams a local text file line by line into a single string, rather
    /// than materializing the whole file via {@code Files.readAllLines}
    /// first — keeping memory use proportional to what's being processed at
    /// any moment, consistent with the project's mobile-memory framing.
    /// Reading stops after {@link Constants#MAX_INPUT_LINES} lines even if
    /// the file is longer — a deliberate, documented mobile-memory guard
    /// (Section 12's "very large file" edge case) rather than an unbounded
    /// read. The remainder of the file is never read off disk at all, not
    /// just discarded after loading.
    /// @throws IOException if the path does not exist, is not readable, or
    ///                      a read error occurs
    private String loadRawTextFromFile(String pathInput) throws IOException {
        Path path = Paths.get(pathInput.trim());

        if (!Files.exists(path)) {
            throw new IOException("file does not exist: " + path);
        }
        if (!Files.isReadable(path)) {
            throw new IOException("file is not readable: " + path);
        }

        StringBuilder content = new StringBuilder();
        int lineCount = 0;
        boolean truncated = false;

        try (BufferedReader reader = Files.newBufferedReader(path)) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (lineCount >= Constants.MAX_INPUT_LINES) {
                    truncated = true;
                    break;
                }
                content.append(line).append(System.lineSeparator());
                lineCount++;
            }
        }

        if (truncated) {
            menu.displayMessage("Note: file exceeds " + Constants.MAX_INPUT_LINES
                    + " lines; only the first " + Constants.MAX_INPUT_LINES
                    + " were loaded (mobile-memory guard).");
        }
        return content.toString();
    }

    /// loaded corpus. Guards against running before any text has been
    /// loaded, per NFR4/Section 12's null-corpus edge case.
    public void handleDashboard() {
        if (!corpusLoaded) {
            menu.displayError("No text loaded yet. Please load text first.");
            return;
        }
        dashboard.computeStatistics(corpus);
        dashboard.printDashboard();
    }

    /// its sentence/token position. A single-word query goes straight to the
    /// O(1)-average indexed lookup; a multi-word query is routed to phrase
    /// search, which uses the same index to prune candidates before
    /// verifying sequential continuation.
    public void handleSearch() {
        if (!corpusLoaded) {
            menu.displayError("No text loaded yet. Please load text first.");
            return;
        }

        String query = menu.readLine("Enter word or phrase to search: ").trim();
        if (query.isEmpty()) {
            menu.displayError("Search query cannot be empty.");
            return;
        }

        String[] parts = query.split("\\s+");
        List<WordOccurrence> results = (parts.length == 1)
                ? searchIndex.search(parts[0])
                : searchIndex.searchPhrase(Arrays.asList(parts));

        if (results.isEmpty()) {
            menu.displayMessage("No occurrences found for \"" + query + "\".");
            return;
        }

        List<String> items = new ArrayList<>(results.size());
        for (WordOccurrence occurrence : results) {
            items.add(occurrence.toString());
        }
        menu.displayMessage("Found " + results.size() + " occurrence(s) of \"" + query + "\":");
        menu.displayMessage(BoxFormatter.verticalList(items));
    }

    /// corpus for undo *before* mutating (so undo can always restore the
    /// pre-replace state), then performs the replacement and reports the
    /// mutation count and elapsed time.
    public void handleReplace() {
        if (!corpusLoaded) {
            menu.displayError("No text loaded yet. Please load text first.");
            return;
        }

        String target = menu.readLine("Enter word to replace: ").trim();
        if (target.isEmpty()) {
            menu.displayError("Target word cannot be empty.");
            return;
        }
        String replacement = menu.readLine("Enter replacement word: ").trim();
        if (replacement.isEmpty()) {
            menu.displayError("Replacement word cannot be empty.");
            return;
        }

        historyManager.pushState(corpus.deepCopy());
        ReplacementEngine.ReplacementResult result =
                replacementEngine.replace(corpus, searchIndex, target, replacement);
        retrainPredictiveFeatures();

        List<String[]> summary = List.of(
                new String[] {"Mutations", String.valueOf(result.mutationCount())},
                new String[] {"Elapsed time (ms)", String.valueOf(result.elapsedMillis())});
        menu.displayMessage(BoxFormatter.labeledBox("Replace Result", summary));
    }

    /// replacement. The search index is fully rebuilt from the restored
    /// snapshot rather than reverse-patched — simpler and safer, and only
    /// paid on an explicit undo rather than on every replace.
    public void handleUndo() {
        if (!historyManager.canUndo()) {
            menu.displayError("Nothing to undo.");
            return;
        }
        this.corpus = historyManager.undo(this.corpus);
        this.searchIndex.buildIndex(this.corpus);
        retrainPredictiveFeatures();
        menu.displayMessage("Undo successful.");
    }

    /// Rollback: re-applies the most recently undone change.
    public void handleRedo() {
        if (!historyManager.canRedo()) {
            menu.displayError("Nothing to redo.");
            return;
        }
        this.corpus = historyManager.redo(this.corpus);
        this.searchIndex.buildIndex(this.corpus);
        retrainPredictiveFeatures();
        menu.displayMessage("Redo successful.");
    }

    /// {@link Constants#DEFAULT_AUTOCOMPLETE_TOP_K} frequency-ranked
    /// suggestions.
    public void handleAutocomplete() {
        if (!corpusLoaded) {
            menu.displayError("No text loaded yet. Please load text first.");
            return;
        }

        String prefix = menu.readLine("Enter prefix: ").trim().toLowerCase();
        if (prefix.isEmpty()) {
            menu.displayError("Prefix cannot be empty.");
            return;
        }

        List<String> suggestions = predictiveTrie.suggest(prefix);
        if (suggestions.isEmpty()) {
            menu.displayMessage("No suggestions found for prefix \"" + prefix + "\".");
            return;
        }

        menu.displayMessage("Suggestions for \"" + prefix + "\":");
        menu.displayMessage(BoxFormatter.verticalList(suggestions));
    }

    /// displays up to {@link Constants#DEFAULT_PREDICTION_TOP_K} most
    /// probable next words observed in the corpus.
    public void handleNextWordPredict() {
        if (!corpusLoaded) {
            menu.displayError("No text loaded yet. Please load text first.");
            return;
        }

        String word = menu.readLine("Enter current word: ").trim().toLowerCase();
        if (word.isEmpty()) {
            menu.displayError("Word cannot be empty.");
            return;
        }

        List<String> predictions = bigramPredictor.predictTopK(word);
        if (predictions.isEmpty()) {
            menu.displayMessage("No predictions available after \"" + word + "\".");
            return;
        }

        menu.displayMessage("Predicted next word(s) after \"" + word + "\":");
        menu.displayMessage(BoxFormatter.verticalList(predictions));
    }

    /// Positive, Negative, or Neutral, reporting the underlying score
    /// breakdown for transparency.
    public void handleSentiment() {
        if (!corpusLoaded) {
            menu.displayError("No text loaded yet. Please load text first.");
            return;
        }

        SentimentAnalyzer.SentimentResult result = sentimentAnalyzer.analyzeCorpus(corpus);
        List<String[]> summary = List.of(
                new String[] {"Label", result.getLabel().toString()},
                new String[] {"Positive signals", String.valueOf(result.getPositiveScore())},
                new String[] {"Negative signals", String.valueOf(result.getNegativeScore())});
        menu.displayMessage(BoxFormatter.labeledBox("Sentiment Result", summary));
    }

    public void handleShutdown() {
        this.running = false;
        menu.displayMessage("Shutting down LexiCore. Goodbye.");
        menu.close();
    }
}
