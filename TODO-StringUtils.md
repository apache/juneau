# StringUtils Enhancement TODO List

This document outlines recommended methods to add to the `StringUtils` class to make it a comprehensive string utility library suitable for external use.

## Current Status
The `StringUtils` class currently has 225+ public static methods covering:
- Base64 encoding/decoding
- Hex encoding/decoding  
- URL encoding/decoding
- String validation (numeric, JSON, URI, etc.)
- String manipulation (trim, replace, escape, etc.)
- Number parsing with suffixes
- Date/time parsing
- Compression/decompression
- Character utilities

## Recommended Additions

### 1. String Joining and Splitting
- ✅ All join methods implemented (Object[], int[], Collection with String/char delimiters)
- ✅ All split methods implemented (split, splitQuoted, splitNested, splitMethodArgs)

### 2. String Validation and Checking
- ✅ `isBlank(String str)` - Implemented
- ✅ `isNotBlank(String str)` - Implemented
- ✅ `hasText(String str)` - Implemented
- ✅ `isAlpha(String str)` - Implemented
- ✅ `isAlphaNumeric(String str)` - Implemented
- ✅ `isDigit(String str)` - Implemented
- ✅ `isWhitespace(String str)` - Implemented
- ✅ `isEmpty(String str)` - Check if string is null or empty - Implemented
- ✅ `isEmail(String str)` - Basic email validation - Implemented
- ✅ `isPhoneNumber(String str)` - Basic phone number validation - Implemented
- ✅ `isCreditCard(String str)` - Credit card number validation (Luhn algorithm) - Implemented

### 3. String Manipulation
- ✅ `capitalize(String str)` - Implemented
- ✅ `uncapitalize(String str)` - Implemented
- ✅ `reverse(String str)` - Implemented
- ✅ `remove(String str, String remove)` - Implemented
- ✅ `removeStart(String str, String prefix)` - Implemented
- ✅ `removeEnd(String str, String suffix)` - Implemented
- ✅ `substringBefore(String str, String separator)` - Implemented
- ✅ `substringAfter(String str, String separator)` - Implemented
- ✅ `substringBetween(String str, String open, String close)` - Implemented
- ✅ `left(String str, int len)` - Implemented
- ✅ `right(String str, int len)` - Implemented
- ✅ `mid(String str, int pos, int len)` - Implemented
- ✅ `padLeft(String str, int size, char padChar)` - Implemented
- ✅ `padRight(String str, int size, char padChar)` - Implemented
- ✅ `padCenter(String str, int size, char padChar)` - Implemented
- ✅ `camelCase(String str)` - Convert to camelCase - Implemented
- ✅ `snakeCase(String str)` - Convert to snake_case - Implemented
- ✅ `kebabCase(String str)` - Convert to kebab-case - Implemented
- ✅ `pascalCase(String str)` - Convert to PascalCase - Implemented
- ✅ `titleCase(String str)` - Convert to Title Case - Implemented
- ✅ `removeAll(String str, String... remove)` - Remove multiple substrings - Implemented
- ✅ `wrap(String str, int wrapLength)` - Wrap text to specified length - Implemented
- ✅ `wrap(String str, int wrapLength, String newline)` - Wrap with custom newline - Implemented

### 4. String Searching and Matching
- ✅ `countChars(String str, char search)` - Implemented (similar to countMatches for char)
- ✅ `indexOf(String str, String search)` - Find index of substring - Implemented
- ✅ `indexOfIgnoreCase(String str, String search)` - Case-insensitive indexOf - Implemented
- ✅ `lastIndexOf(String str, String search)` - Find last index of substring - Implemented
- ✅ `lastIndexOfIgnoreCase(String str, String search)` - Case-insensitive lastIndexOf - Implemented
- ✅ `containsIgnoreCase(String str, String search)` - Case-insensitive contains - Implemented
- ✅ `startsWithIgnoreCase(String str, String prefix)` - Case-insensitive startsWith - Implemented
- ✅ `endsWithIgnoreCase(String str, String suffix)` - Case-insensitive endsWith - Implemented
- ✅ `matches(String str, String regex)` - Check if string matches regex - Implemented
- ✅ `countMatches(String str, String search)` - Count occurrences of substring - Implemented

### 5. Case Conversion
- ✅ `lc(String str)` - Implemented (toLowerCase wrapper)
- ✅ `uc(String str)` - Implemented (toUpperCase wrapper)
- ✅ `swapCase(String str)` - Implemented
- ✅ `camelCase(String str)` - Convert to camelCase - Implemented
- ✅ `snakeCase(String str)` - Convert to snake_case - Implemented
- ✅ `kebabCase(String str)` - Convert to kebab-case - Implemented
- ✅ `pascalCase(String str)` - Convert to PascalCase - Implemented
- ✅ `titleCase(String str)` - Convert to Title Case - Implemented

### 6. String Formatting and Templates
- ✅ `format(String template, Object... args)` - Implemented (could be enhanced)
- ✅ `formatWithNamedArgs(String template, Map<String, Object> args)` - Format with named placeholders - Implemented
- ✅ `interpolate(String template, Map<String, Object> variables)` - Variable interpolation - Implemented
- ✅ `pluralize(String word, int count)` - Simple pluralization - Implemented
- ✅ `ordinal(int number)` - Convert number to ordinal (1st, 2nd, 3rd, etc.) - Implemented

### 7. String Cleaning and Sanitization
- ✅ `clean(String str)` - Implemented
- ✅ `normalizeWhitespace(String str)` - Implemented
- ✅ `removeControlChars(String str)` - Implemented
- ✅ `removeNonPrintable(String str)` - Implemented
- ✅ `sanitize(String str)` - Basic HTML/XML sanitization - Implemented
- ✅ `escapeHtml(String str)` - Escape HTML entities - Implemented
- ✅ `unescapeHtml(String str)` - Unescape HTML entities - Implemented
- ✅ `escapeXml(String str)` - Escape XML entities - Implemented
- ✅ `unescapeXml(String str)` - Unescape XML entities - Implemented
- ✅ `escapeSql(String str)` - Escape SQL strings (basic) - Implemented
- ✅ `escapeRegex(String str)` - Escape regex special characters - Implemented

### 8. String Comparison and Sorting
- ✅ `equalsIgnoreCase(String str1, String str2)` - Case-insensitive equals - Implemented
- ✅ `compareIgnoreCase(String str1, String str2)` - Case-insensitive comparison - Implemented
- ✅ `naturalCompare(String str1, String str2)` - Natural string comparison (handles numbers) - Implemented
- ✅ `levenshteinDistance(String str1, String str2)` - Calculate edit distance - Implemented
- ✅ `similarity(String str1, String str2)` - Calculate string similarity percentage - Implemented
- ✅ `isSimilar(String str1, String str2, double threshold)` - Check if strings are similar - Implemented

### 9. String Generation and Random
- ✅ `generateUUID()` - Generate UUID string - Implemented
- ✅ `randomAlphabetic(int length)` - Generate random alphabetic string - Implemented
- ✅ `randomAlphanumeric(int length)` - Generate random alphanumeric string - Implemented
- ✅ `randomNumeric(int length)` - Generate random numeric string - Implemented
- ✅ `randomAscii(int length)` - Generate random ASCII string - Implemented
- ✅ `randomString(int length, String chars)` - Generate random string from character set - Implemented

### 10. String Parsing and Extraction
- ✅ `parseMap(String str, char keyValueDelimiter, char entryDelimiter, boolean trimKeys)` - Parse key-value pairs - Implemented
- ✅ `extractNumbers(String str)` - Extract all numbers from string - Implemented
- ✅ `extractEmails(String str)` - Extract email addresses from string - Implemented
- ✅ `extractUrls(String str)` - Extract URLs from string - Implemented
- ✅ `extractWords(String str)` - Extract words from string - Implemented
- ✅ `extractBetween(String str, String start, String end)` - Extract text between markers - Implemented

### 11. String Transformation
- ✅ `transliterate(String str, String fromChars, String toChars)` - Character-by-character translation - Implemented
- ✅ `soundex(String str)` - Generate Soundex code - Implemented
- ✅ `metaphone(String str)` - Generate Metaphone code - Implemented
- ✅ `doubleMetaphone(String str)` - Generate Double Metaphone code - Implemented
- ✅ `normalizeUnicode(String str)` - Unicode normalization - Implemented
- ✅ `removeAccents(String str)` - Remove diacritical marks - Implemented

### 12. String Validation Patterns
- ✅ `isValidRegex(String regex)` - Validate regex pattern - Implemented
- ✅ `isValidDateFormat(String dateStr, String format)` - Validate date format - Implemented
- ✅ `isValidTimeFormat(String timeStr, String format)` - Validate time format - Implemented
- ✅ `isValidIpAddress(String ip)` - Validate IP address - Implemented
- ✅ `isValidMacAddress(String mac)` - Validate MAC address - Implemented
- ✅ `isValidHostname(String hostname)` - Validate hostname - Implemented

### 13. String Metrics and Analysis
- ✅ `countChars(String str, char c)` - Implemented
- ✅ `wordCount(String str)` - Count words in string - Implemented
- ✅ `lineCount(String str)` - Count lines in string - Implemented
- ✅ `mostFrequentChar(String str)` - Find most frequent character - Implemented
- ✅ `entropy(String str)` - Calculate string entropy - Implemented
- ✅ `readabilityScore(String str)` - Simple readability score - Implemented

### 14. String Conversion Utilities
- ✅ `nullIfEmpty(String str)` - Implemented
- ✅ `emptyIfNull(String str)` - Return empty string if null
- ✅ `defaultIfEmpty(String str, String defaultStr)` - Return default if empty
- ✅ `defaultIfBlank(String str, String defaultStr)` - Return default if blank
- ✅ `toString(Object obj)` - Safe toString with null handling
- ✅ `toString(Object obj, String defaultStr)` - Safe toString with default

### 15. String Array and Collection Utilities
- ✅ `toStringArray(Collection<String> collection)` - Convert collection to string array - Implemented
- ✅ `filter(String[] array, Predicate<String> predicate)` - Filter string array - Implemented
- ✅ `map(String[] array, Function<String, String> mapper)` - Map string array - Implemented
- ✅ `distinct(String[] array)` - Remove duplicates from string array - Implemented
- ✅ `sort(String[] array)` - Sort string array - Implemented
- ✅ `sortIgnoreCase(String[] array)` - Case-insensitive sort - Implemented

### 16. String Builder Utilities
- ✅ `appendIfNotEmpty(StringBuilder sb, String str)` - Append if not empty - Implemented
- ✅ `appendIfNotBlank(StringBuilder sb, String str)` - Append if not blank - Implemented
- ✅ `appendWithSeparator(StringBuilder sb, String str, String separator)` - Append with separator - Implemented
- ✅ `buildString(Consumer<StringBuilder> builder)` - Functional string building - Implemented

### 17. String Constants and Utilities
- ✅ `EMPTY` - Empty string constant - Implemented
- ✅ `SPACE` - Single space constant - Implemented
- ✅ `NEWLINE` - Newline constant - Implemented
- ✅ `TAB` - Tab constant - Implemented
- ✅ `CRLF` - Carriage return + line feed constant - Implemented
- ✅ `COMMON_SEPARATORS` - Common separator characters - Implemented
- ✅ `WHITESPACE_CHARS` - All whitespace characters - Implemented

### 18. Performance and Memory Utilities
- ✅ `intern(String str)` - String interning utility - Implemented
- ✅ `isInterned(String str)` - Check if string is interned - Implemented
- ✅ `getStringSize(String str)` - Calculate memory size of string - Implemented
- ✅ `optimizeString(String str)` - String optimization suggestions - Implemented

## Implementation Priority

### High Priority (Common Use Cases)
1. ✅ String joining and splitting methods - Completed
2. ✅ String validation methods (isBlank, isNotBlank, etc.) - Completed
3. ✅ String manipulation methods (capitalize, reverse, etc.) - Completed
4. ✅ Case conversion methods (camelCase, snakeCase, kebabCase, pascalCase, titleCase) - Completed
5. ✅ String cleaning and sanitization methods - Completed

### Medium Priority (Useful Utilities)
1. ✅ String searching and matching methods - Completed
2. ✅ String formatting and templates - Completed
3. ✅ String comparison and sorting methods - Completed
4. ✅ String generation and random methods - Completed
5. ✅ String parsing and extraction methods - Completed

### Low Priority (Specialized Features)
1. ✅ String transformation methods - Completed
2. ✅ String validation patterns - Completed
3. ✅ String metrics and analysis - Completed
4. String conversion utilities
5. Performance and memory utilities

## Notes

- All methods should follow the existing naming conventions and patterns
- Methods should be null-safe where appropriate
- Consider adding overloaded versions for common use cases
- Include comprehensive Javadoc documentation
- Add unit tests for all new methods
- Consider performance implications for frequently used methods
- Some methods might be better suited for separate utility classes (e.g., `RegexUtils`, `ValidationUtils`)

## Existing Methods to Consider Enhancing

- ✅ `format(String pattern, Object...args)` - Could be enhanced with more formatting options
- `parseNumber(String s, Class<? extends Number> type)` - Could add more number types
- `getDuration(String s)` - Could support more duration formats
- `replaceVars(String s, Map<String,Object> m)` - Could add more variable syntax options
