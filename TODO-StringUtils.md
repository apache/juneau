# StringUtils Enhancement TODO List

This document outlines recommended methods to add to the `StringUtils` class to make it a comprehensive string utility library suitable for external use.

## Current Status
The `StringUtils` class currently has 86 public static methods covering:
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
- [ ] `join(Object[] array, String delimiter)` - Join array elements with delimiter
- [ ] `join(int[] array, String delimiter)` - Join primitive int array
- [ ] `join(Collection<?> collection, String delimiter)` - Join collection elements
- [ ] `join(Object[] array, char delimiter)` - Join with char delimiter
- [ ] `join(int[] array, char delimiter)` - Join primitive int array with char
- [ ] `join(Collection<?> collection, char delimiter)` - Join collection with char
- [ ] `split(String str, char delimiter)` - Split string by character
- [ ] `split(String str, char delimiter, int limit)` - Split with limit
- [ ] `splitQuoted(String str, char delimiter)` - Split respecting quoted strings
- [ ] `splitNested(String str, char open, char close)` - Split respecting nested brackets
- [ ] `splitMethodArgs(String str)` - Parse method argument strings

### 2. String Validation and Checking
- [ ] `isBlank(String str)` - Check if string is null, empty, or whitespace only
- [ ] `isNotBlank(String str)` - Opposite of isBlank
- [ ] `isEmpty(String str)` - Check if string is null or empty (already has isNotEmpty)
- [ ] `hasText(String str)` - Check if string has non-whitespace content
- [ ] `isAlpha(String str)` - Check if string contains only letters
- [ ] `isAlphaNumeric(String str)` - Check if string contains only letters and digits
- [ ] `isDigit(String str)` - Check if string contains only digits
- [ ] `isWhitespace(String str)` - Check if string contains only whitespace
- [ ] `isEmail(String str)` - Basic email validation
- [ ] `isPhoneNumber(String str)` - Basic phone number validation
- [ ] `isCreditCard(String str)` - Credit card number validation (Luhn algorithm)

### 3. String Manipulation
- [ ] `capitalize(String str)` - Capitalize first letter
- [ ] `uncapitalize(String str)` - Uncapitalize first letter
- [ ] `camelCase(String str)` - Convert to camelCase
- [ ] `snakeCase(String str)` - Convert to snake_case
- [ ] `kebabCase(String str)` - Convert to kebab-case
- [ ] `pascalCase(String str)` - Convert to PascalCase
- [ ] `titleCase(String str)` - Convert to Title Case
- [ ] `reverse(String str)` - Reverse string
- [ ] `remove(String str, String remove)` - Remove all occurrences of substring
- [ ] `removeStart(String str, String prefix)` - Remove prefix if present
- [ ] `removeEnd(String str, String suffix)` - Remove suffix if present
- [ ] `removeAll(String str, String... remove)` - Remove multiple substrings
- [ ] `substringBefore(String str, String separator)` - Get substring before separator
- [ ] `substringAfter(String str, String separator)` - Get substring after separator
- [ ] `substringBetween(String str, String open, String close)` - Get substring between delimiters
- [ ] `left(String str, int len)` - Get leftmost characters
- [ ] `right(String str, int len)` - Get rightmost characters
- [ ] `mid(String str, int pos, int len)` - Get middle characters
- [ ] `padLeft(String str, int size, char padChar)` - Left pad string
- [ ] `padRight(String str, int size, char padChar)` - Right pad string
- [ ] `padCenter(String str, int size, char padChar)` - Center pad string
- [ ] `wrap(String str, int wrapLength)` - Wrap text to specified length
- [ ] `wrap(String str, int wrapLength, String newline)` - Wrap with custom newline

### 4. String Searching and Matching
- [ ] `indexOf(String str, String search)` - Find index of substring
- [ ] `indexOfIgnoreCase(String str, String search)` - Case-insensitive indexOf
- [ ] `lastIndexOf(String str, String search)` - Find last index of substring
- [ ] `lastIndexOfIgnoreCase(String str, String search)` - Case-insensitive lastIndexOf
- [ ] `containsIgnoreCase(String str, String search)` - Case-insensitive contains
- [ ] `startsWithIgnoreCase(String str, String prefix)` - Case-insensitive startsWith
- [ ] `endsWithIgnoreCase(String str, String suffix)` - Case-insensitive endsWith
- [ ] `matches(String str, String regex)` - Check if string matches regex
- [ ] `countMatches(String str, String search)` - Count occurrences of substring
- [ ] `countMatches(String str, char search)` - Count occurrences of character

### 5. Case Conversion
- [ ] `toLowerCase(String str)` - Convert to lowercase (wrapper for String.toLowerCase())
- [ ] `toUpperCase(String str)` - Convert to uppercase (wrapper for String.toUpperCase())
- [ ] `swapCase(String str)` - Swap case of all characters
- [ ] `toTitleCase(String str)` - Convert to title case (first letter of each word capitalized)

### 6. String Formatting and Templates
- [ ] `format(String template, Object... args)` - Enhanced string formatting (already exists, but could be enhanced)
- [ ] `formatWithNamedArgs(String template, Map<String, Object> args)` - Format with named placeholders
- [ ] `interpolate(String template, Map<String, Object> variables)` - Variable interpolation
- [ ] `pluralize(String word, int count)` - Simple pluralization
- [ ] `ordinal(int number)` - Convert number to ordinal (1st, 2nd, 3rd, etc.)

### 7. String Cleaning and Sanitization
- [ ] `clean(String str)` - Remove control characters and normalize whitespace
- [ ] `normalizeWhitespace(String str)` - Normalize all whitespace to single spaces
- [ ] `removeControlChars(String str)` - Remove control characters
- [ ] `removeNonPrintable(String str)` - Remove non-printable characters
- [ ] `sanitize(String str)` - Basic HTML/XML sanitization
- [ ] `escapeHtml(String str)` - Escape HTML entities
- [ ] `unescapeHtml(String str)` - Unescape HTML entities
- [ ] `escapeXml(String str)` - Escape XML entities
- [ ] `unescapeXml(String str)` - Unescape XML entities
- [ ] `escapeSql(String str)` - Escape SQL strings (basic)
- [ ] `escapeRegex(String str)` - Escape regex special characters

### 8. String Comparison and Sorting
- [ ] `equalsIgnoreCase(String str1, String str2)` - Case-insensitive equals
- [ ] `compareIgnoreCase(String str1, String str2)` - Case-insensitive comparison
- [ ] `naturalCompare(String str1, String str2)` - Natural string comparison (handles numbers)
- [ ] `levenshteinDistance(String str1, String str2)` - Calculate edit distance
- [ ] `similarity(String str1, String str2)` - Calculate string similarity percentage
- [ ] `isSimilar(String str1, String str2, double threshold)` - Check if strings are similar

### 9. String Generation and Random
- [ ] `generateUUID()` - Generate UUID string (already exists as random())
- [ ] `randomAlphabetic(int length)` - Generate random alphabetic string
- [ ] `randomAlphanumeric(int length)` - Generate random alphanumeric string
- [ ] `randomNumeric(int length)` - Generate random numeric string
- [ ] `randomAscii(int length)` - Generate random ASCII string
- [ ] `randomString(int length, String chars)` - Generate random string from character set

### 10. String Parsing and Extraction
- [ ] `parseMap(String str, char keyValueDelimiter, char entryDelimiter, boolean trimKeys)` - Parse key-value pairs
- [ ] `extractNumbers(String str)` - Extract all numbers from string
- [ ] `extractEmails(String str)` - Extract email addresses from string
- [ ] `extractUrls(String str)` - Extract URLs from string
- [ ] `extractWords(String str)` - Extract words from string
- [ ] `extractBetween(String str, String start, String end)` - Extract text between markers

### 11. String Transformation
- [ ] `transliterate(String str, String fromChars, String toChars)` - Character-by-character translation
- [ ] `soundex(String str)` - Generate Soundex code
- [ ] `metaphone(String str)` - Generate Metaphone code
- [ ] `doubleMetaphone(String str)` - Generate Double Metaphone code
- [ ] `normalizeUnicode(String str)` - Unicode normalization
- [ ] `removeAccents(String str)` - Remove diacritical marks

### 12. String Validation Patterns
- [ ] `isValidRegex(String regex)` - Validate regex pattern
- [ ] `isValidDateFormat(String dateStr, String format)` - Validate date format
- [ ] `isValidTimeFormat(String timeStr, String format)` - Validate time format
- [ ] `isValidIpAddress(String ip)` - Validate IP address
- [ ] `isValidMacAddress(String mac)` - Validate MAC address
- [ ] `isValidHostname(String hostname)` - Validate hostname

### 13. String Metrics and Analysis
- [ ] `wordCount(String str)` - Count words in string
- [ ] `lineCount(String str)` - Count lines in string
- [ ] `charCount(String str, char c)` - Count specific character (already exists as countChars)
- [ ] `mostFrequentChar(String str)` - Find most frequent character
- [ ] `entropy(String str)` - Calculate string entropy
- [ ] `readabilityScore(String str)` - Simple readability score

### 14. String Conversion Utilities
- [ ] `nullIfEmpty(String str)` - Return null if string is empty
- [ ] `emptyIfNull(String str)` - Return empty string if null
- [ ] `defaultIfEmpty(String str, String defaultStr)` - Return default if empty
- [ ] `defaultIfBlank(String str, String defaultStr)` - Return default if blank
- [ ] `toString(Object obj)` - Safe toString with null handling
- [ ] `toString(Object obj, String defaultStr)` - Safe toString with default

### 15. String Array and Collection Utilities
- [ ] `toStringArray(Collection<String> collection)` - Convert collection to string array
- [ ] `toList(String[] array)` - Convert string array to list
- [ ] `filter(String[] array, Predicate<String> predicate)` - Filter string array
- [ ] `map(String[] array, Function<String, String> mapper)` - Map string array
- [ ] `distinct(String[] array)` - Remove duplicates from string array
- [ ] `sort(String[] array)` - Sort string array
- [ ] `sortIgnoreCase(String[] array)` - Case-insensitive sort

### 16. String Builder Utilities
- [ ] `appendIfNotEmpty(StringBuilder sb, String str)` - Append if not empty
- [ ] `appendIfNotBlank(StringBuilder sb, String str)` - Append if not blank
- [ ] `appendWithSeparator(StringBuilder sb, String str, String separator)` - Append with separator
- [ ] `buildString(Consumer<StringBuilder> builder)` - Functional string building

### 17. String Constants and Utilities
- [ ] `EMPTY` - Empty string constant
- [ ] `SPACE` - Single space constant
- [ ] `NEWLINE` - Newline constant
- [ ] `TAB` - Tab constant
- [ ] `CRLF` - Carriage return + line feed constant
- [ ] `COMMON_SEPARATORS` - Common separator characters
- [ ] `WHITESPACE_CHARS` - All whitespace characters

### 18. Performance and Memory Utilities
- [ ] `intern(String str)` - String interning utility
- [ ] `isInterned(String str)` - Check if string is interned
- [ ] `getStringSize(String str)` - Calculate memory size of string
- [ ] `optimizeString(String str)` - String optimization suggestions

## Implementation Priority

### High Priority (Common Use Cases)
1. String joining and splitting methods
2. String validation methods (isBlank, isNotBlank, etc.)
3. String manipulation methods (capitalize, reverse, etc.)
4. Case conversion methods
5. String cleaning and sanitization methods

### Medium Priority (Useful Utilities)
1. String searching and matching methods
2. String formatting and templates
3. String comparison and sorting methods
4. String generation and random methods
5. String parsing and extraction methods

### Low Priority (Specialized Features)
1. String transformation methods
2. String validation patterns
3. String metrics and analysis
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

- `format(String pattern, Object...args)` - Could be enhanced with more formatting options
- `parseNumber(String s, Class<? extends Number> type)` - Could add more number types
- `getDuration(String s)` - Could support more duration formats
- `replaceVars(String s, Map<String,Object> m)` - Could add more variable syntax options
