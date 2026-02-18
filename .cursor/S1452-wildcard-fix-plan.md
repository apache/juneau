# Plan: SonarLint S1452 – Remove Usage of Generic Wildcard Type

## Rule Overview
Sonar rule **S1452** flags generic wildcards (e.g., `?`, `? extends T`) in return types and method signatures because they reduce type safety and clarity. Fixes typically involve:
1. Replacing with a type parameter
2. Using a more specific type
3. Using `@SuppressWarnings("java:S1452")` when the wildcard is intentional and necessary

---

## Approach Strategy

### Option A: `@SuppressWarnings("java:S1452")` (Preferred for most)
Use when the wildcard is **semantically correct** and required for the API:
- Heterogeneous collections (e.g., `List<AnnotationInfo<? extends Annotation>>`)
- Builder/fluent APIs where the second type parameter varies (e.g., `ObjectSwap<T,?>`)
- Types representing "any" or "unknown" (e.g., `ClassMeta<?>`)

### Option B: Type parameter substitution
Use when a **generic method** can preserve type safety:
- `List<AnnotationInfo<? extends Annotation>>` → `<A extends Annotation> List<AnnotationInfo<A>> find(...)` (only when a single annotation type is queried)
- Not applicable when returning mixed annotation types

### Option C: Replace with concrete type
Use when a **more specific type** fits:
- `?` → `Object` (when we only read/write Object)
- Rarely applicable without losing type information

---

## File-by-File Plan

### juneau-commons

| File | Line | Current | Recommended Fix |
|------|------|---------|-----------------|
| **AnnotationProvider.java** | 676, 699, 722, 749, 788 | `List<AnnotationInfo<? extends Annotation>>` | **Suppress** – heterogeneous annotations; `? extends Annotation` is required |
| **ClassInfo.java** | 721, 1770 | `List<AnnotationInfo<? extends Annotation>>` | **Suppress** – same as AnnotationProvider |
| **CollectionUtils.java** | 2259 | (verify exact usage) | **Suppress** or refactor after inspection |
| **ExecutableInfo.java** | 401 | (verify) | **Suppress** after inspection |
| **SortedArrayList.java** | 291 | (verify) | **Suppress** after inspection |
| **SortedLinkedList.java** | 271 | (verify) | **Suppress** after inspection |

### juneau-marshall – Swap classes

| File | Line | Current | Recommended Fix |
|------|------|---------|-----------------|
| **AutoListSwap.java** | 98 | `ObjectSwap<?,?>` | **Suppress** – `find()` returns dynamically typed swap |
| **AutoMapSwap.java** | 98 | `ObjectSwap<?,?>` | **Suppress** – same |
| **AutoNumberSwap.java** | 121 | `ObjectSwap<?,?>` | **Suppress** – same |
| **AutoObjectSwap.java** | 100 | `ObjectSwap<?,?>` | **Suppress** – same |
| **BuilderSwap.java** | 53, 96, 264 | `BuilderSwap<?,?>` | **Suppress** – builder pattern with varying types |
| **DefaultSwaps.java** | 79 | (verify) | **Suppress** |
| **ObjectSwap.java** | 179, 213, 408 | `ObjectSwap<T,?>`, `ClassMeta<?>` | **Suppress** – `?` for output type in fluent API, `ClassMeta<?>` for unknown class |
| **SurrogateSwap.java** | 55 | (verify) | **Suppress** |

### juneau-marshall – Bean / Class meta

| File | Line | Current | Recommended Fix |
|------|------|---------|-----------------|
| **Bean.java** | 207 | (verify) | **Suppress** |
| **BeanBuilder.java** | 129 | (verify) | **Suppress** |
| **BeanContext.java** | 3991 | (verify) | **Suppress** |
| **BeanFilter.java** | 893 | (verify) | **Suppress** |
| **BeanMapEntry.java** | 72 | (verify) | **Suppress** |
| **BeanPropertyMeta.java** | 799, 826 | `ClassMeta<?>`, etc. | **Suppress** – element/component types vary |
| **BeanPropertyValue.java** | 65 | (verify) | **Suppress** |
| **BeanRegistry.java** | 89 | (verify) | **Suppress** |
| **BeanSession.java** | 732, 1546 | (verify) | **Suppress** |
| **BeanTraverseSession.java** | 252, 360 | (verify) | **Suppress** |
| **ClassMeta.java** | 166–797, 1706, 1717 | `ClassMeta<?>`, `ObjectSwap<T,?>`, `Mutater<T,?>`, etc. | **Suppress** – `?` needed for element/component/key/value types |
| **ExtendedBeanMeta.java** | 44 | (verify) | **Suppress** |
| **ExtendedClassMeta.java** | 44 | (verify) | **Suppress** |

### juneau-marshall – Other

| File | Line | Current | Recommended Fix |
|------|------|---------|-----------------|
| **HtmlClassMeta.java** | 85 | (verify) | **Suppress** |
| **HttpPartSchema.java** | 4077 | (verify) | **Suppress** |
| **JsonList.java** | 789 | (verify) | **Suppress** |
| **JsonMap.java** | 1111 | (verify) | **Suppress** |
| **ObjectRest.java** | 513, 528, 571, 586 | (verify) | **Suppress** |
| **ParserSession.java** | 928 | (verify) | **Suppress** |
| **RequestBeanMeta.java** | 153 | (verify) | **Suppress** |
| **ResponseBeanMeta.java** | 214 | (verify) | **Suppress** |
| **SerializerSession.java** | 821, 1001 | (verify) | **Suppress** |

### juneau-rest-server

| File | Line | Current | Recommended Fix |
|------|------|---------|-----------------|
| **RestContext.java** | 2764 | (verify) | **Suppress** |
| **RestOpContext.java** | 1239 | (verify) | **Suppress** |

---

## Implementation Phases

### Phase 1: juneau-commons
1. Add `@SuppressWarnings("java:S1452")` to AnnotationProvider methods (lines 676, 699, 722, 749, 788).
2. Add suppression to ClassInfo (721, 1770).
3. Inspect and fix CollectionUtils, ExecutableInfo, SortedArrayList, SortedLinkedList.

### Phase 2: juneau-marshall – Swap classes
Add class- or method-level suppressions to Auto*Swap, BuilderSwap, DefaultSwaps, ObjectSwap, SurrogateSwap.

### Phase 3: juneau-marshall – Bean/Class meta
Add suppressions to Bean*, ClassMeta, Extended*, HtmlClassMeta, HttpPartSchema, Json*, ObjectRest, ParserSession, Request/ResponseBeanMeta, SerializerSession.

### Phase 4: juneau-rest-server
Add suppressions to RestContext, RestOpContext.

---

## Suppression Format

Use the project’s existing style:

```java
@SuppressWarnings("java:S1452") // Wildcard required - heterogeneous annotations / dynamic types
```

Or add to existing `@SuppressWarnings`:

```java
@SuppressWarnings({ "rawtypes", "java:S1452" }) // S1452: wildcard required for ...
```

---

## Alternative Consideration

A project-wide exclusion for S1452 in `sonar-project.properties` (or equivalent) could be used if:
- The codebase consistently relies on wildcards for reflection, beans, and dynamic typing.
- Fixing each occurrence would be high risk and low benefit.

Given the volume of issues, `@SuppressWarnings` per file or method is recommended over a global exclusion, so specific locations and reasons remain documented.
