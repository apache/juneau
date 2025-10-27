# TODO: com.sfdc.irs.Utils Methods to Copy to Juneau

This file tracks methods from `com.sfdc.irs.Utils` that should be evaluated for copying to Apache Juneau utility classes.

## Methods Already in Juneau (No Action Needed)

These methods already exist in Juneau utility classes:

1. `eq(T s1, T s2)` - Exists in `Utils`
2. `ne(T s1, T s2)` - Exists in `Utils`
3. `isEmpty(String o)` - Exists in `StringUtils`
4. `isNotEmpty(String o)` - Exists in `StringUtils`
5. `split(String s)` - Wrapper to `StringUtils.split()`
6. `split(String s, char delim)` - Wrapper to `StringUtils.split()`
7. `json(Object o)` - Uses `Json5.DEFAULT.write()` (Juneau already has this)
8. `abbreviate(String value, int length)` - Wrapper to `StringUtils.abbreviate()`
9. `isNumeric(String val)` - Wrapper to `StringUtils.isNumeric()`
10. `emptyIfNull(String value)` - Exists in `StringUtils`
11. `nullIfEmpty(String value)` - Exists in `StringUtils`
12. `splitQcd(String s, ArrayList<String> result)` - Complex quoted CSV parsing (likely already exists)
13. `beanMap(T bean)` - Uses `BeanContext.DEFAULT_SESSION.toBeanMap()`
14. `getBeanProp(Object o, String name)` - SFDC-specific bean property access
15. `safe(ThrowingSupplier<T> s)` - Exists in Juneau as similar patterns
16. `safe(ThrowingSupplier<T> s, ThrowableLogger logger)` - Exists in Juneau as similar patterns
17. `s(Object val)` - Exists as `StringUtils.stringify()` or similar
18. `escapeChars(String val)` - Wrapper to `StringUtils.escapeChars()`
19. `endsWith(String s, char...chars)` - Wrapper to `StringUtils.endsWith()`
20. `indexOf(String s, char...chars)` - Wrapper to `StringUtils.indexOf()`
21. `isNotNumeric(String s)` - Can be `!isNumeric(s)`
22. `compare(Object o1, Object o2)` - May already exist
23. `getStackTrace(Throwable t)` - May already exist in `ThrowableUtils`

## Methods to Consider Copying

### String Methods (Target: `StringUtils`)

- [ ] **1. `lc(String s)`** - Lowercase with null safety
  ```java
  public static String lc(String s) {
    return s == null ? null : s.toLowerCase();
  }
  ```

- [ ] **2. `uc(String s)`** - Uppercase with null safety
  ```java
  public static String uc(String s) {
    return s == null ? null : s.toUpperCase();
  }
  ```

- [ ] **3. `eqic(Object a, Object b)`** - Equals ignore case (converts both to strings)
  ```java
  public static boolean eqic(Object a, Object b) {
    if (a == null && b == null) { return true; }
    if (a == null || b == null) { return false; }
    return Objects.equals(a.toString().toLowerCase(), b.toString().toLowerCase());
  }
  ```

- [ ] **4. `articlized(String subject)`** - Adds 'a' or 'an' before word
  ```java
  public static String articlized(String subject) {
    var p = Pattern.compile("^[AEIOUaeiou].*");
    return (p.matcher(subject).matches() ? "an " : "a ") + subject;
  }
  ```

- [ ] **5. `obfuscate(String s)`** - Returns obfuscated string (e.g., "p*******")
  ```java
  public static String obfuscate(String s) {
    if (s == null || s.length() < 2)
      return "*";
    return s.substring(0, 1) + s.substring(1).replaceAll(".", "*");
  }
  ```

- [ ] **6. `coalesce(String...vals)`** - Returns first non-empty string
  ```java
  public static String coalesce(String...vals) {
    for (String v : vals) {
      if (isNotEmpty(v)) {
        return v;
      }
    }
    return null;
  }
  ```

- [ ] **7. `splits(String s)`** - Splits comma-delimited list to Stream
  ```java
  public static Stream<String> splits(String s) {
    return Stream.of(isEmpty(s) ? new String[0] : split(s)).map(String::trim);
  }
  ```

- [ ] **8. `splits(String s, char delim)`** - Splits delimited list to Stream with custom delimiter
  ```java
  public static Stream<String> splits(String s, char delim) {
    return Stream.of(isEmpty(s) ? new String[0] : split(s, delim)).map(String::trim);
  }
  ```

- [ ] **9. `cdlToList(String s)`** - Comma-delimited string to List
  ```java
  public static List<String> cdlToList(String s) {
    return Stream.of(isEmpty(s) ? new String[0] : split(s)).map(String::trim).collect(toList());
  }
  ```

- [ ] **10. `join(String...values)`** - Combines values into comma-delimited list
  ```java
  public static String join(String...values) {
    return StringUtils.join(values, ',');
  }
  ```

- [ ] **11. `join(Collection<?> values)`** - Combines collection into comma-delimited list
  ```java
  public static String join(Collection<?> values) {
    return StringUtils.joine(new ArrayList<>(values), ',');
  }
  ```

- [ ] **12. `contains(String s, String...values)`** - Null-safe contains check for multiple values
  ```java
  public static boolean contains(String s, String...values) {
    if (s == null || values == null || values.length == 0)
      return false;
    for (String v : values) {
      if (s.contains(v))
        return true;
    }
    return false;
  }
  ```

- [ ] **13. `contains(String s, char...values)`** - Null-safe contains check for multiple chars
  ```java
  public static boolean contains(String s, char...values) {
    if (s == null || values == null || values.length == 0)
      return false;
    for (char v : values) {
      if (s.indexOf(v) >= 0)
        return true;
    }
    return false;
  }
  ```

- [ ] **14. `notContains(String s, String...values)`** - Null-safe not-contains check
  ```java
  public static boolean notContains(String s, String...values) {
    return ! contains(s, values);
  }
  ```

- [ ] **15. `notContains(String s, char...values)`** - Null-safe not-contains check for chars
  ```java
  public static boolean notContains(String s, char...values) {
    return ! contains(s, values);
  }
  ```

- [ ] **16. `stringify(Object o)`** - Enhanced stringify with Collection/Map/Calendar support
  ```java
  public static String stringify(Object o) {
    if (o instanceof Collection)
      return (String) Collection.class.cast(o).stream().map(Utils::stringify).collect(joining(",","[","]"));
    if (o instanceof Map)
      return (String) Map.class.cast(o).entrySet().stream().map(Utils::stringify).collect(joining(",","{","}"));
    if (o instanceof Map.Entry) {
      var e = Map.Entry.class.cast(o);
      return stringify(e.getKey()) + '=' + stringify(e.getValue());
    }
    if (o instanceof GregorianCalendar) {
      return GregorianCalendar.class.cast(o).toZonedDateTime().format(DateTimeFormatter.ISO_INSTANT);
    }
    if (o != null && o.getClass().isArray()) {
      List<Object> l = list();
      for (var i = 0; i < Array.getLength(o); i++) {
        l.add(Array.get(o, i));
      }
      return stringify(l);
    }
    return StringUtils.stringify(o);
  }
  ```

### Utils Methods

- [ ] **17. `b(Object val)`** - Converts string/object to boolean
  ```java
  public static boolean b(Object val) {
    return ofNullable(val).map(Object::toString).map(Boolean::valueOf).orElse(false);
  }
  ```

- [ ] **18. `isBetween(int n, int lower, int higher)`** - Check if number is inclusively between two values
  ```java
  public static boolean isBetween(int n, int lower, int higher) {
    return n >= lower && n <= higher;
  }
  ```

- [ ] **19. `parseInt(String value)`** - parseInt with underscore removal
  ```java
  public static int parseInt(String value) {
    return Integer.parseInt(removeUnderscores(value));
  }
  ```

- [ ] **20. `parseLong(String value)`** - parseLong with underscore removal
  ```java
  public static long parseLong(String value) {
    return Long.parseLong(removeUnderscores(value));
  }
  ```

- [ ] **21. `parseFloat(String value)`** - parseFloat with underscore removal
  ```java
  public static float parseFloat(String value) {
    return Float.parseFloat(removeUnderscores(value));
  }
  ```

- [ ] **22. `optional(T value)`** - Optional.ofNullable but treats -1 as null
  ```java
  public static <T extends Number> Optional<T> optional(T value) {
    return Optional.ofNullable(value).filter(x -> x.intValue() >= 0);
  }
  ```

- [ ] **23. `format(String pattern, Object...args)`** - MessageFormat.format wrapper
  ```java
  public static String format(String pattern, Object...args) {
    if (notContains(pattern, "{"))
      return pattern;
    return MessageFormat.format(pattern, args);
  }
  ```

- [ ] **24. `coalesce(T...vals)`** - Returns first non-null object
  ```java
  public static <T> T coalesce(T...vals) {
    for (T v : vals) {
      if (v != null) {
        return v;
      }
    }
    return null;
  }
  ```

- [ ] **25. `eq(T o1, U o2, BiPredicate<T,U> test)`** - Equals with custom test
  ```java
  public static <T,U> boolean eq(T o1, U o2, BiPredicate<T,U> test) {
    if (o1 == null) { return o2 == null; }
    if (o2 == null) { return false; }
    if (o1 == o2) { return true; }
    return test.test(o1, o2);
  }
  ```

### ThrowableUtils Methods

- [ ] **26. `findCause(Throwable e, Class<T> cause)`** - Find exception in cause chain
  ```java
  public static <T extends Throwable> Optional<T> findCause(Throwable e, Class<T> cause) {
    while (e != null) {
      if (cause.isInstance(e)) { return Optional.of(cause.cast(e)); }
      e = e.getCause();
    }
    return Optional.empty();
  }
  ```

- [ ] **27. `hash(Throwable t, String stopClass)`** - Calculate hash from throwable stacktrace
  ```java
  public static int hash(Throwable t, String stopClass) {
    var i = 0;
    while (t != null) {
      for (StackTraceElement e : t.getStackTrace()) {
        if (e.getClassName().equals(stopClass))
          break;
        if (notContains(e.getClassName(), '$'))
          i = 31*i+e.hashCode();
      }
      t = t.getCause();
    }
    return i;
  }
  ```

- [ ] **28. `runtimeException(String msg, Object...args)`** - Supplier for RuntimeException
  ```java
  public static Supplier<RuntimeException> runtimeException(String msg, Object...args) {
    return () -> new RuntimeException(MessageFormat.format(msg, args));
  }
  ```

### CollectionUtils Methods

- [ ] **29. `cdlToSet(String s)`** - Comma-delimited string to LinkedHashSet
  ```java
  public static LinkedHashSet<String> cdlToSet(String s) {
    return Stream.of(isEmpty(s) ? new String[0] : split(s)).map(String::trim).collect(toCollection(LinkedHashSet::new));
  }
  ```

- [ ] **30. `treeSet(Set<T> copyFrom)`** - Create TreeSet from Set
  ```java
  public static <T> TreeSet<T> treeSet(Set<T> copyFrom) {
    return copyFrom == null ? null : new TreeSet<>(copyFrom);
  }
  ```

- [ ] **31. `treeSet(T...values)`** - Create TreeSet from varargs
  ```java
  public static <T> TreeSet<T> treeSet(T...values) {
    return new TreeSet<>(Arrays.asList(values));
  }
  ```

- [ ] **32. `list(T...values)`** - Create ArrayList from varargs
  ```java
  public static <T> ArrayList<T> list(T...values) {
    return new ArrayList<>(Arrays.asList(values));
  }
  ```

- [ ] **33. `set(T...values)`** - Create LinkedHashSet from varargs
  ```java
  public static <T> LinkedHashSet<T> set(T...values) {
    return new LinkedHashSet<>(Arrays.asList(values));
  }
  ```

- [ ] **34. `appendSet(Set<T> existing, T...values)`** - Append values to set (creates if null)
  ```java
  public static <T> Set<T> appendSet(Set<T> existing, T...values) {
    var existing2 = ofNullable(existing).orElse(new LinkedHashSet<>());
    Arrays.stream(values).forEach(existing2::add);
    return existing2;
  }
  ```

- [ ] **35. `append(Set<E> set, E...values)`** - Append values to set
  ```java
  public static <E> Set<E> append(Set<E> set, E...values) {
    return set == null ? set(values) : addAll(set, values);
  }
  ```

- [ ] **36. `addAll(C collection, E...elements)`** - Add all elements to collection
  ```java
  public static <E, C extends Collection<E>> C addAll(C collection, E...elements) {
    Collections.addAll(collection, elements);
    return collection;
  }
  ```

- [ ] **37. `toTreeSet(Comparator<T> comparator)`** - Stream collector for TreeSet
  ```java
  public static <T> Collector<T,?,TreeSet<T>> toTreeSet(Comparator<T> comparator) {
    return Collectors.toCollection(() -> new TreeSet<>(comparator));
  }
  ```

- [ ] **38. `map(Object...values)`** - Create LinkedHashMap from key/value pairs
  ```java
  public static <K,V> LinkedHashMap<K,V> map(Object...values) {
    var m = new LinkedHashMap<K,V>();
    for (var i = 0; i < values.length; i+=2) {
      m.put((K)values[i], (V)values[i+1]);
    }
    return m;
  }
  ```

- [ ] **39. `jmap(Object...values)`** - Create JsonMap from key/value pairs
  ```java
  public static JsonMap jmap(Object...values) {
    var m = new JsonMap();
    for (var i = 0; i < values.length; i+=2) {
      m.put(stringify(values[i]), values[i+1]);
    }
    return m;
  }
  ```

- [ ] **40. `smap(Object...values)`** - Create string map from key/value pairs
  ```java
  public static Map<String,String> smap(Object...values) {
    var m = new LinkedHashMap<String,String>();
    for (var i = 0; i < values.length; i += 2) {
      m.put(stringify(values[i]), stringify(values[i + 1]));
    }
    return m;
  }
  ```

- [ ] **41. `jlist(Object...values)`** - Create JsonList from values
  ```java
  public static JsonList jlist(Object...values) {
    return new JsonList(values);
  }
  ```

### AssertionUtils Methods

- [ ] **42. `assertOneOf(T actual, T...expected)`** - Assert value is one of expected
  ```java
  public static final <T> T assertOneOf(T actual, T...expected) {
    for (T e : expected) {
      if (eq(actual,e)) return actual;
    }
    throw new AssertionError("Invalid value specified: " + actual);
  }
  ```

### DateUtils Methods

- [ ] **43. `calendar(String isoDateOrDuration)`** - Create Calendar from ISO date/duration/year/short format
  ```java
  public static Calendar calendar(String isoDateOrDuration) throws IllegalArgumentException {
    try {
      var x = isoDateOrDuration.charAt(0);
      if (x == 'P' || x == '-') {
        var c = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        var duration = Duration.parse(isoDateOrDuration).toMillis() / 1000;
        c.add(Calendar.SECOND, (int)duration);
        return c;
      }
      if (notContains(isoDateOrDuration, '-')) {
        if (isoDateOrDuration.length() == 4) isoDateOrDuration += "0101";
        Calendar c = new GregorianCalendar(TimeZone.getTimeZone("Z"));
        c.setTime(SIMPLIFIED_DATE.get().parse(isoDateOrDuration));
        return c;
      }
      if (notContains(isoDateOrDuration, 'T')) {
        isoDateOrDuration += "T00:00:00Z";
      }
      var zdt = ZonedDateTime.ofInstant(Instant.parse(isoDateOrDuration), ZoneId.of("Z"));
      return GregorianCalendar.from(zdt);
    } catch (DateTimeParseException | ParseException e) {
      throw new IllegalArgumentException(e);
    }
  }
  ```

- [ ] **44. `addSubtractDays(Calendar c, int days)`** - Add/subtract days from Calendar
  ```java
  public static Calendar addSubtractDays(Calendar c, int days) {
    return ofNullable(c)
      .map(x -> (Calendar)x.clone())
      .map(x -> add(x, Calendar.DATE, days))
      .orElse(null);
  }
  ```

- [ ] **45. `add(Calendar c, int field, int amount)`** - Add to calendar field
  ```java
  public static Calendar add(Calendar c, int field, int amount) {
    c.add(field, amount);
    return c;
  }
  ```

- [ ] **46. `toZonedDateTime(Calendar c)`** - Convert Calendar to ZonedDateTime
  ```java
  public static Optional<ZonedDateTime> toZonedDateTime(Calendar c) {
    return ofNullable(c).map(GregorianCalendar.class::cast).map(GregorianCalendar::toZonedDateTime);
  }
  ```

### Utility Methods (May Not Be Needed)

- [ ] **47. `stringSupplier(Supplier<?> s)`** - Convert Supplier to Supplier<String>
  ```java
  public static Supplier<String> stringSupplier(Supplier<?> s) {
    return () -> Utils.stringify(s.get());
  }
  ```
  *Note: This seems very specific to logging - may not be generally useful.*

## Summary

- **Total Methods in com.sfdc.irs.Utils**: ~47 unique methods
- **Methods Already in Juneau**: ~23 (wrappers or equivalents exist)
- **Methods to Evaluate**: ~47 (some overlap, some new)
- **Primary Targets**:
  - StringUtils: 16 methods
  - Utils: 9 methods
  - CollectionUtils: 13 methods
  - ThrowableUtils: 3 methods
  - AssertionUtils: 1 method
  - DateUtils: 4 methods

## Next Steps

1. Review each method and decide if it should be copied
2. For approved methods, copy to appropriate utility class
3. Add comprehensive unit tests
4. Update TODO-38 in TODO.md when complete

