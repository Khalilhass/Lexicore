package ui;

import java.util.Scanner;

/// Presentation layer only — renders menu text and reads raw user input.
/// Contains no business logic; all decisions are made by the engine.
public class ConsoleMenu {

    private final Scanner scanner;

    public ConsoleMenu() {
        this.scanner = new Scanner(System.in);
    }

    /// Prints the main numbered menu.
    public void displayMainMenu() {
        System.out.println();
        System.out.println("===== LexiCore Main Menu =====");
        System.out.println("  1. Load / Reload Text");
        System.out.println("  2. View Analytics Dashboard");
        System.out.println("  3. Search Word/Phrase");
        System.out.println("  4. Replace Word");
        System.out.println("  5. Undo Last Change");
        System.out.println("  6. Redo Last Change");
        System.out.println("  7. Smart Autocomplete");
        System.out.println("  8. Predict Next Word");
        System.out.println("  9. Analyze Sentiment");
        System.out.println("  0. Shutdown");
        System.out.print("Choice: ");
    }

    public void displayInputSourceMenu() {
        System.out.println();
        System.out.println("How would you like to provide text?");
        System.out.println("  1. Type/paste text directly");
        System.out.println("  2. Load from a local file path");
        System.out.print("Choice: ");
    }


    /// Reads and returns the user's raw menu choice as an int; caller handles validation.
    public int readMenuChoice() {
        String line = scanner.nextLine();
        try {
            return Integer.parseInt(line.trim());
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    /// Reads a single line of free-text input (e.g., a search query or replacement word).
    public String readLine(String prompt) {
        System.out.print(prompt);
        return scanner.nextLine();
    }


     /// Reads raw multi-line typed input, one line at a time,
     /// until the user enters a line that exactly matches {@code sentinel}.
     /// The sentinel line itself is not included in the returned text.
    public String readMultilineUntilSentinel(String sentinel) {
        System.out.println();
        System.out.println("Type or paste your text below.");
        System.out.println("Enter a line containing only \"" + sentinel + "\" when done.");

        StringBuilder buffer = new StringBuilder();
        String line;
        while (!(line = scanner.nextLine()).equals(sentinel)) {
            buffer.append(line).append(System.lineSeparator());
        }
        return buffer.toString();
    }



    public void displayMessage(String message) {
        System.out.println(message);
    }

    public void displayError(String message) {
        System.out.println("Error: " + message);
    }

    public void close() {
        scanner.close();
    }
}
