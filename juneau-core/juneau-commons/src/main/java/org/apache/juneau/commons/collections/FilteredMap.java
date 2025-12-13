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

import java.util.*;
import java.util.function.*;

/**
 * A map wrapper that filters entries based on a {@link BiPredicate} when they are added.
 *
 * <p>
 * This class wraps an underlying map and applies a filter to determine whether entries should be added.
 * Only entries that pass the filter (i.e., the predicate returns <jk>true</jk>) are actually stored in
 * the underlying map.
 *
 * <h5 class='section'>Features:</h5>
 * <ul class='spaced-list'>
 * 	<li><b>Flexible Filtering:</b> Use any {@link BiPredicate} to filter entries based on key, value, or both
 * 	<li><b>Custom Map Types:</b> Works with any map implementation via the builder's <c>creator</c> function
 * 	<li><b>Transparent Interface:</b> Implements the full {@link Map} interface, so it can be used anywhere a map is expected
 * 	<li><b>Filter on Add:</b> Filtering happens when entries are added via {@link #put(Object, Object)}, {@link #putAll(Map)}, etc.
 * </ul>
 *
 * <h5 class='section'>Usage:</h5>
 * <p class='bjava'>
 * 	<jc>// Create a filtered map that only accepts non-null values</jc>
 * 	FilteredMap&lt;String, String&gt; <jv>map</jv> = FilteredMap
 * 		.<jsm>create</jsm>(String.<jk>class</jk>, String.<jk>class</jk>)
 * 		.filter((k, v) -&gt; v != <jk>null</jk>)
 * 		.build();
 *
 * 	<jv>map</jv>.put(<js>"key1"</js>, <js>"value1"</js>);  <jc>// Added</jc>
 * 	<jv>map</jv>.put(<js>"key2"</js>, <jk>null</jk>);      <jc>// Filtered out</jc>
 * 	<jv>map</jv>.put(<js>"key3"</js>, <js>"value3"</js>);  <jc>// Added</jc>
 *
 * 	<jc>// map now contains: {"key1"="value1", "key3"="value3"}</jc>
 * </p>
 *
 * <h5 class='section'>Example - Custom Map Type:</h5>
 * <p class='bjava'>
 * 	<jc>// Create a filtered TreeMap that only accepts positive values</jc>
 * 	FilteredMap&lt;String, Integer&gt; <jv>map</jv> = FilteredMap
 * 		.<jsm>create</jsm>(String.<jk>class</jk>, Integer.<jk>class</jk>)
 * 		.filter((k, v) -&gt; v != <jk>null</jk> &amp;&amp; v &gt; 0)
 * 		.creator(() -&gt; <jk>new</jk> TreeMap&lt;&gt;())
 * 		.build();
 *
 * 	<jv>map</jv>.put(<js>"a"</js>, 5);   <jc>// Added</jc>
 * 	<jv>map</jv>.put(<js>"b"</js>, -1);  <jc>// Filtered out</jc>
 * 	<jv>map</jv>.put(<js>"c"</js>, 10);  <jc>// Added</jc>
 * </p>
 *
 * <h5 class='section'>Behavior Notes:</h5>
 * <ul class='spaced-list'>
 * 	<li>Filtering is applied to all entry addition methods: {@link #put(Object, Object)}, {@link #putAll(Map)}, etc.
 * 	<li>If the filter returns <jk>false</jk>, the entry is silently ignored (not added)
 * 	<li>When {@link #put(Object, Object)} filters out an entry, it returns the previous value associated with the key
 * 		(if any), or <jk>null</jk> if the key did not exist. This allows callers to distinguish between a new entry
 * 		being filtered out versus an existing entry being filtered out.
 * 	<li>The filter is not applied when reading from the map (e.g., {@link #get(Object)}, {@link #containsKey(Object)})
 * 	<li>All other map operations behave as expected on the underlying map
 * 	<li>The underlying map type is determined by the <c>creator</c> function (defaults to {@link LinkedHashMap})
 * </ul>
 *
 * <h5 class='section'>Thread Safety:</h5>
 * <p>
 * This class is not thread-safe unless the underlying map is thread-safe. If thread safety is required,
 * use a thread-safe map type (e.g., {@link java.util.concurrent.ConcurrentHashMap}) via the <c>creator</c> function.
 *
 * <h5 class='section'>See Also:</h5>
 * <ul>
 * 	<li class='link'><a class="doclink" href="../../../../../index.html#juneau-commons">Overview &gt; juneau-commons</a>
 * </ul>
 *
 * @param <K> The key type.
 * @param <V> The value type.
 */
public class FilteredMap<K,V> extends AbstractMap<K,V> {

	/**
	 * Builder for creating {@link FilteredMap} instances.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	FilteredMap&lt;String, Integer&gt; <jv>map</jv> = FilteredMap
	 * 		.<jsm>create</jsm>(String.<jk>class</jk>, Integer.<jk>class</jk>)
	 * 		.filter((k, v) -&gt; v != <jk>null</jk> &amp;&amp; v &gt; 0)
	 * 		.creator(() -&gt; <jk>new</jk> TreeMap&lt;&gt;())
	 * 		.build();
	 * </p>
	 *
	 * @param <K> The key type.
	 * @param <V> The value type.
	 */
	public static class Builder<K,V> {
		private BiPredicate<K,V> filter;
		private Supplier<Map<K,V>> creator = LinkedHashMap::new;
		private Class<K> keyType;
		private Class<V> valueType;
		private Function<Object,K> keyFunction;
		private Function<Object,V> valueFunction;

		/**
		 * Sets the filter predicate that determines whether entries should be added.
		 *
		 * <p>
		 * The predicate receives both the key and value of each entry. If it returns <jk>true</jk>,
		 * the entry is added to the map. If it returns <jk>false</jk>, the entry is silently ignored.
		 *
		 * <h5 class='section'>Example:</h5>
		 * <p class='bjava'>
		 * 	<jc>// Filter out null values</jc>
		 * 	Builder&lt;String, String&gt; <jv>b</jv> = FilteredMap.<jsm>create</jsm>(String.<jk>class</jk>, String.<jk>class</jk>)
		 * 		.filter((k, v) -&gt; v != <jk>null</jk>);
		 *
		 * 	<jc>// Filter based on both key and value</jc>
		 * 	Builder&lt;String, Integer&gt; <jv>b2</jv> = FilteredMap.<jsm>create</jsm>(String.<jk>class</jk>, Integer.<jk>class</jk>)
		 * 		.filter((k, v) -&gt; ! k.startsWith(<js>"_"</js>) &amp;&amp; v != <jk>null</jk> &amp;&amp; v &gt; 0);
		 * </p>
		 *
		 * @param value The filter predicate. Must not be <jk>null</jk>.
		 * @return This object for method chaining.
		 */
		public Builder<K,V> filter(BiPredicate<K,V> value) {
			filter = assertArgNotNull("value", value);
			return this;
		}

		/**
		 * Sets the supplier that creates the underlying map instance.
		 *
		 * <p>
		 * This supplier is called once during {@link #build()} to create the underlying map that will
		 * store the filtered entries. The default implementation creates a {@link LinkedHashMap}.
		 *
		 * <h5 class='section'>Example:</h5>
		 * <p class='bjava'>
		 * 	<jc>// Use a TreeMap for sorted keys</jc>
		 * 	Builder&lt;String, Integer&gt; <jv>b</jv> = FilteredMap.<jsm>create</jsm>(String.<jk>class</jk>, Integer.<jk>class</jk>)
		 * 		.creator(() -&gt; <jk>new</jk> TreeMap&lt;&gt;());
		 *
		 * 	<jc>// Use a ConcurrentHashMap for thread safety</jc>
		 * 	Builder&lt;String, Integer&gt; <jv>b2</jv> = FilteredMap.<jsm>create</jsm>(String.<jk>class</jk>, Integer.<jk>class</jk>)
		 * 		.creator(() -&gt; <jk>new</jk> ConcurrentHashMap&lt;&gt;());
		 * </p>
		 *
		 * @param value The creator supplier. Must not be <jk>null</jk>.
		 * @return This object for method chaining.
		 */
		public Builder<K,V> creator(Supplier<Map<K,V>> value) {
			creator = assertArgNotNull("value", value);
			return this;
		}

		/**
		 * Sets the function to use for converting keys when using {@link FilteredMap#add(Object, Object)}.
		 *
		 * <p>
		 * If specified, keys passed to {@link FilteredMap#add(Object, Object)} will be converted using
		 * this function before being added to the map. If not specified, keys must already be of the
		 * correct type (or <c>Object.class</c> is used if types were not specified, which accepts any type).
		 *
		 * <h5 class='section'>Example:</h5>
		 * <p class='bjava'>
		 * 	FilteredMap&lt;String, Integer&gt; <jv>map</jv> = FilteredMap
		 * 		.<jsm>create</jsm>(String.<jk>class</jk>, Integer.<jk>class</jk>)
		 * 		.filter((k, v) -&gt; v != <jk>null</jk>)
		 * 		.keyFunction(o -&gt; o.toString())
		 * 		.build();
		 *
		 * 	<jv>map</jv>.add(123, 5);  <jc>// Key will be converted from Integer to String</jc>
		 * </p>
		 *
		 * @param value The key conversion function. Can be <jk>null</jk>.
		 * @return This object for method chaining.
		 */
		public Builder<K,V> keyFunction(Function<Object,K> value) {
			keyFunction = value;
			return this;
		}

		/**
		 * Sets the function to use for converting values when using {@link FilteredMap#add(Object, Object)}.
		 *
		 * <p>
		 * If specified, values passed to {@link FilteredMap#add(Object, Object)} will be converted using
		 * this function before being added to the map. If not specified, values must already be of the
		 * correct type (or <c>Object.class</c> is used if types were not specified, which accepts any type).
		 *
		 * <h5 class='section'>Example:</h5>
		 * <p class='bjava'>
		 * 	FilteredMap&lt;String, Integer&gt; <jv>map</jv> = FilteredMap
		 * 		.<jsm>create</jsm>(String.<jk>class</jk>, Integer.<jk>class</jk>)
		 * 		.filter((k, v) -&gt; v != <jk>null</jk> &amp;&amp; v &gt; 0)
		 * 		.valueFunction(o -&gt; Integer.parseInt(o.toString()))
		 * 		.build();
		 *
		 * 	<jv>map</jv>.add(<js>"key"</js>, <js>"123"</js>);  <jc>// Value will be converted from String to Integer</jc>
		 * </p>
		 *
		 * @param value The value conversion function. Can be <jk>null</jk>.
		 * @return This object for method chaining.
		 */
		public Builder<K,V> valueFunction(Function<Object,V> value) {
			valueFunction = value;
			return this;
		}

		/**
		 * Sets both the key and value conversion functions at once.
		 *
		 * <p>
		 * This is a convenience method for setting both conversion functions in a single call.
		 *
		 * <h5 class='section'>Example:</h5>
		 * <p class='bjava'>
		 * 	FilteredMap&lt;String, Integer&gt; <jv>map</jv> = FilteredMap
		 * 		.<jsm>create</jsm>(String.<jk>class</jk>, Integer.<jk>class</jk>)
		 * 		.filter((k, v) -&gt; v != <jk>null</jk> &amp;&amp; v &gt; 0)
		 * 		.functions(
		 * 			o -&gt; o.toString(),                    <jc>// Key function</jc>
		 * 			o -&gt; Integer.parseInt(o.toString())   <jc>// Value function</jc>
		 * 		)
		 * 		.build();
		 * </p>
		 *
		 * @param keyFunction The key conversion function. Can be <jk>null</jk>.
		 * @param valueFunction The value conversion function. Can be <jk>null</jk>.
		 * @return This object for method chaining.
		 */
		public Builder<K,V> functions(Function<Object,K> keyFunction, Function<Object,V> valueFunction) {
			this.keyFunction = keyFunction;
			this.valueFunction = valueFunction;
			return this;
		}

		/**
		 * Builds a new {@link FilteredMap} instance.
		 *
		 * @return A new filtered map instance.
		 * @throws IllegalArgumentException If the filter has not been set.
		 */
		public FilteredMap<K,V> build() {
			assertArgNotNull("filter", filter);
			return new FilteredMap<>(filter, creator.get(), keyType, valueType, keyFunction, valueFunction);
		}
	}

	/**
	 * Creates a new builder for constructing a filtered map.
	 *
	 * @param <K> The key type.
	 * @param <V> The value type.
	 * @param keyType The key type class. Must not be <jk>null</jk>.
	 * @param valueType The value type class. Must not be <jk>null</jk>.
	 * @return A new builder.
	 */
	public static <K,V> Builder<K,V> create(Class<K> keyType, Class<V> valueType) {
		assertArgNotNull("keyType", keyType);
		assertArgNotNull("valueType", valueType);
		var builder = new Builder<K,V>();
		builder.keyType = keyType;
		builder.valueType = valueType;
		return builder;
	}

	/**
	 * Creates a new builder for constructing a filtered map with generic types.
	 *
	 * <p>
	 * This is a convenience method that creates a builder without requiring explicit type class parameters.
	 * The generic types must be explicitly specified using the diamond operator syntax.
	 *
	 * <p>
	 * When using this method, type checking is effectively disabled (using <c>Object.class</c> as the type),
	 * allowing any key and value types to be added. However, the generic type parameters should still be
	 * specified for type safety.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Explicitly specify generic types using diamond operator</jc>
	 * 	<jk>var</jk> <jv>map</jv> = FilteredMap
	 * 		.&lt;String, String&gt;<jsm>create</jsm>()
	 * 		.filter((<jv>k</jv>, <jv>v</jv>) -&gt; <jv>v</jv> != <jk>null</jk>)
	 * 		.build();
	 * </p>
	 *
	 * @param <K> The key type.
	 * @param <V> The value type.
	 * @return A new builder.
	 */
	@SuppressWarnings("unchecked")
	public static <K,V> Builder<K,V> create() {
		var builder = new Builder<K,V>();
		builder.keyType = (Class<K>)Object.class;
		builder.valueType = (Class<V>)Object.class;
		return builder;
	}
	private final BiPredicate<K,V> filter;
	private final Map<K,V> map;
	private final Class<K> keyType;
	private final Class<V> valueType;
	private final Function<Object,K> keyFunction;
	private final Function<Object,V> valueFunction;

	/**
	 * Constructor.
	 *
	 * @param filter The filter predicate. Must not be <jk>null</jk>.
	 * @param map The underlying map. Must not be <jk>null</jk>.
	 * @param keyType The key type. Must not be <jk>null</jk> (use <c>Object.class</c> to disable type checking).
	 * @param valueType The value type. Must not be <jk>null</jk> (use <c>Object.class</c> to disable type checking).
	 * @param keyFunction The key conversion function, or <jk>null</jk> if not specified.
	 * @param valueFunction The value conversion function, or <jk>null</jk> if not specified.
	 */
	protected FilteredMap(BiPredicate<K,V> filter, Map<K,V> map, Class<K> keyType, Class<V> valueType, Function<Object,K> keyFunction, Function<Object,V> valueFunction) {
		this.filter = assertArgNotNull("filter", filter);
		this.map = assertArgNotNull("map", map);
		this.keyType = assertArgNotNull("keyType", keyType);
		this.valueType = assertArgNotNull("valueType", valueType);
		this.keyFunction = keyFunction;
		this.valueFunction = valueFunction;
	}

	/**
	 * Associates the specified value with the specified key in this map, if the entry passes the filter.
	 *
	 * <p>
	 * If the entry passes the filter, it is added to the map and this method returns the previous value
	 * associated with the key (or <jk>null</jk> if there was no mapping).
	 *
	 * <p>
	 * If the entry is filtered out, it is not added to the map. This method returns the previous value
	 * associated with the key (or <jk>null</jk> if there was no mapping). This allows callers to distinguish
	 * between a new entry being filtered out versus an existing entry being filtered out.
	 *
	 * @param key The key with which the specified value is to be associated.
	 * @param value The value to be associated with the specified key.
	 * @return The previous value associated with <c>key</c>, or <jk>null</jk> if there was no mapping for <c>key</c>.
	 * 	This applies whether the entry was added or filtered out.
	 */
	@Override
	public V put(K key, V value) {
		if (filter.test(key, value))
			return map.put(key, value);
		return map.get(key);  // Return previous value if exists, null otherwise
	}

	@Override
	public void putAll(Map<? extends K, ? extends V> m) {
		for (var entry : m.entrySet()) {
			if (filter.test(entry.getKey(), entry.getValue()))
				map.put(entry.getKey(), entry.getValue());
		}
	}

	/**
	 * Tests whether the specified key-value pair would pass the filter and be added to this map.
	 *
	 * <p>
	 * This method can be used to check if an entry would be accepted by the filter without actually
	 * adding it to the map. This is useful for validation, debugging, or pre-checking entries before
	 * attempting to add them.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	FilteredMap&lt;String, Integer&gt; <jv>map</jv> = FilteredMap
	 * 		.<jsm>create</jsm>(String.<jk>class</jk>, Integer.<jk>class</jk>)
	 * 		.filter((k, v) -&gt; v != <jk>null</jk> &amp;&amp; v &gt; 0)
	 * 		.build();
	 *
	 * 	<jk>if</jk> (<jv>map</jv>.wouldAccept(<js>"key"</js>, 5)) {
	 * 		<jv>map</jv>.put(<js>"key"</js>, 5);  <jc>// Will be added</jc>
	 * 	}
	 * </p>
	 *
	 * @param key The key to test.
	 * @param value The value to test.
	 * @return <jk>true</jk> if the entry would be added to the map, <jk>false</jk> if it would be filtered out.
	 */
	public boolean wouldAccept(K key, V value) {
		return filter.test(key, value);
	}

	/**
	 * Returns the filter predicate used by this map.
	 *
	 * <p>
	 * This method provides access to the filter for debugging, inspection, or advanced use cases.
	 * The returned predicate is the same instance used internally by this map.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	FilteredMap&lt;String, Integer&gt; <jv>map</jv> = FilteredMap
	 * 		.<jsm>create</jsm>(String.<jk>class</jk>, Integer.<jk>class</jk>)
	 * 		.filter((k, v) -&gt; v != <jk>null</jk> &amp;&amp; v &gt; 0)
	 * 		.build();
	 *
	 * 	BiPredicate&lt;String, Integer&gt; <jv>filter</jv> = <jv>map</jv>.getFilter();
	 * 	<jc>// Use filter for other purposes</jc>
	 * </p>
	 *
	 * @return The filter predicate. Never <jk>null</jk>.
	 */
	public BiPredicate<K,V> getFilter() {
		return filter;
	}

	@Override
	public Set<Entry<K,V>> entrySet() {
		return map.entrySet();
	}

	@Override
	public V get(Object key) {
		return map.get(key);
	}

	@Override
	public V remove(Object key) {
		return map.remove(key);
	}

	@Override
	public boolean containsKey(Object key) {
		return map.containsKey(key);
	}

	@Override
	public boolean containsValue(Object value) {
		return map.containsValue(value);
	}

	@Override
	public int size() {
		return map.size();
	}

	@Override
	public boolean isEmpty() {
		return map.isEmpty();
	}

	@Override
	public void clear() {
		map.clear();
	}

	@Override
	public Set<K> keySet() {
		return map.keySet();
	}

	@Override
	public Collection<V> values() {
		return map.values();
	}

	/**
	 * Adds an entry to this map with automatic type conversion.
	 *
	 * <p>
	 * This method converts the key and value using the configured converters (if any) and validates
	 * the types (if key/value types were specified when creating the map). After conversion and validation,
	 * the entry is added using the standard {@link #put(Object, Object)} method, which applies the filter.
	 *
	 * <h5 class='section'>Type Conversion:</h5>
	 * <ul>
	 * 	<li>If a key function is configured, the key is converted by applying the function
	 * 	<li>If a value function is configured, the value is converted by applying the function
	 * 	<li>If no function is configured, the object is used as-is (but may be validated if types were specified)
	 * </ul>
	 *
	 * <h5 class='section'>Type Validation:</h5>
	 * <ul>
	 * 	<li>If key type was specified (via {@link #create(Class, Class)}), the converted key must be an instance of that type
	 * 	<li>If value type was specified (via {@link #create(Class, Class)}), the converted value must be an instance of that type
	 * 	<li>If {@link #create()} was used (no types specified), <c>Object.class</c> is used, which accepts any type
	 * </ul>
	 *
	 * <h5 class='section'>Return Value:</h5>
	 * <ul>
	 * 	<li>If the entry is added successfully, returns the previous value associated with the key (or <jk>null</jk> if there was no mapping)
	 * 	<li>If the entry is filtered out, returns <jk>null</jk> (even if there was a previous value)
	 * </ul>
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	FilteredMap&lt;String, Integer&gt; <jv>map</jv> = FilteredMap
	 * 		.<jsm>create</jsm>(String.<jk>class</jk>, Integer.<jk>class</jk>)
	 * 		.filter((k, v) -&gt; v != <jk>null</jk> &amp;&amp; v &gt; 0)
	 * 		.valueFunction(o -&gt; Integer.parseInt(o.toString()))
	 * 		.build();
	 *
	 * 	<jv>map</jv>.add(<js>"key"</js>, <js>"123"</js>);  <jc>// Value converted from String to Integer</jc>
	 * </p>
	 *
	 * @param key The key to add. Will be converted if a key function is configured.
	 * @param value The value to add. Will be converted if a value function is configured.
	 * @return The previous value associated with the key if the entry was added, or <jk>null</jk> if there was no mapping or the entry was filtered out.
	 * @throws RuntimeException If conversion fails or type validation fails.
	 */
	public V add(Object key, Object value) {
		K convertedKey = convertKey(key);
		V convertedValue = convertValue(value);
		if (filter.test(convertedKey, convertedValue))
			return put(convertedKey, convertedValue);
		return null;  // Filtered out, return null
	}

	/**
	 * Adds all entries from the specified map with automatic type conversion.
	 *
	 * <p>
	 * This method iterates over all entries in the source map and adds each one using {@link #add(Object, Object)},
	 * which applies conversion and validation before adding.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	FilteredMap&lt;String, Integer&gt; <jv>map</jv> = FilteredMap
	 * 		.<jsm>create</jsm>(String.<jk>class</jk>, Integer.<jk>class</jk>)
	 * 		.filter((k, v) -&gt; v != <jk>null</jk> &amp;&amp; v &gt; 0)
	 * 		.valueFunction(o -&gt; Integer.parseInt(o.toString()))
	 * 		.build();
	 *
	 * 	Map&lt;String, String&gt; <jv>source</jv> = Map.of(<js>"a"</js>, <js>"5"</js>, <js>"b"</js>, <js>"-1"</js>);
	 * 	<jv>map</jv>.addAll(<jv>source</jv>);  <jc>// Values converted, negative value filtered out</jc>
	 * </p>
	 *
	 * @param source The map containing entries to add. Can be <jk>null</jk> (no-op).
	 */
	public void addAll(Map<?,?> source) {
		if (source != null) {
			for (var entry : source.entrySet()) {
				add(entry.getKey(), entry.getValue());
			}
		}
	}

	private K convertKey(Object key) {
		if (keyFunction != null) {
			key = keyFunction.apply(key);
		}
		if (key == null) {
			// Allow null for non-primitive types
			if (keyType.isPrimitive())
				throw rex("Cannot set null key for primitive type {0}", keyType.getName());
			return null;
		}
		if (keyType.isInstance(key))
			return keyType.cast(key);
		throw rex("Object of type {0} could not be converted to key type {1}", cn(key), cn(keyType));
	}

	private V convertValue(Object value) {
		if (valueFunction != null) {
			value = valueFunction.apply(value);
		}
		if (value == null) {
			// Allow null for non-primitive types
			if (valueType.isPrimitive())
				throw rex("Cannot set null value for primitive type {0}", valueType.getName());
			return null;
		}
		if (valueType.isInstance(value))
			return valueType.cast(value);
		throw rex("Object of type {0} could not be converted to value type {1}", cn(value), cn(valueType));
	}
}

