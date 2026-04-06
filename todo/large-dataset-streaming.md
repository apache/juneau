# Large Dataset Streaming Plan

Add built-in serializer/parser support for large datasets via lazy producers (Supplier/Stream) and
consumers (Consumer), avoiding loading entire collections into memory. Includes lifecycle management
for database-backed implementations.

---

## Key Design Decision: `Supplier<T>` and End-of-Input

`java.util.function.Supplier<T>` has **no end-of-input mechanism** — it only has `T get()`,
returning one value. It cannot model a sequence. The right data structures are:

- **`Stream<T>`** — already supported in Juneau 9.2.1 for both top-level and bean properties; lazy,
  auto-signals end when exhausted, `AutoCloseable` for lifecycle
- **`Supplier<T>`** — should be treated as a **single-value lazy wrapper** analogous to `Optional<T>`:
  the serializer calls `get()` and serializes the result transparently; the parser wraps the result
  in a `Supplier`
- **`BeanSupplier<T>`** (new) — lifecycle-aware wrapper around a sequence; extends `Iterable<T>` so
  existing `forEachStreamableEntry()` handles it; adds `begin()`/`complete()` hooks for database
  lifecycle

**Recommendation:** `Stream<T>` for sequences (already works), `BeanSupplier<T>` when lifecycle
callbacks are needed, `Supplier<T>` for single lazy values.

---

## New Interfaces (juneau-commons)

Four new interfaces in `juneau-core/juneau-commons/src/main/java/org/apache/juneau/commons/`:

```java
// Universal factory interface — used with @Bean(factory=X.class) for any bean type
@FunctionalInterface
public interface BeanFactory<T> {
    T create();
}

// For parsers — receives deserialized beans one at a time
// Extends ThrowingConsumer<T> (which extends Consumer<T>) so acceptThrows() supports checked exceptions
public interface BeanConsumer<T> extends ThrowingConsumer<T> {
    default void begin() throws Exception {}    // called before first accept()
    default void complete() throws Exception {} // always called (like finally) — close resources
    default void onError(Exception e) throws Exception { throw e; } // rollback; absorb to skip, rethrow to stop
}

// For serializers — provides beans lazily with lifecycle
public interface BeanSupplier<T> extends Iterable<T> {
    default void begin() throws Exception {}    // called before iteration starts
    default void complete() throws Exception {} // always called (like finally) — close resources
    default void onError(Exception e) throws Exception { throw e; } // called on iteration failure
}

// For round-trip — combines both supplier and consumer on the same property
public interface BeanChannel<T> extends BeanSupplier<T>, BeanConsumer<T> {
    @Override default void begin() throws Exception {}
    @Override default void complete() throws Exception {}
    @Override default void onError(Exception e) throws Exception { throw e; }
}
```

**Interface hierarchy:**
- `BeanConsumer<T>` — parse only (implements `ThrowingConsumer<T>`)
- `BeanSupplier<T>` — serialize only (implements `Iterable<T>`)
- `BeanChannel<T>` — both directions (extends both)

`BeanFactory<T>` is the single universal factory interface. A Spring `@Component` that creates
`ItemChannel` instances simply implements `BeanFactory<ItemChannel>`.

`BeanSupplier<T>` deliberately extends `Iterable<T>` so ClassMeta's existing `ITERABLE` category
picks it up automatically — no new category needed.

**Direction validation:** The framework validates that the correct interface is used for each
direction. Since `BeanChannel` extends both interfaces, the checks must test for *pure* types:
- Serializer: if `o instanceof BeanConsumer && !(o instanceof BeanSupplier)` →
  `"Cannot serialize a BeanConsumer. Use BeanSupplier for serialization or BeanChannel for round-trip."`
- Parser: if target `instanceof BeanSupplier && !(target instanceof BeanConsumer)` →
  `"Cannot parse into a BeanSupplier. Use BeanConsumer for parsing or BeanChannel for round-trip."`

A `BeanChannel` passes both checks because it implements both interfaces.

**BeanChannel lifecycle — direction is implicit (Option A):** The `BeanChannel` shares
`begin()`/`complete()`/`onError()` across both directions. The implementation knows which
direction it's in based on which data method is called first after `begin()`:
- `iterator()` called → read mode (serialization)
- `acceptThrows()` called → write mode (parsing)

---

## Built-in Implementations (no factory required)

For implementations that manage their own storage, no factory or DI framework is needed.
Provide these in `juneau-commons`:

```java
// Round-trip in-memory implementation — collects on parse, iterates on serialize
public class ListBeanChannel<T> implements BeanChannel<T> {
    private final List<T> list = new ArrayList<>();
    @Override public void acceptThrows(T item) { list.add(item); }
    @Override public Iterator<T> iterator() { return list.iterator(); }
    public List<T> getList() { return list; }
}
```

`ListBeanChannel<T>` replaces the need for separate `ListBeanConsumer` and `IterableBeanSupplier`
implementations — it handles both directions. Users can subclass it to add custom behavior.

These are instantiated directly by `BeanCreator` (no-arg constructor) — no factory annotation
or BeanStore needed.

---

## Phase 1: `Supplier<T>` Single-Value Unwrapping (Serializer)

Model after `Optional<T>` handling in `BeanTraverseSession`.

**Unwrapping is recursive** — `Supplier<Supplier<T>>` unwraps fully to `T`. A depth guard of 10
levels prevents infinite loops from self-referential suppliers and throws `IllegalArgumentException`
if exceeded (same pattern as the existing cyclic object graph guard):

```java
private Object unwrapSupplier(Object o, int depth) {
    if (depth > 10)
        throw illegalArg("Supplier chain exceeds maximum unwrap depth of 10");
    return o instanceof Supplier<?> s ? unwrapSupplier(s.get(), depth + 1) : o;
}
```

Files to change:

- `ClassMeta.java` — add `isSupplier()` / `isSupplierType()` detection alongside `isOptional()`
- `BeanTraverseSession.java` — add `unwrapSupplier(Object, int)` helper; call at the early-unwrap
  block in `getExpectedRootType()` and `serializeAnything()`
- All `serializeAnything()` implementations — add transparent `Supplier` unwrap before type dispatch
  (same location as `Optional` unwrap)

Parser side: when target `ClassMeta` is `Supplier`, wrap the parsed value in a `() -> value`
lambda.

---

## Phase 2: `Stream<T>` Lifecycle (close after serialization)

`Stream<T>` implements `AutoCloseable`. Currently `forEachStreamableEntry()` in `SerializerSession.java`
calls `stream.forEach()` but never `stream.close()`.

Change `forEachStreamableEntry()` to close streams after consumption:

```java
} else if (type.isStream()) {
    try (Stream s2 = (Stream)o) {
        s2.forEach(consumer);
    }
}
```

This handles database-backed `Stream<T>` with connection lifecycle automatically.

---

## Phase 3: `BeanSupplier<T>` — Lifecycle-Aware Producer

Since `BeanSupplier<T>` extends `Iterable<T>`, ClassMeta already classifies it as `ITERABLE`.
The only additions needed:

- In `SerializerSession.serialize(Object o, Object out)`: before calling `doSerialize()`, check if
  `o instanceof BeanSupplier`; if so drive the full lifecycle:
  ```java
  if (o instanceof BeanSupplier<?> bs) {
      bs.begin();
      try {
          doSerialize(out, o);
      } catch (Exception e) {
          bs.onError(e);  // rollback / error-specific handling
      } finally {
          bs.complete();  // always: close cursor, release resources
      }
  }
  ```
- Same check in `forEachStreamableEntry()` for property-level `BeanSupplier` values

---

## Phase 4: `BeanConsumer<T>` — Parser Target

New `parse()` overloads in `ParserSession.java`:

```java
// Parse a JSON array / collection, calling consumer.accept() per element
public <T> void parse(Object input, BeanConsumer<T> consumer, Class<T> type) throws ParseException
public <T> void parse(Object input, BeanConsumer<T> consumer, Type type, Type...args) throws ParseException
```

Lifecycle contract:

1. Call `consumer.begin()`
2. Parse outer array/object wrapper
3. For each element:
   a. Parse element token, call `consumer.acceptThrows(element)` (checked exceptions supported)
   b. If exception: call `consumer.onError(e)`
      - If `onError()` absorbs (doesn't rethrow): continue to next element (skip-and-continue)
      - If `onError()` rethrows: stop parsing, propagate as `ParseException`
4. **Always** call `consumer.complete()` (like `finally`) — whether parsing succeeded or
   `onError()` rethrew

`complete()` acts as a resource cleanup hook (close statement/connection) and is always called.
`onError()` handles error-specific logic (rollback). This separates rollback from cleanup:

```java
@Override public void onError(Exception e) throws Exception {
    conn.rollback();   // error-specific: undo partial work
    throw e;
}
@Override public void complete() throws Exception {
    stmt.close();      // always: release resources
    conn.close();
}
```

The default `onError()` rethrows, so the default behavior is stop-on-first-error. Users can
override to absorb errors for fault-tolerant ingestion (e.g., log bad records and skip them).

**Implementation: format-specific `doParse()` hooks (true streaming).** Each format-specific parser
session implements a new low-level hook:

```java
protected abstract <T> void doParse(ParserPipe pipe, BeanConsumer<T> consumer, ClassMeta<T> type)
    throws IOException, ParseException;
```

This ensures elements are passed to `consumer.accept()` one at a time as they are read from the
input stream — no intermediate buffering of the full collection. Each format's existing
`doParseCollection()` logic is the natural starting point for the per-element loop.

Files to change: `ReaderParserSession`, `InputStreamParserSession`, and each concrete format
session — `JsonParserSession`, `XmlParserSession`, `CsvParserSession`, `BsonParserSession`,
`MsgPackParserSession`, `UonParserSession`, `UrlEncodingParserSession`, etc.

---

## Spring / DI Framework Integration

### Overview

`@Bean(factory=X.class)` is a **universal DI hook** for any class the framework needs to
instantiate — not just `BeanConsumer`/`BeanSupplier`. The same mechanism applies to:

- `BeanConsumer` / `BeanSupplier` subclasses (per-request stateful, must be created fresh)
- `ObjectSwap` subclasses (previously forced to use no-arg constructors; now can receive injection)
- Any ordinary bean class parsed from JSON/XML/etc. (factory provides the instance, parser sets properties)

Two annotation targets:

1. **`@Bean(factory=X.class)`** — on the target class itself; used whenever that class is instantiated
2. **`@Beanp(factory=X.class)`** — on a bean property getter/setter; overrides factory for one property
   (needed when the declared property type is an interface)

### Universal `BeanCreator` integration

`BeanCreator` already centralizes all bean instantiation. The factory check becomes its first step:

```
BeanCreator instantiating class C:
  1. Does C have @Bean(factory=X.class)?
     YES → try beanStore.getBean(X) — if found, use it
           else try to instantiate X directly (no-arg constructor / getInstance())
           if both fail → throw IllegalArgumentException
           → call BeanFactory<C>.create()
     NO  → existing logic: getInstance(), constructors, builder pattern, etc.
```

This degrades gracefully — works without Spring (factory instantiated directly), works with Spring
(factory fetched from `SpringBeanStore`). Classes with no factory annotation are unaffected.
A clear exception is thrown if the factory class cannot be resolved by either means, preventing
silent failures.

### What this unifies

| Class type | Before | After |
|---|---|---|
| Ordinary parsed bean | Reflection/constructor | Factory (if annotated) |
| `ObjectSwap` | No-arg constructor only | Factory (if annotated) — Spring injection now possible |
| `BeanConsumer` | Same | Factory → lifecycle driven by parser |
| `BeanSupplier` | Same | Factory → lifecycle driven by serializer |
| `BeanChannel` | N/A | Factory → round-trip; serializer/parser drive same instance |

### Spring example (property-level)

```java
// Spring singleton factory — implements BeanFactory<ItemConsumer>
@Component
public class ItemConsumerFactory implements BeanFactory<ItemConsumer> {
    @Autowired ItemRepository repo;

    @Override
    public ItemConsumer create() {
        return new ItemConsumer(repo.openBatch());
    }
}

// BeanConsumer subclass declares its factory via @Bean
@Bean(factory=ItemConsumerFactory.class)
public class ItemConsumer implements BeanConsumer<Item> {
    private final BatchWriter writer;
    public ItemConsumer(BatchWriter writer) { this.writer = writer; }

    @Override public void begin() { writer.open(); }
    @Override public void accept(Item item) { writer.write(item); }
    @Override public void complete() { writer.commit(); writer.close(); }
    @Override public void onError(Exception e) throws Exception { writer.rollback(); throw e; }
}

// Bean class declares property using ItemConsumer type
public class ItemCollection {
    // Parser creates ItemConsumer via factory, calls begin()/accept()/complete()
    @Beanp(type=Item.class)
    public ItemConsumer getItems() { ... }
}
```

### Spring example (round-trip BeanChannel)

```java
// Factory creates a DB-backed channel that reads/writes via JDBC
@Component
public class ItemChannelFactory implements BeanFactory<ItemChannel> {
    @Autowired DataSource ds;

    @Override
    public ItemChannel create() {
        return new ItemChannel(ds);
    }
}

@Bean(factory=ItemChannelFactory.class)
public class ItemChannel implements BeanChannel<Item> {
    private final DataSource ds;
    private Connection conn;

    @Override public void begin() throws Exception {
        conn = ds.getConnection();
        conn.setAutoCommit(false);
    }

    // Serializer calls iterator() — read mode
    @Override public Iterator<Item> iterator() {
        var rs = conn.prepareStatement("SELECT * FROM items").executeQuery();
        return new ResultSetIterator<>(rs, Item::fromRow);
    }

    // Parser calls acceptThrows() — write mode
    @Override public void acceptThrows(Item item) throws Exception {
        var stmt = conn.prepareStatement("INSERT INTO items (name, price) VALUES (?, ?)");
        stmt.setString(1, item.getName());
        stmt.setBigDecimal(2, item.getPrice());
        stmt.executeUpdate();
    }

    @Override public void onError(Exception e) throws Exception {
        conn.rollback();
        throw e;
    }

    @Override public void complete() throws Exception {
        conn.commit();
        conn.close();
    }
}
```

### Spring example (top-level parsing)

The `@Bean(factory=...)` on the class means the call site needs no special annotation:

```java
// Parser sees ItemConsumer is a BeanConsumer, reads @Bean(factory=ItemConsumerFactory.class),
// fetches factory from BeanStore, calls factory.create(), drives lifecycle
parser.parse(input, ItemConsumer.class);

// Same for BeanChannel — parser sees it's a BeanConsumer (via BeanChannel extends BeanConsumer)
parser.parse(input, ItemChannel.class);

// Serializer sees ItemChannel is a BeanSupplier (via BeanChannel extends BeanSupplier)
serializer.serialize(channelFactory.create(), output);
```

### `BeanContext.Builder.beanStore(BeanStore)` change

`BeanContext` needs a `BeanStore` reference so sessions can resolve factories:

- Add `BeanStore beanStore` field to `BeanContext.Builder` (uses the `juneau-commons`
  `BeanStore` interface — no layering violation)
- In `RestContext.createBeanContext(BasicBeanStore beanStore, ...)`, add one line:
  `builder.beanStore(beanStore)` — the `SpringBeanStore` is already available here
- `SerializerSession` and `ParserSession` access it via `ctx.getBeanStore()`

No Spring-specific types leak into `BeanContext` — it only knows about the `BeanStore` interface.
The `SpringBeanStore` implementation stays in the REST spring module.

---

## Phase 5: Property-Level Annotation Support

Due to type erasure, `Stream<MyBean>` or `BeanSupplier<MyBean>` on a getter loses the `MyBean`
type parameter at runtime. A new `elementType=` attribute is added to `@Beanp`.

`elementType=` mirrors the semantics of the existing `type=` attribute (which applies to the
property itself) but applied to the **elements** within the stream/supplier/consumer. It serves
three purposes:

1. **Type erasure resolution** — supply the generic `T` when it's lost at runtime
2. **Narrowing** — use a more specific subtype than the declared element type
3. **Concrete implementation** — supply a concrete class when the element type is abstract or an interface

```java
// Type erasure resolution — T cannot be inferred at runtime without this
@Beanp(elementType=MyBean.class)
public Stream<MyBean> getItems() { ... }

// Narrowing — stream is declared as Animal but elements are always Dog
@Beanp(elementType=Dog.class)
public BeanSupplier<Animal> getItems() { ... }

// Concrete implementation — element type is an interface; use ArrayList for deserialization
@Beanp(elementType=ArrayList.class)
public BeanConsumer<List<String>> setItems() { ... }
```

`elementType=` is distinct from the existing `type=` (which specifies the property's own
implementation class). The framework determines directionality from the property return/parameter
type — no separate `streamType=` / `consumerType=` attributes needed.

Changes to `BeanPropertyMeta.Builder.validate()`:
- Detect `Supplier`/`BeanSupplier`/`Consumer`/`BeanConsumer`/`BeanChannel` return/parameter types
- Extract element type from `@Beanp(elementType=)`, falling back to generic type inspection via
  `ClassInfo` when the annotation is absent
- Property getter returning `BeanSupplier<T>` or `BeanChannel<T>` on serialization: call `begin()`,
  iterate, call `complete()`
- Property accepting `BeanConsumer<T>` or `BeanChannel<T>` on parsing: call consumer per element

**Getter-only `BeanConsumer`/`BeanChannel` properties** (no setter required):

`BeanConsumer<T>` and `BeanChannel<T>` mirror the existing getter-only collection reuse pattern —
the parser calls the getter to obtain the consumer, then drives it via the lifecycle protocol.
No setter is needed:

| Collection (existing behavior) | `BeanConsumer<T>` / `BeanChannel<T>` (new) |
|---|---|
| Get existing list via getter | Get existing consumer/channel via getter |
| `list.clear()` | `consumer.begin()` |
| `list.add(element)` per element | `consumer.acceptThrows(element)` per element |
| *(none)* | `consumer.complete()` |

```java
public class MyBean {
    private final ItemChannel channel = new ItemChannel();
    public ItemChannel getItems() { return channel; } // no setter needed; round-trip capable
}
```

`BeanPropertyMeta` changes needed:
- Add a new `isBeanConsumer` branch alongside the existing `isCollection` / `isMap` branches in
  `set()` — matches both `BeanConsumer` and `BeanChannel` (since `BeanChannel extends BeanConsumer`)
- Set `canWrite = true` for getter-only `BeanConsumer`/`BeanChannel` properties (mirrors collection
  behavior at line 317: `canWrite |= (nn(field) || nn(setter) || isConstructorArg)` —
  add `|| isBeanConsumer`)

---

## Phase 6: CSV Integration

CSV's row-per-bean model maps naturally to streaming:

- **Serializer**: Accept `Stream<T>` / `BeanSupplier<T>` / `BeanChannel<T>` as top-level input →
  write header row from ClassMeta, then one data row per element (largely works already via
  existing STREAM support)
- **Parser**: Accept `BeanConsumer<T>` / `BeanChannel<T>` as parser target → parse header row,
  then call `consumer.acceptThrows()` per data row

---

## Implementation Order

1. ✅ New interfaces in `juneau-commons`: `BeanFactory<T>`, `BeanConsumer<T>`, `BeanSupplier<T>`,
   `BeanChannel<T>`
2. ✅ Built-in implementation: `ListBeanChannel<T>`
3. ✅ `BeanContext.Builder.beanStore(BeanStore)` + wire into `RestContext.createBeanContext()`
4. ✅ `@Bean(factory=X.class)` attribute + universal factory resolution in `BeanCreator`
5. ✅ `Supplier<T>` single-value unwrapping (ClassMeta + all serializer sessions)
6. ✅ `Stream<T>` auto-close in `forEachStreamableEntry()`
7. ✅ `BeanSupplier<T>` lifecycle calls + direction validation in `SerializerSession.serialize()`
8. ✅ `BeanConsumer<T>` parse overloads + direction validation in `ParserSession` + format implementations
9. ✅ `@Beanp(factory=X.class)` attribute + property-level factory resolution
10. ✅ `@Beanp(elementType=)` new attribute for streaming/consumer element type declaration
11. ✅ Property-level wiring in `BeanPropertyMeta`
12. ~~CSV-specific integration~~ (cancelled - not required for initial implementation)
13. ✅ Tests for each phase (`BeanChannel_Test`, `BeanStreaming_Test`)
14. ✅ `BeanChannel` round-trip tests in existing `RoundTrip` test suite (`RoundTripBeanChannel_Test`)
15. ✅ Update release notes (`RELEASE-NOTES.txt` 9.2.1 section)
16. ✅ Update Javadoc overview; full Docusaurus docs pending (no Docusaurus site in this repo yet)

---

## Status: IMPLEMENTED ✅

All phases complete. The large-dataset streaming APIs are production-ready.

**New classes/interfaces added:**
- `org.apache.juneau.commons.function.BeanFactory<T>`
- `org.apache.juneau.commons.function.BeanConsumer<T>`
- `org.apache.juneau.commons.function.BeanSupplier<T>`
- `org.apache.juneau.commons.function.BeanChannel<T>`
- `org.apache.juneau.commons.function.ListBeanChannel<T>`

**Annotations updated:**
- `@Bean(factory=X.class)` - class-level factory for DI integration
- `@Beanp(factory=X.class)` - property-level factory
- `@Beanp(elementType=Y.class)` - element type for streaming/generic properties

**Tests added:**
- `juneau-utest/.../commons/function/BeanChannel_Test.java`
- `juneau-utest/.../BeanStreaming_Test.java`
- `juneau-utest/.../a/rttests/RoundTripBeanChannel_Test.java`
