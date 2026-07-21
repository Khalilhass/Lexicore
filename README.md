# LexiCore: Mobile Text Processing Engine

A console-based Java simulation of a smart-keyboard text-processing SDK — sentence/word indexing, positional search, atomic replace with undo/redo, Trie-based autocomplete, bigram next-word prediction, and lexicon-based sentiment analysis.


---

## 1. Requirements

- JDK 11 or higher (`javac`/`java` on your PATH)
- No external dependencies — everything here is core Java (`java.util`, `java.nio.file`, `java.io`)

Check your JDK:
```
javac -version
```

---

## 2. How to Compile

From the folder containing this README (the one with the `Lexicore/` folder inside it):

```
mkdir out
javac -d out $(find Lexicore -name "*.java")
```

On Windows (PowerShell), replace the second line with:
```
javac -d out (Get-ChildItem -Recurse -Filter *.java Lexicore | ForEach-Object { $_.FullName })
```

A clean compile produces no output at all. If you see errors, double-check your JDK version (`javac -version` should say 11 or higher).

---

## 3. How to Run

```
java -cp out lexicore.app.Main
```

You'll land on the main menu:
```
===== LexiCore Main Menu =====
  1. Load / Reload Text
  2. View Analytics Dashboard
  3. Search Word/Phrase
  4. Replace Word
  5. Undo Last Change
  6. Redo Last Change
  7. Smart Autocomplete
  8. Predict Next Word
  9. Analyze Sentiment
  0. Shutdown
Choice:
```

Type a number and press Enter to choose an action. Type `0` to exit cleanly (or just close the terminal / press Ctrl+D — LexiCore handles an abrupt end-of-input gracefully too).

---

## 4. Quick Start — Load Some Text First

Everything except option `1` requires text to be loaded first (you'll get a clear error if you try otherwise). Two ways to load:

**Option A — type text directly:**
1. Choose `1` (Load / Reload Text)
2. Choose `1` (type/paste text directly)
3. Type or paste a few sentences
4. On its own line, type exactly: `$$END_TEXT$$` and press Enter

**Option B — load from a file:**
1. Choose `1` (Load / Reload Text)
2. Choose `2` (load from a local file path)
3. Enter the **absolute path** to a `.txt` file (e.g. `/home/you/sample.txt`)

A sample paragraph you can paste for testing (has enough vocabulary variety to exercise every feature meaningfully):

```
The movie was great and the acting was amazing. I love this film. The plot was not good and the ending was boring. I did not like the villain.
$$END_TEXT$$
```

---

## 5. Manual Test Guide — Every Feature, Step by Step

Load the sample paragraph above, then work through these in order. Expected results are given so you can confirm correct behavior yourself. Every action's output is now formatted consistently — a bordered box for single-result summaries (load confirmation, replace result, sentiment result), and a bordered vertical list for anything that returns multiple items (search results, autocomplete suggestions, next-word predictions, character frequency). Borders use plain ASCII (`+`/`-`/`|`), not Unicode box-drawing characters, so they render correctly in any terminal or console encoding — including Windows Command Prompt without a UTF-8 codepage.

### 5.1 Analytics Dashboard (option 2)
Just run it after loading. You'll see a bordered summary box (total tokens, unique vocabulary count, total non-space characters) followed by the character-frequency table laid out horizontally, 8 per row, most-frequent first, e.g.:

```
+-----------------------------------+
|        Analytics Dashboard        |
+-----------------------------------+
| Total tokens (words)         :  3 |
| Unique vocabulary count      :  3 |
| Total characters (no spaces) : 19 |
+-----------------------------------+

Character frequency (most frequent first):
+--------+--------+--------+--------+--------+--------+--------+--------+
| 'a':2  | 'c':2  | 'e':2  | 'j':2  | 'o':2  | 's':2  | 'b':1  | 'i':1  |
+--------+--------+--------+--------+--------+--------+--------+--------+
| 'l':1  | 'm':1  | 'p':1  | 't':1  | 'v':1  |        |        |        |
+--------+--------+--------+--------+--------+--------+--------+--------+
```

(Borders use plain ASCII `+`/`-`/`|`, not Unicode box-drawing characters, so this renders correctly in any terminal or console encoding — including Windows Command Prompt without a UTF-8 codepage.)

### 5.2 Search (option 3)
- Try a single word: `great` → should report 1 occurrence with its sentence/position.
- Try a phrase: `was not` → should find it in "the plot was not good."
- Try a word that isn't there: `dinosaur` → should report "No occurrences found," not an error.

### 5.3 Replace (option 4)
- Replace `was` with `seemed` → should report the mutation count (how many `was` occurrences existed) and elapsed time in ms.
- Run Search (5.2) for `was` again → should now find nothing; searching `seemed` should find the same positions `was` used to occupy.

### 5.4 Undo / Redo (options 5, 6)
- After the replace above, choose `5` (Undo) → text reverts; searching `was` again should find it.
- Choose `6` (Redo) → `seemed` comes back.
- Try Undo again with nothing left to undo (e.g. right after a fresh load) → should say "Nothing to undo," not crash.

### 5.5 Smart Autocomplete (option 7)
- Type prefix `gr` → should suggest `great`.
- Type prefix `xyz` → should say "No suggestions found."
- **Staleness check:** replace `great` with `superb` (option 4), then try prefix `gr` again → should now find nothing, and prefix `su` should now suggest `superb`. (This confirms the Trie is being retrained after every edit, not just at load time.)

### 5.6 Predict Next Word (option 8)
- Type `the` → should suggest a short ranked list of words that followed "the" somewhere in the text (e.g. `plot`, `ending`, `acting`, `movie`, `villain` — top 3 shown, alphabetical on ties).
- Type a word that's the last word of a sentence (nothing ever follows it) → should say "No predictions available."

### 5.7 Sentiment Analysis (option 9)
- Run it on the unmodified sample paragraph → should report **POSITIVE** with a positive/negative score breakdown (great + amazing + love = 3 positive; "not good" flips to negative + boring = 2 negative).
- This is a good one to walk through live in your demo — it's the clearest illustration of the negation-window logic working correctly.

### 5.8 Edge Cases Worth Demonstrating Live
These show off the hardening work specifically (good material for the "handling input exceptions" part of your demo):
- Enter an invalid menu choice (e.g. `99`) → rejected cleanly, menu redisplays.
- Try to load from a file path that doesn't exist → reported as an error, no crash.
- Try to load from a directory path instead of a file → reported as an error, no crash.
- Load an empty typed input (just type the sentinel immediately) → "No text provided," nothing loaded.
- Reload a second, different text over the first, then try Undo → should say "Nothing to undo" (history is correctly cleared on reload, not left pointing at the old text).

---

## 6. Automated-Style Regression Testing (optional, faster for repeated runs)

Instead of typing interactively every time, you can pipe a whole session in from a text file — useful for quickly re-verifying everything still works after a code change:

```
java -cp out com.lexicore.app.Main < test_file.txt
```

Where `test_file.txt` contains one menu choice / input per line, in the order you'd type them interactively, e.g.:
```
1
1
The movie was great. I loved it.
$$END_TEXT$$
2
3
great
9
0
```

This is exactly how this project's phases were verified during development — every phase was compiled and actually run against scripted sessions like this, not just reviewed by eye.

---

## 7. Project Structure

```
com/lexicore/
 ├── app/Main.java                       — entry point
 ├── controller/LexiCoreController.java  — orchestrates every use case
 ├── ui/ConsoleMenu.java                 — all console input/output
 ├── data/
 ├── domain/
 │    ├── Corpus.java                    — owns the loaded text (raw + tokenized)
 │    ├── TextEngine.java                — preprocessing (normalize, tokenize)
 │    ├── AnalyticsDashboard.java        — token/vocab/char stats
 │    ├── WordOccurrence.java            — one word's sentence/position
 │    ├── SearchIndex.java               — inverted index for positional search
 │    ├── ReplacementEngine.java         — atomic word replace
 │    └── HistoryManager.java            — undo/redo (two capped stacks)
 ├── features/
 │    ├── autocomplete/                  — TrieNode, PredictiveTrie
 │    ├── predictive/                    — BigramPredictor
 │    └── sentiment/                     — SentimentAnalyzer
 └── util/Constants.java                 — sentinel string, caps, defaults
```

