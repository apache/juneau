# Feasibility Analysis: Overriding BctAssertions.DEFAULT_CONVERTER

## Overview

This document analyzes the feasibility of making `BctAssertions.DEFAULT_CONVERTER` a resettable, memoized thread-local field that can be overridden during test setup.

## Current State

- `DEFAULT_CONVERTER` is currently a `static final` field: `BasicBeanConverter.DEFAULT`
- Used throughout `BctAssertions` via: `args.getBeanConverter().orElse(DEFAULT_CONVERTER)`
- Custom converters can be provided per-assertion via `AssertionArgs.setBeanConverter()`
- TODO-88 exists to eliminate the need for `AssertionArgs` by making the default converter resettable

## Proposed Solution

### Core Implementation

```java
public class BctAssertions {
    // Thread-local with memoized default
    private static final ThreadLocal<ResettableSupplier<BeanConverter>> DEFAULT_CONVERTER = 
        ThreadLocal.withInitial(() -> memoizeResettable(() -> BasicBeanConverter.DEFAULT));
    
    // Get the current converter for this thread
    private static BeanConverter getDefaultConverter() {
        return DEFAULT_CONVERTER.get().get();
    }
    
    // Override the converter for this thread
    public static void setDefaultConverter(BeanConverter converter) {
        DEFAULT_CONVERTER.get().reset();
        // Need to set a new value - see implementation options below
    }
    
    // Reset to default for this thread
    public static void resetDefaultConverter() {
        DEFAULT_CONVERTER.get().reset();
    }
}
```

## JUnit 5 Parallel Execution Considerations

### How JUnit 5 Parallelization Works

1. **Test Method Parallelization**: When `junit.jupiter.execution.parallel.mode.default = concurrent`, each test method can run in its own thread
2. **Test Class Parallelization**: When `junit.jupiter.execution.parallel.mode.classes.default = concurrent`, different test classes run in parallel
3. **Thread Isolation**: Each test method thread has its own `ThreadLocal` storage

### Thread-Local Behavior

✅ **Advantages:**
- Each parallel test method gets its own converter instance
- No cross-thread interference
- Thread-safe by design

⚠️ **Challenges:**
- Tests in the same class running in parallel will have **separate** converter instances
- Cannot easily share a converter across all tests in a class when running in parallel

## Options for Class-Level Converter Sharing

### Option 1: Thread-Local Only (Method-Level Isolation)

**Behavior:**
- Each test method gets its own converter instance
- Tests in the same class running in parallel use different converters
- `@BeforeEach` can set converter per test method
- `@AfterEach` can reset converter per test method

**Pros:**
- Simple implementation
- No cross-test interference
- Works perfectly with parallel execution

**Cons:**
- Cannot share converter across all tests in a class when running in parallel
- Requires setting converter in each test method if you want customization

**Use Case:**
```java
@BeforeEach
void setUp() {
    var customConverter = BasicBeanConverter.builder()
        .defaultSettings()
        .addStringifier(MyType.class, obj -> obj.customFormat())
        .build();
    BctAssertions.setDefaultConverter(customConverter);
}

@AfterEach
void tearDown() {
    BctAssertions.resetDefaultConverter();
}
```

### Option 2: Class-Level Thread-Local with Synchronization

**Behavior:**
- Use a `ConcurrentHashMap<Class<?>, BeanConverter>` to store class-level converters
- Thread-local checks class-level first, then falls back to thread-local
- Requires synchronization or atomic operations

**Implementation Sketch:**
```java
private static final ConcurrentHashMap<Class<?>, BeanConverter> CLASS_CONVERTERS = new ConcurrentHashMap<>();
private static final ThreadLocal<ResettableSupplier<BeanConverter>> THREAD_CONVERTER = 
    ThreadLocal.withInitial(() -> memoizeResettable(() -> {
        // Check class-level first
        Class<?> testClass = findTestClass(); // Need to detect current test class
        BeanConverter classConverter = CLASS_CONVERTERS.get(testClass);
        return classConverter != null ? classConverter : BasicBeanConverter.DEFAULT;
    }));

public static void setDefaultConverterForClass(Class<?> testClass, BeanConverter converter) {
    CLASS_CONVERTERS.put(testClass, converter);
    // Invalidate thread-local cache for all threads running this class
    THREAD_CONVERTER.get().reset();
}
```

**Pros:**
- Can share converter across all tests in a class
- Still thread-safe

**Cons:**
- Complex implementation
- Requires detecting current test class (reflection/stack trace)
- Thread-local cache invalidation is tricky
- May not work well with nested test classes

### Option 3: Hybrid Approach with Test Class Detection

**Behavior:**
- Use `ThreadLocal` with a `ResettableSupplier` that checks for class-level overrides
- Detect test class from stack trace or via explicit registration
- Cache converter per thread, but check class-level map on cache miss

**Implementation Sketch:**
```java
private static final ConcurrentHashMap<Class<?>, BeanConverter> CLASS_CONVERTERS = new ConcurrentHashMap<>();
private static final ThreadLocal<ResettableSupplier<BeanConverter>> THREAD_CONVERTER = 
    ThreadLocal.withInitial(() -> memoizeResettable(() -> {
        Class<?> testClass = findTestClassFromStack();
        return CLASS_CONVERTERS.getOrDefault(testClass, BasicBeanConverter.DEFAULT);
    }));

private static Class<?> findTestClassFromStack() {
    StackTraceElement[] stack = Thread.currentThread().getStackTrace();
    for (StackTraceElement element : stack) {
        try {
            Class<?> clazz = Class.forName(element.getClassName());
            if (clazz.getName().endsWith("_Test") || clazz.isAnnotationPresent(TestClass.class)) {
                return clazz;
            }
        } catch (ClassNotFoundException e) {
            // Continue
        }
    }
    return null;
}
```

**Pros:**
- Automatic class detection
- Supports both class-level and method-level overrides
- Thread-safe

**Cons:**
- Stack trace inspection is expensive (but memoized)
- May incorrectly detect class in some scenarios
- Complex implementation

### Option 4: Explicit Test Class Registration (Recommended)

**Behavior:**
- Provide explicit methods for class-level and method-level overrides
- Use `@BeforeAll` to set class-level converter
- Use `@BeforeEach` to set method-level converter
- Thread-local stores the active converter

**Implementation:**
```java
public class BctAssertions {
    // Class-level converters (shared across all threads for a class)
    private static final ConcurrentHashMap<Class<?>, BeanConverter> CLASS_CONVERTERS = new ConcurrentHashMap<>();
    
    // Thread-local converter (method-level override)
    private static final ThreadLocal<ResettableSupplier<BeanConverter>> THREAD_CONVERTER = 
        ThreadLocal.withInitial(() -> memoizeResettable(() -> {
            // Check if current thread has a class-level converter
            Class<?> testClass = getTestClassForThread();
            if (testClass != null) {
                BeanConverter classConverter = CLASS_CONVERTERS.get(testClass);
                if (classConverter != null) {
                    return classConverter;
                }
            }
            return BasicBeanConverter.DEFAULT;
        }));
    
    // Set converter for all tests in a class
    public static void setDefaultConverterForClass(Class<?> testClass, BeanConverter converter) {
        CLASS_CONVERTERS.put(testClass, converter);
        // Note: Existing threads will continue using their cached value
        // New threads will pick up the class-level converter
    }
    
    // Set converter for current thread (method-level)
    public static void setDefaultConverter(BeanConverter converter) {
        // Store in a separate thread-local for method-level override
        METHOD_CONVERTER.set(converter);
        THREAD_CONVERTER.get().reset(); // Force recomputation
    }
    
    private static BeanConverter getDefaultConverter() {
        // Check method-level first, then class-level, then default
        BeanConverter methodConverter = METHOD_CONVERTER.get();
        if (methodConverter != null) {
            return methodConverter;
        }
        return THREAD_CONVERTER.get().get();
    }
}
```

**Usage:**
```java
public class MyTest extends TestBase {
    @BeforeAll
    static void setUpClass() {
        var classConverter = BasicBeanConverter.builder()
            .defaultSettings()
            .addStringifier(MyType.class, obj -> obj.customFormat())
            .build();
        BctAssertions.setDefaultConverterForClass(MyTest.class, classConverter);
    }
    
    @AfterAll
    static void tearDownClass() {
        BctAssertions.clearDefaultConverterForClass(MyTest.class);
    }
    
    @BeforeEach
    void setUp() {
        // Optional: Override for this specific test method
        // BctAssertions.setDefaultConverter(customConverter);
    }
    
    @AfterEach
    void tearDown() {
        // Optional: Reset method-level override
        // BctAssertions.resetDefaultConverter();
    }
}
```

**Pros:**
- Clear and explicit API
- Supports both class-level and method-level overrides
- Works with parallel execution
- No stack trace inspection needed
- Easy to understand and maintain

**Cons:**
- Requires explicit class registration in `@BeforeAll`
- Class-level converters persist until explicitly cleared (but that's usually desired)

## Recommended Implementation

I recommend **Option 4 (Explicit Test Class Registration)** because:

1. **Clarity**: Explicit is better than implicit - developers know exactly what's happening
2. **Performance**: No stack trace inspection overhead
3. **Flexibility**: Supports both class-level and method-level overrides
4. **Parallel-Safe**: Works correctly with JUnit 5 parallel execution
5. **Maintainability**: Simple to understand and debug

### Implementation Details

```java
public class BctAssertions {
    // Class-level converters (shared across threads for a class)
    private static final ConcurrentHashMap<Class<?>, BeanConverter> CLASS_CONVERTERS = new ConcurrentHashMap<>();
    
    // Method-level converter override (thread-local)
    private static final ThreadLocal<BeanConverter> METHOD_CONVERTER = new ThreadLocal<>();
    
    // Thread-local memoized supplier for class-level or default converter
    private static final ThreadLocal<ResettableSupplier<BeanConverter>> THREAD_CONVERTER = 
        ThreadLocal.withInitial(() -> memoizeResettable(() -> {
            // Find the test class for this thread
            Class<?> testClass = findTestClassForThread();
            if (testClass != null) {
                BeanConverter classConverter = CLASS_CONVERTERS.get(testClass);
                if (classConverter != null) {
                    return classConverter;
                }
            }
            return BasicBeanConverter.DEFAULT;
        }));
    
    // Internal: Get converter (checks method-level first, then class-level/default)
    private static BeanConverter getDefaultConverter() {
        BeanConverter methodConverter = METHOD_CONVERTER.get();
        if (methodConverter != null) {
            return methodConverter;
        }
        return THREAD_CONVERTER.get().get();
    }
    
    // Public API: Set converter for all tests in a class
    public static void setDefaultConverterForClass(Class<?> testClass, BeanConverter converter) {
        assertArgNotNull("testClass", testClass);
        assertArgNotNull("converter", converter);
        CLASS_CONVERTERS.put(testClass, converter);
        // Invalidate thread-local cache for this class's threads
        // Note: This is best-effort; existing threads may continue with cached value
        // until they call getDefaultConverter() again
    }
    
    // Public API: Clear converter for a class
    public static void clearDefaultConverterForClass(Class<?> testClass) {
        assertArgNotNull("testClass", testClass);
        CLASS_CONVERTERS.remove(testClass);
    }
    
    // Public API: Set converter for current thread (method-level override)
    public static void setDefaultConverter(BeanConverter converter) {
        assertArgNotNull("converter", converter);
        METHOD_CONVERTER.set(converter);
    }
    
    // Public API: Reset converter for current thread (clears method-level override)
    public static void resetDefaultConverter() {
        METHOD_CONVERTER.remove();
        THREAD_CONVERTER.get().reset(); // Also reset class-level cache
    }
    
    // Internal: Find test class for current thread (simplified - may need refinement)
    private static Class<?> findTestClassForThread() {
        // This is a simplified version - you may want to cache this per thread
        // or use a more sophisticated detection mechanism
        StackTraceElement[] stack = Thread.currentThread().getStackTrace();
        for (StackTraceElement element : stack) {
            String className = element.getClassName();
            if (className.endsWith("_Test") || className.contains("Test")) {
                try {
                    return Class.forName(className);
                } catch (ClassNotFoundException e) {
                    // Continue
                }
            }
        }
        return null;
    }
    
    // Update all methods to use getDefaultConverter() instead of DEFAULT_CONVERTER
    // Example:
    public static void assertBean(Object actual, String fields, String expected) {
        assertBean(args(), actual, fields, expected);
    }
    
    public static void assertBean(AssertionArgs args, Object actual, String fields, String expected) {
        // ... existing code ...
        var converter = args.getBeanConverter().orElse(getDefaultConverter());
        // ... rest of method ...
    }
}
```

## Alternative: Simpler Thread-Local Only Approach

If class-level sharing is not a requirement, a simpler implementation is:

```java
public class BctAssertions {
    private static final ThreadLocal<ResettableSupplier<BeanConverter>> DEFAULT_CONVERTER = 
        ThreadLocal.withInitial(() -> memoizeResettable(() -> BasicBeanConverter.DEFAULT));
    
    private static BeanConverter getDefaultConverter() {
        return DEFAULT_CONVERTER.get().get();
    }
    
    public static void setDefaultConverter(BeanConverter converter) {
        assertArgNotNull("converter", converter);
        // Store override in a separate thread-local
        CONVERTER_OVERRIDE.set(converter);
        DEFAULT_CONVERTER.get().reset(); // Invalidate cache
    }
    
    public static void resetDefaultConverter() {
        CONVERTER_OVERRIDE.remove();
        DEFAULT_CONVERTER.get().reset();
    }
    
    private static final ThreadLocal<BeanConverter> CONVERTER_OVERRIDE = new ThreadLocal<>();
    
    private static BeanConverter getDefaultConverter() {
        BeanConverter override = CONVERTER_OVERRIDE.get();
        if (override != null) {
            return override;
        }
        return DEFAULT_CONVERTER.get().get();
    }
}
```

This simpler approach:
- ✅ Works perfectly with parallel execution
- ✅ Each test method can set its own converter
- ✅ No class-level sharing (each test is independent)
- ✅ Much simpler implementation

## Questions to Answer

1. **Do you need class-level converter sharing?**
   - If YES → Use Option 4 (Explicit Registration)
   - If NO → Use simpler thread-local only approach

2. **How should converter be set in test setup?**
   - `@BeforeAll` for class-level?
   - `@BeforeEach` for method-level?
   - Both?

3. **Should converter persist across test methods in a class?**
   - If tests run sequentially → Yes, can share
   - If tests run in parallel → Each thread gets its own

## Recommendation

Start with the **simpler thread-local only approach** unless you have a specific need for class-level sharing. You can always add class-level support later if needed.

The simpler approach:
- Solves the core problem (eliminating need for AssertionArgs)
- Works perfectly with parallel execution
- Is easy to implement and maintain
- Can be enhanced later if class-level sharing is needed

