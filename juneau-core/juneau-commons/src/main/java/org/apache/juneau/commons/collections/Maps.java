/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.juneau.commons.collections;

import static org.apache.juneau.commons.utils.AssertionUtils.*;
import static org.apache.juneau.commons.utils.ThrowableUtils.*;
import static org.apache.juneau.commons.utils.Utils.*;

import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;

/**
 * A fluent builder for constructing {@link Map} instances with various configuration options.
 *
 * <p>
 * This builder provides a flexible and type-safe way to construct maps with support for adding entries,
 * other maps, sorting by keys, and applying modifiers like unmodifiable or sparse modes. It's particularly
 * useful when constructing maps dynamically from multiple sources or with conditional entries.
 *
 * <p>
 * Instances of this builder can be created using {@link #create(Class, Class)} or the convenience method
 * {@link org.apache.juneau.commons.utils.CollectionUtils#mapb(Class, Class)}.
 *
 * <h5 class='section'>Features:</h5>
 * <ul class='spaced-list'>
 * 	<li>Fluent API - all methods return <c>this</c> for method chaining
 * 	<li>Multiple add methods - single entries, pairs, other maps
 * 	<li>Arbitrary input support - automatic type conversion with {@link #addAny(Object...)}
 * 	<li>Pair adding - {@link #addPairs(Object...)} for varargs key-value pairs
 * 	<li>Filtering support - exclude unwanted entries via {@link #filtered()} or {@link #filtered(BiPredicate)}
 * 	<li>Sorting support - natural key order or custom {@link Comparator}
 * 	<li>Sparse mode - return <jk>null</jk> for empty maps
 * 	<li>Unmodifiable mode - create immutable maps
 * 	<li>Custom conversion functions - type conversion via {@link #keyFunction(Function)} and {@link #valueFunction(Function)}
 * </ul>
 *
 * <h5 class='section'>Examples:</h5>
 * <p class='bjava'>
 * 	<jk>import static</jk> org.apache.juneau.commons.utils.CollectionUtils.*;
 *
 * 	<jc>// Basic usage - returns Map</jc>
 * 	Map&lt;String,Integer&gt; <jv>map</jv> = <jsm>mapb</jsm>(String.<jk>class</jk>, Integer.<jk>class</jk>)
 * 		.add(<js>"one"</js>, 1)
 * 		.add(<js>"two"</js>, 2)
 * 		.add(<js>"three"</js>, 3)
 * 		.build();
 *
 * 	<jc>// Using pairs - returns Map</jc>
 * 	Map&lt;String,String&gt; <jv>props</jv> = <jsm>mapb</jsm>(String.<jk>class</jk>, String.<jk>class</jk>)
 * 		.addPairs(<js>"host"</js>, <js>"localhost"</js>, <js>"port"</js>, <js>"8080"</js>)
 * 		.build();
 *
 * 	<jc>// With sorting by key - returns Map</jc>
 * 	Map&lt;String,Integer&gt; <jv>sorted</jv> = <jsm>mapb</jsm>(String.<jk>class</jk>, Integer.<jk>class</jk>)
 * 		.add(<js>"zebra"</js>, 3)
 * 		.add(<js>"apple"</js>, 1)
 * 		.add(<js>"banana"</js>, 2)
 * 		.sorted()
 * 		.build();  <jc>// Returns TreeMap with natural key order</jc>
 *
 * 	<jc>// Immutable map - returns Map</jc>
 * 	Map&lt;String,String&gt; <jv>config</jv> = <jsm>mapb</jsm>(String.<jk>class</jk>, String.<jk>class</jk>)
 * 		.add(<js>"env"</js>, <js>"prod"</js>)
 * 		.add(<js>"region"</js>, <js>"us-west"</js>)
 * 		.unmodifiable()
 * 		.build();
 *
 * 	<jc>// From multiple sources - returns Map</jc>
 * 	Map&lt;String,Integer&gt; <jv>existing</jv> = Map.of(<js>"a"</js>, 1, <js>"b"</js>, 2);
 * 	Map&lt;String,Integer&gt; <jv>combined</jv> = <jsm>mapb</jsm>(String.<jk>class</jk>, Integer.<jk>class</jk>)
 * 		.addAll(<jv>existing</jv>)
 * 		.add(<js>"c"</js>, 3)
 * 		.build();
 *
 * 	<jc>// Sparse mode - returns null when empty</jc>
 * 	Map&lt;String,String&gt; <jv>maybeNull</jv> = <jsm>mapb</jsm>(String.<jk>class</jk>, String.<jk>class</jk>)
 * 		.sparse()
 * 		.build();  <jc>// Returns null, not empty map</jc>
 *
 * 	<jc>// FluentMap wrapper - use buildFluent()</jc>
 * 	FluentMap&lt;String,Integer&gt; <jv>fluent</jv> = <jsm>mapb</jsm>(String.<jk>class</jk>, Integer.<jk>class</jk>)
 * 		.add(<js>"one"</js>, 1)
 * 		.buildFluent();
 *
 * 	<jc>// FilteredMap - use buildFiltered()</jc>
 * 	FilteredMap&lt;String,Integer&gt; <jv>filtered</jv> = <jsm>mapb</jsm>(String.<jk>class</jk>, Integer.<jk>class</jk>)
 * 		.filtered((k, v) -&gt; v &gt; 0)
 * 		.add(<js>"a"</js>, 5)
 * 		.add(<js>"b"</js>, -1)  <jc>// Filtered out</jc>
 * 		.buildFiltered();
 * </p>
 *
 * <h5 class='section'>Thread Safety:</h5>
 * <p>
 * This class is <b>not thread-safe</b>. Each builder instance should be used by a single thread or
 * properly synchronized.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauCommonsCollections">Collections Package</a>
 * 	<li class='jc'>{@link Lists}
 * 	<li class='jc'>{@link Sets}
 * </ul>
 *
 * @param <K> The key type.
 * @param <V> The value type.
 */
public class Maps<K,V> {

	/**
	 * Static creator.
	 *
	 * @param <K> Key type.
	 * @param <V> Value type.
	 * @param keyType The key type. Must not be <jk>null</jk>.
	 * @param valueType The value type. Must not be <jk>null</jk>.
	 * @return A new builder.
	 */
	public static <K,V> Maps<K,V> create(Class<K> keyType, Class<V> valueType) {
		return new Maps<>(assertArgNotNull("keyType", keyType), assertArgNotNull("valueType", valueType));
	}

	/**
	 * Static creator without explicit type parameters.
	 *
	 * <p>
	 * This is a convenience method that creates a builder without requiring explicit type parameters.
	 * The types will be inferred from usage context. Internally uses <c>Object.class</c> for both
	 * key and value types, which allows any types to be added.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	Map&lt;String, Integer&gt; <jv>map</jv> = Maps.<jsm>create</jsm>()
	 * 		.add(<js>"one"</js>, 1)
	 * 		.add(<js>"two"</js>, 2)
	 * 		.build();
	 * </p>
	 *
	 * @param <K> Key type.
	 * @param <V> Value type.
	 * @return A new builder.
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static <K,V> Maps<K,V> create() {
		return new Maps(Object.class, Object.class);
	}

	private Map<K,V> map;
	private boolean unmodifiable = false, sparse = false, concurrent = false, ordered = false;
	private Comparator<K> comparator;

	private BiPredicate<K,V> filter;
	private Class<K> keyType;
	private Class<V> valueType;

	private Function<Object,K> keyFunction;
	private Function<Object,V> valueFunction;

	/**
	 * Constructor.
	 *
	 * @param keyType The key type. Must not be <jk>null</jk>.
	 * @param valueType The value type. Must not be <jk>null</jk>.
	 */
	public Maps(Class<K> keyType, Class<V> valueType) {
		this.keyType = assertArgNotNull("keyType", keyType);
		this.valueType = assertArgNotNull("valueType", valueType);
	}

	/**
	 * Adds a single entry to this map.
	 *
	 * <p>
	 * Note: Filtering is applied at build time, not when adding entries.
	 *
	 * @param key The map key.
	 * @param value The map value.
	 * @return This object.
	 */
	public Maps<K,V> add(K key, V value) {
		if (map == null)
			map = new LinkedHashMap<>();
		map.put(key, value);
		return this;
	}

	/**
	 * Appends the contents of the specified map into this map.
	 *
	 * <p>
	 * This is a no-op if the value is <jk>null</jk>.
	 *
	 * <p>
	 * Note: Filtering is applied at build time, not when adding entries.
	 *
	 * @param value The map to add to this map.
	 * @return This object.
	 */
	public Maps<K,V> addAll(Map<K,V> value) {
		if (nn(value)) {
			if (map == null)
				map = new LinkedHashMap<>();
			map.putAll(value);
		}
		return this;
	}

	/**
	 * Adds arbitrary values to this map.
	 *
	 * <p>
	 * Objects can be any of the following:
	 * <ul>
	 * 	<li>Maps of key/value types convertible to the key/value types of this map.
	 * </ul>
	 *
	 * <p>
	 * Each entry from the maps will be added using {@link #add(Object, Object)}, which applies
	 * key/value function conversion if configured. Non-Map objects will cause a {@link RuntimeException} to be thrown.
	 *
	 * @param values The values to add. Can contain <jk>null</jk> values (ignored).
	 * @return This object.
	 * @throws RuntimeException If a non-Map object is provided.
	 */
	@SuppressWarnings("unchecked")
	public Maps<K,V> addAny(Object...values) {
		for (var o : values) {
			if (nn(o)) {
				if (o instanceof Map o2) {
					o2.forEach((k, v) -> {
						K key = convertKey(k);
						V value = convertValue(v);
						add(key, value);
					});
				} else {
					throw rex("Object of type {0} could not be converted to type {1}", cn(o), "Map");
				}
			}
		}
		return this;
	}

	/**
	 * Adds a list of key/value pairs to this map.
	 *
	 * @param pairs The pairs to add.
	 * @return This object.
	 */
	@SuppressWarnings("unchecked")
	public Maps<K,V> addPairs(Object...pairs) {
		assertArgNotNull("pairs", pairs);
		if (pairs.length % 2 != 0)
			throw illegalArg("Odd number of parameters passed into Maps.addPairs(...)");
		for (var i = 0; i < pairs.length; i += 2)
			add((K)pairs[i], (V)pairs[i + 1]);
		return this;
	}

	/**
	 * Builds the map.
	 *
	 * <p>
	 * Applies filtering, sorting, ordering, concurrent, unmodifiable, and sparse options.
	 *
	 * <p>
	 * Map type selection:
	 * <ul>
	 * 	<li>If {@link #sorted()} is set: Uses {@link TreeMap} (or {@link java.util.concurrent.ConcurrentSkipListMap} if concurrent)
	 * 	<li>If {@link #ordered()} is set: Uses {@link LinkedHashMap} (or synchronized LinkedHashMap if concurrent)
	 * 	<li>Otherwise: Uses {@link HashMap} (or {@link java.util.concurrent.ConcurrentHashMap} if concurrent)
	 * </ul>
	 *
	 * <p>
	 * If filtering is applied, the result is wrapped in a {@link FilteredMap}.
	 *
	 * @return The built map, or {@code null} if {@link #sparse()} is set and the map is empty.
	 */
	public Map<K,V> build() {

		if (sparse && e(map))
			return null;

		var map2 = (Map<K,V>)null;

		if (ordered) {
			map2 = new LinkedHashMap<>();
			if (concurrent)
				map2 = Collections.synchronizedMap(map2);
		} else if (nn(comparator)) {
			map2 = concurrent ? new ConcurrentSkipListMap<>(comparator) : new TreeMap<>(comparator);
		} else {
			map2 = concurrent ? new ConcurrentHashMap<>() : new HashMap<>();
		}

		if (nn(filter) || nn(keyFunction) || nn(valueFunction)) {
			var map3b = FilteredMap.create(keyType, valueType);
			if (nn(filter))
				map3b.filter(filter);
			if (nn(keyFunction))
				map3b.keyFunction(keyFunction);
			if (nn(valueFunction))
				map3b.valueFunction(valueFunction);
			map2 = map3b.inner(map2).build();
		}

		if (nn(map))
			map2.putAll(map);

		if (unmodifiable)
			map2 = Collections.unmodifiableMap(map2);

		return map2;
	}

	/**
	 * Builds the map and wraps it in a {@link FluentMap}.
	 *
	 * <p>
	 * This is a convenience method that calls {@link #build()} and wraps the result in a {@link FluentMap}.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jk>import static</jk> org.apache.juneau.commons.utils.CollectionUtils.*;
	 *
	 * 	FluentMap&lt;String,Integer&gt; <jv>map</jv> = <jsm>mapb</jsm>(String.<jk>class</jk>, Integer.<jk>class</jk>)
	 * 		.add(<js>"one"</js>, 1)
	 * 		.add(<js>"two"</js>, 2)
	 * 		.buildFluent();
	 * </p>
	 *
	 * @return The built map wrapped in a {@link FluentMap}, or {@code null} if {@link #sparse()} is set and the map is empty.
	 */
	public FluentMap<K,V> buildFluent() {
		Map<K,V> result = build();
		return result == null ? null : new FluentMap<>(result);
	}

	/**
	 * Builds the map as a {@link FilteredMap}.
	 *
	 * <p>
	 * Map type selection:
	 * <ul>
	 * 	<li>If {@link #sorted()} is set: Uses {@link TreeMap} (or {@link java.util.concurrent.ConcurrentSkipListMap} if concurrent)
	 * 	<li>If {@link #ordered()} is set: Uses {@link LinkedHashMap} (or synchronized LinkedHashMap if concurrent)
	 * 	<li>Otherwise: Uses {@link HashMap} (or {@link java.util.concurrent.ConcurrentHashMap} if concurrent)
	 * </ul>
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jk>import static</jk> org.apache.juneau.commons.utils.CollectionUtils.*;
	 *
	 * 	FilteredMap&lt;String,Integer&gt; <jv>map</jv> = <jsm>mapb</jsm>(String.<jk>class</jk>, Integer.<jk>class</jk>)
	 * 		.filtered((k, v) -&gt; v != <jk>null</jk> &amp;&amp; v &gt; 0)
	 * 		.add(<js>"a"</js>, 5)
	 * 		.add(<js>"b"</js>, -1)  <jc>// Will be filtered out</jc>
	 * 		.buildFiltered();
	 * </p>
	 *
	 * <p>
	 * Note: If {@link #unmodifiable()} is set, the returned map will be wrapped in an unmodifiable view,
	 * which may cause issues if the FilteredMap tries to modify it internally. It's recommended to avoid
	 * using {@link #unmodifiable()} when calling this method.
	 *
	 * @return The built map as a {@link FilteredMap}, or {@code null} if {@link #sparse()} is set and the map is empty.
	 */
	public FilteredMap<K,V> buildFiltered() {
		var m = build();
		if (m == null)  // sparse mode and empty
			return null;
		if (m instanceof FilteredMap<K,V> m2)
			return m2;
		// Note that if unmodifiable is true, 'm' will be unmodifiable and will cause an error if you try
		// to insert a value from within FilteredMap.
		return FilteredMap.create(keyType, valueType).inner(m).build();
	}

	/**
	 * Sets the key conversion function for converting keys in {@link #addAny(Object...)}.
	 *
	 * <p>
	 * The function is applied to each key when adding entries from maps in {@link #addAny(Object...)}.
	 *
	 * @param keyFunction The function to convert keys. Must not be <jk>null</jk>.
	 * @return This object.
	 */
	public Maps<K,V> keyFunction(Function<Object,K> keyFunction) {
		this.keyFunction = assertArgNotNull("keyFunction", keyFunction);
		return this;
	}

	/**
	 * Sets the value conversion function for converting values in {@link #addAny(Object...)}.
	 *
	 * <p>
	 * The function is applied to each value when adding entries from maps in {@link #addAny(Object...)}.
	 *
	 * @param valueFunction The function to convert values. Must not be <jk>null</jk>.
	 * @return This object.
	 */
	public Maps<K,V> valueFunction(Function<Object,V> valueFunction) {
		this.valueFunction = assertArgNotNull("valueFunction", valueFunction);
		return this;
	}

	/**
	 * Sets both key and value conversion functions.
	 *
	 * <p>
	 * Convenience method for setting both functions at once.
	 *
	 * @param keyFunction The function to convert keys. Must not be <jk>null</jk>.
	 * @param valueFunction The function to convert values. Must not be <jk>null</jk>.
	 * @return This object.
	 */
	public Maps<K,V> functions(Function<Object,K> keyFunction, Function<Object,V> valueFunction) {
		this.keyFunction = assertArgNotNull("keyFunction", keyFunction);
		this.valueFunction = assertArgNotNull("valueFunction", valueFunction);
		return this;
	}

	/**
	 * Applies a default filter that excludes common "empty" or "unset" values from being added to the map.
	 *
	 * <p>
	 * The following values are filtered out:
	 * <ul>
	 * 	<li>{@code null}
	 * 	<li>{@link Boolean#FALSE}
	 * 	<li>Numbers with {@code intValue() == -1}
	 * 	<li>Empty arrays
	 * 	<li>Empty {@link Map Maps}
	 * 	<li>Empty {@link Collection Collections}
	 * </ul>
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jk>import static</jk> org.apache.juneau.commons.utils.CollectionUtils.*;
	 *
	 * 	FluentMap&lt;String,Object&gt; <jv>map</jv> = <jsm>mapb</jsm>(String.<jk>class</jk>, Object.<jk>class</jk>)
	 * 		.filtered()
	 * 		.add(<js>"name"</js>, <js>"John"</js>)
	 * 		.add(<js>"age"</js>, -1)              <jc>// Filtered out at build time</jc>
	 * 		.add(<js>"enabled"</js>, <jk>false</jk>)   <jc>// Filtered out at build time</jc>
	 * 		.add(<js>"tags"</js>, <jk>new</jk> String[0]) <jc>// Filtered out at build time</jc>
	 * 		.build();
	 * </p>
	 *
	 * @return This object.
	 */
	public Maps<K,V> filtered() {
		// @formatter:off
		return filtered((k, v) -> ! (
			v == null
			|| (v instanceof Boolean v2 && v2.equals(false))
			|| (v instanceof Number v3 && v3.intValue() == -1)
			|| (isArray(v) && Array.getLength(v) == 0)
			|| (v instanceof Map v2 && v2.isEmpty())
			|| (v instanceof Collection v3 && v3.isEmpty())
			));
		// @formatter:on
	}

	/**
	 * Applies a filter predicate to entries when building the map.
	 *
	 * <p>
	 * The filter receives both the key and value of each entry. Entries where the predicate returns
	 * {@code true} will be kept; entries where it returns {@code false} will be filtered out.
	 *
	 * <p>
	 * This method can be called multiple times. When called multiple times, all filters are combined
	 * using AND logic - an entry must pass all filters to be kept in the map.
	 *
	 * <p>
	 * Note: Filtering is applied at build time, not when adding entries.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jk>import static</jk> org.apache.juneau.commons.utils.CollectionUtils.*;
	 *
	 * 	<jc>// Keep only non-null, non-empty string values</jc>
	 * 	Map&lt;String,String&gt; <jv>map</jv> = <jsm>mapb</jsm>(String.<jk>class</jk>, String.<jk>class</jk>)
	 * 		.filtered((k, v) -&gt; v != <jk>null</jk> &amp;&amp; !v.equals(<js>""</js>))
	 * 		.add(<js>"a"</js>, <js>"foo"</js>)
	 * 		.add(<js>"b"</js>, <jk>null</jk>)     <jc>// Filtered out at build time</jc>
	 * 		.add(<js>"c"</js>, <js>""</js>)       <jc>// Filtered out at build time</jc>
	 * 		.build();
	 *
	 * 	<jc>// Multiple filters combined with AND</jc>
	 * 	Map&lt;String,Integer&gt; <jv>map2</jv> = <jsm>mapb</jsm>(String.<jk>class</jk>, Integer.<jk>class</jk>)
	 * 		.filtered((k, v) -&gt; v != <jk>null</jk>)           <jc>// First filter</jc>
	 * 		.filtered((k, v) -&gt; v &gt; 0)                    <jc>// Second filter (ANDed with first)</jc>
	 * 		.filtered((k, v) -&gt; ! k.startsWith(<js>"_"</js>)) <jc>// Third filter (ANDed with previous)</jc>
	 * 		.add(<js>"a"</js>, 5)
	 * 		.add(<js>"_b"</js>, 10)  <jc>// Filtered out (starts with "_")</jc>
	 * 		.add(<js>"c"</js>, -1)   <jc>// Filtered out (not &gt; 0)</jc>
	 * 		.build();
	 * </p>
	 *
	 * @param filter The filter predicate. Must not be <jk>null</jk>.
	 * @return This object.
	 */
	public Maps<K,V> filtered(BiPredicate<K,V> filter) {
		BiPredicate<K,V> newFilter = assertArgNotNull("filter", filter);
		if (this.filter == null)
			this.filter = newFilter;
		else
			this.filter = this.filter.and(newFilter);
		return this;
	}

	/**
	 * Converts the set into a {@link SortedMap}.
	 *
	 * <p>
	 * Note: If {@link #ordered()} was previously called, calling this method will override it.
	 * The last method called ({@link #ordered()} or {@link #sorted()}) determines the final map type.
	 *
	 * @return This object.
	 */
	@SuppressWarnings("unchecked")
	public Maps<K,V> sorted() {
		return sorted((Comparator<K>)Comparator.naturalOrder());
	}

	/**
	 * Converts the set into a {@link SortedMap} using the specified comparator.
	 *
	 * <p>
	 * Note: If {@link #ordered()} was previously called, calling this method will override it.
	 * The last method called ({@link #ordered()} or {@link #sorted()}) determines the final map type.
	 *
	 * @param comparator The comparator to use for sorting. Must not be <jk>null</jk>.
	 * @return This object.
	 */
	public Maps<K,V> sorted(Comparator<K> comparator) {
		this.comparator = assertArgNotNull("comparator", comparator);
		ordered = false;
		return this;
	}

	/**
	 * When specified, the {@link #build()} method will return <jk>null</jk> if the map is empty.
	 *
	 * <p>
	 * Otherwise {@link #build()} will never return <jk>null</jk>.
	 *
	 * @return This object.
	 */
	public Maps<K,V> sparse() {
		sparse = true;
		return this;
	}

	/**
	 * When specified, {@link #build()} will return an unmodifiable map.
	 *
	 * @return This object.
	 */
	public Maps<K,V> unmodifiable() {
		unmodifiable = true;
		return this;
	}

	/**
	 * When specified, {@link #build()} will return a thread-safe map.
	 *
	 * <p>
	 * The thread-safety implementation depends on other settings:
	 * <ul>
	 * 	<li>If {@link #sorted()} is set: Uses {@link java.util.concurrent.ConcurrentSkipListMap}
	 * 	<li>If {@link #ordered()} is set: Uses {@link Collections#synchronizedMap(LinkedHashMap)}
	 * 	<li>Otherwise: Uses {@link java.util.concurrent.ConcurrentHashMap}
	 * </ul>
	 *
	 * <p>
	 * This is useful when the map needs to be accessed from multiple threads.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jk>import static</jk> org.apache.juneau.commons.utils.CollectionUtils.*;
	 *
	 * 	<jc>// Create a thread-safe map using ConcurrentHashMap</jc>
	 * 	Map&lt;String,Integer&gt; <jv>map</jv> = <jsm>mapb</jsm>(String.<jk>class</jk>, Integer.<jk>class</jk>)
	 * 		.add(<js>"one"</js>, 1)
	 * 		.add(<js>"two"</js>, 2)
	 * 		.concurrent()
	 * 		.build();
	 *
	 * 	<jc>// Create a thread-safe ordered map</jc>
	 * 	Map&lt;String,Integer&gt; <jv>map2</jv> = <jsm>mapb</jsm>(String.<jk>class</jk>, Integer.<jk>class</jk>)
	 * 		.ordered()
	 * 		.concurrent()
	 * 		.add(<js>"one"</js>, 1)
	 * 		.build();
	 * </p>
	 *
	 * @return This object.
	 */
	public Maps<K,V> concurrent() {
		concurrent = true;
		return this;
	}

	/**
	 * Sets whether {@link #build()} should return a thread-safe map.
	 *
	 * <p>
	 * The thread-safety implementation depends on other settings:
	 * <ul>
	 * 	<li>If {@link #sorted()} is set: Uses {@link java.util.concurrent.ConcurrentSkipListMap}
	 * 	<li>If {@link #ordered()} is set: Uses {@link Collections#synchronizedMap(LinkedHashMap)}
	 * 	<li>Otherwise: Uses {@link java.util.concurrent.ConcurrentHashMap}
	 * </ul>
	 *
	 * <p>
	 * This is useful when the map needs to be accessed from multiple threads.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jk>import static</jk> org.apache.juneau.commons.utils.CollectionUtils.*;
	 *
	 * 	<jc>// Conditionally create a thread-safe map</jc>
	 * 	Map&lt;String,Integer&gt; <jv>map</jv> = <jsm>mapb</jsm>(String.<jk>class</jk>, Integer.<jk>class</jk>)
	 * 		.add(<js>"one"</js>, 1)
	 * 		.concurrent(<jv>needsThreadSafety</jv>)
	 * 		.build();
	 * </p>
	 *
	 * @param value Whether to make the map thread-safe.
	 * @return This object.
	 */
	public Maps<K,V> concurrent(boolean value) {
		concurrent = value;
		return this;
	}

	/**
	 * When specified, {@link #build()} will use a {@link LinkedHashMap} to preserve insertion order.
	 *
	 * <p>
	 * If not specified, a {@link HashMap} is used by default (no guaranteed order).
	 *
	 * <p>
	 * Note: If {@link #sorted()} was previously called, calling this method will override it.
	 * The last method called ({@link #ordered()} or {@link #sorted()}) determines the final map type.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jk>import static</jk> org.apache.juneau.commons.utils.CollectionUtils.*;
	 *
	 * 	<jc>// Create an ordered map (preserves insertion order)</jc>
	 * 	Map&lt;String,Integer&gt; <jv>map</jv> = <jsm>mapb</jsm>(String.<jk>class</jk>, Integer.<jk>class</jk>)
	 * 		.ordered()
	 * 		.add(<js>"one"</js>, 1)
	 * 		.add(<js>"two"</js>, 2)
	 * 		.build();
	 * </p>
	 *
	 * @return This object.
	 */
	public Maps<K,V> ordered() {
		return ordered(true);
	}

	/**
	 * Sets whether {@link #build()} should use a {@link LinkedHashMap} to preserve insertion order.
	 *
	 * <p>
	 * If <c>false</c> (default), a {@link HashMap} is used (no guaranteed order).
	 * If <c>true</c>, a {@link LinkedHashMap} is used (preserves insertion order).
	 *
	 * <p>
	 * Note: If {@link #sorted()} was previously called, calling this method with <c>true</c> will override it.
	 * The last method called ({@link #ordered()} or {@link #sorted()}) determines the final map type.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jk>import static</jk> org.apache.juneau.commons.utils.CollectionUtils.*;
	 *
	 * 	<jc>// Conditionally create an ordered map</jc>
	 * 	Map&lt;String,Integer&gt; <jv>map</jv> = <jsm>mapb</jsm>(String.<jk>class</jk>, Integer.<jk>class</jk>)
	 * 		.ordered(<jv>preserveOrder</jv>)
	 * 		.add(<js>"one"</js>, 1)
	 * 		.build();
	 * </p>
	 *
	 * @param value Whether to preserve insertion order.
	 * @return This object.
	 */
	public Maps<K,V> ordered(boolean value) {
		ordered = value;
		if (ordered)
			comparator = null;
		return this;
	}

	/**
	 * Converts a key object to the key type.
	 *
	 * @param o The object to convert.
	 * @return The converted key.
	 */
	@SuppressWarnings("unchecked")
	private K convertKey(Object o) {
		if (keyType.isInstance(o))
			return (K)o;
		if (nn(keyFunction))
			return keyFunction.apply(o);
		throw rex("Object of type {0} could not be converted to key type {1}", cn(o), cn(keyType));
	}

	/**
	 * Converts a value object to the value type.
	 *
	 * @param o The object to convert.
	 * @return The converted value.
	 */
	@SuppressWarnings("unchecked")
	private V convertValue(Object o) {
		if (valueType.isInstance(o))
			return (V)o;
		if (nn(valueFunction))
			return valueFunction.apply(o);
		throw rex("Object of type {0} could not be converted to value type {1}", cn(o), cn(valueType));
	}
}