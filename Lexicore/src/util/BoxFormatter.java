package util;

import java.util.List;


public final class BoxFormatter {

    private BoxFormatter() {
        // Static utility class — never instantiated.
    }

    /// Builds a bordered box with a centered title and a list of
    /// "label : value" rows, values right-aligned. Used for anything that's
    /// fundamentally a small set of named results (dashboard summary,
    /// replace result, sentiment result, and similar).
    public static String labeledBox(String title, List<String[]> labelValuePairs) {
        int labelWidth = 0;
        int valueWidth = 0;
        for (String[] pair : labelValuePairs) {
            labelWidth = Math.max(labelWidth, pair[0].length());
            valueWidth = Math.max(valueWidth, pair[1].length());
        }
        int innerWidth = Math.max(title.length(), labelWidth + 3 + valueWidth) + 2;

        StringBuilder sb = new StringBuilder();
        sb.append("+").append(repeat('-', innerWidth)).append("+\n");
        sb.append("|").append(centerPad(title, innerWidth)).append("|\n");
        sb.append("+").append(repeat('-', innerWidth)).append("+\n");
        for (String[] pair : labelValuePairs) {
            String line = " " + padRight(pair[0], labelWidth) + " : " + padLeft(pair[1], valueWidth) + " ";
            sb.append("|").append(padRight(line, innerWidth)).append("|\n");
        }
        sb.append("+").append(repeat('-', innerWidth)).append("+");
        return sb.toString();
    }

    /// Builds a bordered, single-column vertical list — one item per row,
    /// with a divider between every row, content left-aligned. Used for any
    /// "here's a ranked/found list of things" result: search occurrences,
    /// autocomplete suggestions, next-word predictions.
    ///
    /// Assumes {@code items} is non-empty — callers are expected to handle
    /// the empty case themselves with a context-specific message (e.g. "no
    /// suggestions found"), since a generic empty box says less than that.
    public static String verticalList(List<String> items) {
        int width = 0;
        for (String item : items) {
            width = Math.max(width, item.length());
        }
        int cellWidth = width + 2;
        String segment = repeat('-', cellWidth);

        StringBuilder sb = new StringBuilder();
        sb.append("+").append(segment).append("+\n");
        for (int i = 0; i < items.size(); i++) {
            sb.append("| ").append(padRight(items.get(i), width)).append(" |\n");
            sb.append("+").append(segment).append("+");
            if (i < items.size() - 1) {
                sb.append("\n");
            }
        }
        return sb.toString();
    }

    /// Builds a bordered grid — up to {@code columns} items per row, content
    /// centered per cell, reading left-to-right then top-to-bottom. Used
    /// where items are short, roughly uniform-width tokens best scanned
    /// horizontally, e.g. character frequency ("'a':4").
    ///
    /// Assumes {@code items} is non-empty, same as {@link #verticalList}. An
    /// incomplete final row is padded with blank cells so the border stays
    /// rectangular.
    public static String gridList(List<String> items, int columns) {
        int maxLen = 0;
        for (String item : items) {
            maxLen = Math.max(maxLen, item.length());
        }
        int cellWidth = maxLen + 2;
        int cols = Math.max(1, Math.min(columns, items.size()));
        int rows = (int) Math.ceil(items.size() / (double) cols);
        String segment = repeat('-', cellWidth);

        StringBuilder sb = new StringBuilder();
        appendGridBorder(sb, segment, cols);
        for (int r = 0; r < rows; r++) {
            sb.append("|");
            for (int c = 0; c < cols; c++) {
                int index = r * cols + c;
                String content = (index < items.size()) ? items.get(index) : "";
                sb.append(centerPad(content, cellWidth)).append("|");
            }
            sb.append("\n");
            appendGridBorder(sb, segment, cols);
        }
        // Remove the trailing newline after the final border so callers
        // control their own spacing, consistent with the other methods here.
        if (sb.length() > 0 && sb.charAt(sb.length() - 1) == '\n') {
            sb.setLength(sb.length() - 1);
        }
        return sb.toString();
    }

    private static void appendGridBorder(StringBuilder sb, String segment, int columns) {
        sb.append("+");
        for (int c = 0; c < columns; c++) {
            sb.append(segment).append("+");
        }
        sb.append("\n");
    }

    private static String centerPad(String text, int width) {
        int totalPad = Math.max(0, width - text.length());
        int left = totalPad / 2;
        int right = totalPad - left;
        return repeat(' ', left) + text + repeat(' ', right);
    }

    private static String padRight(String text, int width) {
        int pad = Math.max(0, width - text.length());
        return text + repeat(' ', pad);
    }

    private static String padLeft(String text, int width) {
        int pad = Math.max(0, width - text.length());
        return repeat(' ', pad) + text;
    }

    private static String repeat(char c, int count) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < count; i++) {
            sb.append(c);
        }
        return sb.toString();
    }
}
