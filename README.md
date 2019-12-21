# LogViewer

This is a log viewer built around the idea of filtering logs by line. When a filter is applied, the viewer will show only only lines that match the filter.

## Filtering

The filter supports two different syntaxes, simple contains and regex. If your filter string starts with a `~`, the regex sytax will be used; otherwise, the simple contains syntax is used.

### Simple Contains

A simple contains filter string is a set of simple contains filter strings separated by `&` (and), `|` (or), or `!` (not) operators in infix notation. Parentheses for grouping operations are allowed. Operators, parentheses, and backslashes can be escaped with a backslash. The filter is case-insensitive.

> Example: `bob&(apple|orange)`
>
> Will filter to all lines that have "bob" and also have either "apple" or "orange" in them.

### Regex

A regex filter is a string starting with `~` and followed by a regex as supported by the Java `Pattern` class (https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/util/regex/Pattern.html). The regex willbe prepended with the flags for Unicode case insensitivity, `(?iu)`.

## Searching Within a Filter

You can do a type-ahead style search within the current filter a-la Emacs using Ctrl-S and Ctrl-R.

## Other Features

### Epoch Time Converstions

Select text containing only an integer and press Ctrl-T. A popup will show the number formatted as a epoch timestamp in the current system timezone and in UTC. Both second and millisecond timestamps are supported. The formatter heuristically determines which type of timestamp is being used.

### Token Styling

You can style any given character sequence so it is highlighted in a particular way. Select the text you want to style and use the menu or press Ctrl-# where the # is any number 1-5.

### Line Numbers

The LogViewer supports loading multiple files and displaying them as if they were one. The line numbers in the display show the aggregate line number. To see which file a particular line came from and what the line number in the file is, hold down the right mouse button with the cursor over the line number of the line.