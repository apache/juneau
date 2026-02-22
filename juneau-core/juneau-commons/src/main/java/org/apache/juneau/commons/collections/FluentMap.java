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

import java.util.*;
import java.util.function.Predicate;

/**
 * A fluent wrapper around an arbitrary map that provides convenient methods for adding entries.
 *
 * <p>
 * This class wraps an underlying map and provides a fluent API for adding entries. All methods return
 * <c>this</c> to allow method chaining. The underlying map can be any {@link Map} implementation.
 *
 * <h5 class='section'>Features:</h5>
 * <ul class='spaced-list'>
 * 	<li><b>Fluent API:</b> All methods return <c>this</c> for method chaining
 * 	<li><b>Arbitrary Map Support:</b> Works with any map implementation
 * 	<li><b>Conditional Adding:</b> Add entries conditionally based on boolean expressions
 * 	<li><b>Transparent Interface:</b> Implements the full {@link Map} interface, so it can be used anywhere a map is expected
 * </ul>
 *
 * <h5 class='section'>Usage:</h5>
 * <p class='bjava'>
 * 	<jc>// Create a FluentMap wrapping a LinkedHashMap</jc>
 * 	FluentMap&lt;String, String&gt; <jv>map</jv> = <jk>new</jk> FluentMap&lt;&gt;(<jk>new</jk> LinkedHashMap&lt;&gt;());
 *
 * 	<jv>map</jv>
 * 		.a(<js>"key1"</js>, <js>"value1"</js>)
 * 		.a(<js>"key2"</js>, <js>"value2"</js>)
 * 		.ai(<jk>true</jk>, <js>"key3"</js>, <js>"value3"</js>)   <jc>// Added</jc>
 * 		.ai(<jk>false</jk>, <js>"key4"</js>, <js>"value4"</js>); <jc>// Not added</jc>
 *
 * 	<jc>// Add all entries from another map</jc>
 * 	Map&lt;String, String&gt; <jv>other</jv> = Map.of(<js>"key5"</js>, <js>"value5"</js>, <js>"key6"</js>, <js>"value6"</js>);
 * 	<jv>map</jv>.aa(<jv>other</jv>);
 * </p>
 *
 * <h5 class='section'>Example - Conditional Building:</h5>
 * <p class='bjava'>
 * 	<jk>boolean</jk> <jv>includeDebug</jv> = <jk>true</jk>;
 * 	<jk>boolean</jk> <jv>includeTest</jv> = <jk>false</jk>;
 *
 * 	FluentMap&lt;String, String&gt; <jv>config</jv> = <jk>new</jk> FluentMap&lt;&gt;(<jk>new</jk> LinkedHashMap&lt;&gt;())
 * 		.a(<js>"host"</js>, <js>"localhost"</js>)
 * 		.a(<js>"port"</js>, <js>"8080"</js>)
 * 		.ai(<jv>includeDebug</jv>, <js>"debug"</js>, <js>"true"</js>)   <jc>// Added</jc>
 * 		.ai(<jv>includeTest</jv>, <js>"test"</js>, <js>"true"</js>);    <jc>// Not added</jc>
 * </p>
 *
 * <h5 class='section'>Behavior Notes:</h5>
 * <ul class='spaced-list'>
 * 	<li>All map operations are delegated to the underlying map
 * 	<li>The fluent methods ({@link #a(Object, Object)}, {@link #aa(Map)}, {@link #ai(boolean, Object, Object)}) return <c>this</c> for chaining
 * 	<li>If a <jk>null</jk> map is passed to {@link #aa(Map)}, it is treated as a no-op
 * 	<li>The underlying map is stored by reference (not copied), so modifications affect the original map
 * </ul>
 *
 * <h5 class='section'>Thread Safety:</h5>
 * <p>
 * This class is not thread-safe unless the underlying map is thread-safe. If thread safety is required,
 * use a thread-safe map type (e.g., {@link java.util.concurrent.ConcurrentHashMap}).
 *
 * <h5 class='section'>See Also:</h5>
 * <ul>
 * 	<li class='link'><a class="doclink" href="../../../../../index.html#juneau-commons">Overview &gt; juneau-commons</a>
 * </ul>
 *
 * @param <K> The key type.
 * @param <V> The value type.
 */
@SuppressWarnings({
	"java:S115" // Constants use UPPER_snakeCase convention
})
public class FluentMap<K,V> extends AbstractMap<K,V> {

	// Argument name constants for assertArgNotNull
	private static final String ARG_inner = "inner";

	private final Map<K,V> map;

	/**
	 * Constructor.
	 *
	 * @param inner The underlying map to wrap. Must not be <jk>null</jk>.
	 */
	public FluentMap(Map<K,V> inner) {
		this.map = assertArgNotNull(ARG_inner, inner);
	}

	/**
	 * Adds a single key-value pair to this map.
	 *
	 * <p>
	 * This is a convenience method that calls {@link #put(Object, Object)} and returns <c>this</c>
	 * for method chaining.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	FluentMap&lt;String, String&gt; <jv>map</jv> = <jk>new</jk> FluentMap&lt;&gt;(<jk>new</jk> LinkedHashMap&lt;&gt;());
	 * 	<jv>map</jv>.a(<js>"key1"</js>, <js>"value1"</js>).a(<js>"key2"</js>, <js>"value2"</js>);
	 * </p>
	 *
	 * @param key The key to add.
	 * @param value The value to add.
	 * @return This object for method chaining.
	 */
	public FluentMap<K,V> a(K key, V value) {
		map.put(key, value);
		return this;
	}

	/**
	 * Adds all entries from the specified map to this map.
	 *
	 * <p>
	 * This is a convenience method that calls {@link #putAll(Map)} and returns <c>this</c>
	 * for method chaining. If the specified map is <jk>null</jk>, this is a no-op.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	FluentMap&lt;String, String&gt; <jv>map</jv> = <jk>new</jk> FluentMap&lt;&gt;(<jk>new</jk> LinkedHashMap&lt;&gt;());
	 * 	Map&lt;String, String&gt; <jv>other</jv> = Map.of(<js>"key1"</js>, <js>"value1"</js>, <js>"key2"</js>, <js>"value2"</js>);
	 * 	<jv>map</jv>.aa(<jv>other</jv>).a(<js>"key3"</js>, <js>"value3"</js>);
	 * </p>
	 *
	 * @param m The map whose entries are to be added. Can be <jk>null</jk> (no-op).
	 * @return This object for method chaining.
	 */
	public FluentMap<K,V> aa(Map<? extends K, ? extends V> m) {
		if (m != null)
			map.putAll(m);
		return this;
	}

	/**
	 * Adds a key-value pair to this map if the specified boolean condition is <jk>true</jk>.
	 *
	 * <p>
	 * This method is useful for conditionally adding entries based on runtime conditions.
	 * If the condition is <jk>false</jk>, the entry is not added and this method returns <c>this</c>
	 * without modifying the map.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jk>boolean</jk> <jv>includeDebug</jv> = <jk>true</jk>;
	 * 	<jk>boolean</jk> <jv>includeTest</jv> = <jk>false</jk>;
	 *
	 * 	FluentMap&lt;String, String&gt; <jv>map</jv> = <jk>new</jk> FluentMap&lt;&gt;(<jk>new</jk> LinkedHashMap&lt;&gt;())
	 * 		.a(<js>"host"</js>, <js>"localhost"</js>)
	 * 		.ai(<jv>includeDebug</jv>, <js>"debug"</js>, <js>"true"</js>)   <jc>// Added</jc>
	 * 		.ai(<jv>includeTest</jv>, <js>"test"</js>, <js>"true"</js>);    <jc>// Not added</jc>
	 * </p>
	 *
	 * @param condition The condition to evaluate. If <jk>true</jk>, the entry is added; if <jk>false</jk>, it is not.
	 * @param key The key to add if the condition is <jk>true</jk>.
	 * @param value The value to add if the condition is <jk>true</jk>.
	 * @return This object for method chaining.
	 */
	public FluentMap<K,V> ai(boolean condition, K key, V value) {
		if (condition)
			map.put(key, value);
		return this;
	}

	/**
	 * Adds a key-value pair to this map if the specified predicate returns <jk>true</jk> when applied to the value.
	 *
	 * <p>
	 * This method is useful for conditionally adding entries based on the value itself.
	 * The predicate is applied to the value, and if it returns <jk>true</jk>, the entry is added.
	 * If the predicate returns <jk>false</jk>, the entry is not added and this method returns <c>this</c>
	 * without modifying the map.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	FluentMap&lt;String, String&gt; <jv>map</jv> = <jk>new</jk> FluentMap&lt;&gt;(<jk>new</jk> LinkedHashMap&lt;&gt;())
	 * 		.a(<js>"host"</js>, <js>"localhost"</js>)
	 * 		.ai(s -&gt; !s.isEmpty(), <js>"debug"</js>, <js>"true"</js>)   <jc>// Added (value is not empty)</jc>
	 * 		.ai(s -&gt; !s.isEmpty(), <js>"test"</js>, <js>""</js>);       <jc>// Not added (value is empty)</jc>
	 * </p>
	 *
	 * @param predicate The predicate to test on the value. If it returns <jk>true</jk>, the entry is added; if <jk>false</jk>, it is not.
	 * @param key The key to add if the predicate returns <jk>true</jk>.
	 * @param value The value to add if the predicate returns <jk>true</jk>.
	 * @return This object for method chaining.
	 */
	public FluentMap<K,V> ai(Predicate<V> predicate, K key, V value) {
		if (predicate.test(value))
			map.put(key, value);
		return this;
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
	public V put(K key, V value) {
		return map.put(key, value);
	}

	@Override
	public V remove(Object key) {
		return map.remove(key);
	}

	@Override
	public void putAll(Map<? extends K, ? extends V> m) {
		map.putAll(m);
	}

	@Override
	public void clear() {
		map.clear();
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
	public Set<K> keySet() {
		return map.keySet();
	}

	@Override
	public Collection<V> values() {
		return map.values();
	}

	@Override
	public String toString() {
		return map.toString();
	}

	@Override
	public boolean equals(Object o) {
		return map.equals(o);
	}

	@Override
	public int hashCode() {
		return map.hashCode();
	}
}

