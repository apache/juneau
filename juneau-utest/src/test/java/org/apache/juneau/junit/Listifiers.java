// ***************************************************************************************************************************
// * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file *
// * distributed with this work for additional information regarding copyright ownership.  The ASF licenses this file        *
// * to you under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance            *
// * with the License.  You may obtain a copy of the License at                                                              *
// *                                                                                                                         *
// *  http://www.apache.org/licenses/LICENSE-2.0                                                                             *
// *                                                                                                                         *
// * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an  *
// * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the License for the        *
// * specific language governing permissions and limitations under the License.                                              *
// ***************************************************************************************************************************
package org.apache.juneau.junit;

import static java.util.Collections.*;
import static java.util.stream.StreamSupport.*;
import static java.util.Spliterators.*;

import java.util.*;
import java.util.stream.*;

/**
 * Collection of standard listifier implementations for the Bean-Centric Testing framework.
 *
 * <p>This class provides built-in list conversion strategies that handle common Java collection
 * and iterable types. These listifiers are automatically registered when using
 * {@link BasicBeanConverter.Builder#defaultSettings()}.</p>
 *
 * <h5 class='section'>Purpose:</h5>
 * <p>Listifiers convert various collection-like objects to {@link List} instances for uniform
 * processing in BCT assertions. This enables consistent iteration and element access across
 * different data structures like arrays, iterators, streams, and custom collections.</p>
 *
 * <h5 class='section'>Built-in Listifiers:</h5>
 * <ul>
 *    <li><b>{@link #collectionListifier()}</b> - Converts {@link Collection} objects to {@link ArrayList}</li>
 *    <li><b>{@link #iterableListifier()}</b> - Converts {@link Iterable} objects using streams</li>
 *    <li><b>{@link #iteratorListifier()}</b> - Converts {@link Iterator} objects to lists (consumes iterator)</li>
 *    <li><b>{@link #enumerationListifier()}</b> - Converts {@link Enumeration} objects to lists</li>
 *    <li><b>{@link #streamListifier()}</b> - Converts {@link Stream} objects to lists (terminates stream)</li>
 *    <li><b>{@link #mapListifier()}</b> - Converts {@link Map} to list of {@link Map.Entry} objects</li>
 * </ul>
 *
 * <h5 class='section'>Usage Example:</h5>
 * <p class='bjava'>
 *    <jc>// Register listifiers using builder</jc>
 *    <jk>var</jk> <jv>converter</jv> = BasicBeanConverter.<jsm>builder</jsm>()
 *       .defaultSettings()
 *       .addListifier(Collection.<jk>class</jk>, Listifiers.<jsm>collectionListifier</jsm>())
 *       .addListifier(Stream.<jk>class</jk>, Listifiers.<jsm>streamListifier</jsm>())
 *       .build();
 * </p>
 *
 * <h5 class='section'>Iterator Consumption:</h5>
 * <p><b>Warning:</b> Some listifiers consume their input objects during conversion:</p>
 * <ul>
 *    <li><b>{@link Iterator}:</b> Elements are consumed and iterator becomes exhausted</li>
 *    <li><b>{@link Enumeration}:</b> Elements are consumed and enumeration becomes exhausted</li>
 *    <li><b>{@link Stream}:</b> Stream is terminated and cannot be reused</li>
 * </ul>
 *
 * <h5 class='section'>Custom Listifier Development:</h5>
 * <p>When creating custom listifiers, follow these patterns:</p>
 * <ul>
 *    <li><b>Null Safety:</b> Handle <jk>null</jk> inputs gracefully</li>
 *    <li><b>Immutability:</b> Return new lists rather than modifying inputs</li>
 *    <li><b>Type Safety:</b> Ensure proper generic type handling</li>
 *    <li><b>Performance:</b> Consider memory usage for large collections</li>
 * </ul>
 *
 * @see Listifier
 * @see BasicBeanConverter.Builder#addListifier(Class, Listifier)
 * @see BasicBeanConverter.Builder#defaultSettings()
 */
@SuppressWarnings("rawtypes")
public class Listifiers {

	/**
	 * Constructor.
	 */
	private Listifiers() {}

	/**
	 * Returns a listifier for {@link Collection} objects that converts them to {@link ArrayList}.
	 *
	 * <p>This listifier creates a new ArrayList containing all elements from the source collection.
	 * It works with any Collection subtype including List, Set, Queue, and Deque.</p>
	 *
	 * <h5 class='section'>Behavior:</h5>
	 * <ul>
	 *    <li><b>Non-empty collections:</b> Returns new ArrayList with all elements in iteration order</li>
	 *    <li><b>Empty collections:</b> Returns new empty ArrayList</li>
	 *    <li><b>Preserves order:</b> Maintains the iteration order of the source collection</li>
	 * </ul>
	 *
	 * <h5 class='section'>Usage Examples:</h5>
	 * <p class='bjava'>
	 *    <jc>// Test with different collection types</jc>
	 *    <jk>var</jk> <jv>list</jv> = List.<jsm>of</jsm>(<js>"a"</js>, <js>"b"</js>, <js>"c"</js>);
	 *    <jsm>assertList</jsm>(<jv>list</jv>, <js>"a"</js>, <js>"b"</js>, <js>"c"</js>);
	 *
	 *    <jk>var</jk> <jv>set</jv> = Set.<jsm>of</jsm>(<js>"x"</js>, <js>"y"</js>);
	 *    <jsm>assertList</jsm>(<jv>set</jv>, <js>"x"</js>, <js>"y"</js>); <jc>// Order may vary</jc>
	 *
	 *    <jk>var</jk> <jv>queue</jv> = <jk>new</jk> LinkedList&lt;&gt;(Arrays.<jsm>asList</jsm>(<js>"first"</js>, <js>"second"</js>));
	 *    <jsm>assertList</jsm>(<jv>queue</jv>, <js>"first"</js>, <js>"second"</js>);
	 * </p>
	 *
	 * <h5 class='section'>Performance:</h5>
	 * <p>This listifier creates a new ArrayList and copies all elements, so it has O(n) time
	 * and space complexity. For large collections, consider the memory implications.</p>
	 *
	 * @return A {@link Listifier} for {@link Collection} objects
	 * @see Collection
	 * @see ArrayList
	 */
	public static Listifier<Collection> collectionListifier() {
		return (bc, collection) -> new ArrayList<>(collection);
	}

	/**
	 * Returns a listifier for {@link Iterable} objects that converts them using streams.
	 *
	 * <p>This listifier handles any Iterable implementation by creating a stream from its
	 * spliterator and collecting elements to a list. It works with custom iterables and
	 * collection types not covered by more specific listifiers.</p>
	 *
	 * <h5 class='section'>Behavior:</h5>
	 * <ul>
	 *    <li><b>Standard iterables:</b> Converts elements to list maintaining iteration order</li>
	 *    <li><b>Custom iterables:</b> Works with any object implementing Iterable interface</li>
	 *    <li><b>Large iterables:</b> Processes all elements regardless of size</li>
	 * </ul>
	 *
	 * <h5 class='section'>Usage Examples:</h5>
	 * <p class='bjava'>
	 *    <jc>// Test with custom iterable</jc>
	 *    <jk>var</jk> <jv>range</jv> = IntStream.<jsm>range</jsm>(<jv>1</jv>, <jv>4</jv>).<jsm>boxed</jsm>().<jsm>collect</jsm>(toList());
	 *    <jsm>assertList</jsm>(<jv>range</jv>, <jv>1</jv>, <jv>2</jv>, <jv>3</jv>);
	 *
	 *    <jc>// Test with custom Iterable implementation</jc>
	 *    <jk>var</jk> <jv>fibonacci</jv> = <jk>new</jk> FibonacciIterable(<jv>5</jv>);
	 *    <jsm>assertList</jsm>(<jv>fibonacci</jv>, <jv>1</jv>, <jv>1</jv>, <jv>2</jv>, <jv>3</jv>, <jv>5</jv>);
	 * </p>
	 *
	 * @return A {@link Listifier} for {@link Iterable} objects
	 * @see Iterable
	 */
	public static Listifier<Iterable> iterableListifier() {
		return (bc, iterable) -> stream(iterable.spliterator(), false).toList();
	}

	/**
	 * Returns a listifier for {@link Iterator} objects that converts them to lists.
	 *
	 * <p><b>Warning:</b> This listifier consumes the iterator during conversion. After listification,
	 * the iterator will be exhausted and cannot be used again.</p>
	 *
	 * <h5 class='section'>Behavior:</h5>
	 * <ul>
	 *    <li><b>Element extraction:</b> Consumes all remaining elements from the iterator</li>
	 *    <li><b>Order preservation:</b> Maintains the iterator's order in the resulting list</li>
	 *    <li><b>Iterator exhaustion:</b> The iterator becomes unusable after conversion</li>
	 * </ul>
	 *
	 * <h5 class='section'>Usage Examples:</h5>
	 * <p class='bjava'>
	 *    <jc>// Test with list iterator</jc>
	 *    <jk>var</jk> <jv>list</jv> = List.<jsm>of</jsm>(<js>"a"</js>, <js>"b"</js>, <js>"c"</js>);
	 *    <jk>var</jk> <jv>iterator</jv> = <jv>list</jv>.iterator();
	 *    <jsm>assertList</jsm>(<jv>iterator</jv>, <js>"a"</js>, <js>"b"</js>, <js>"c"</js>);
	 *    <jc>// iterator is now exhausted</jc>
	 *
	 *    <jc>// Test with custom iterator</jc>
	 *    <jk>var</jk> <jv>numbers</jv> = IntStream.<jsm>range</jsm>(<jv>1</jv>, <jv>4</jv>).iterator();
	 *    <jsm>assertList</jsm>(<jv>numbers</jv>, <jv>1</jv>, <jv>2</jv>, <jv>3</jv>);
	 * </p>
	 *
	 * <h5 class='section'>Important Notes:</h5>
	 * <ul>
	 *    <li><b>One-time use:</b> The iterator becomes exhausted and unusable after conversion</li>
	 *    <li><b>Side effects:</b> Any side effects in the iterator's next() method will occur</li>
	 *    <li><b>Exception handling:</b> Exceptions from the iterator are propagated</li>
	 * </ul>
	 *
	 * @return A {@link Listifier} for {@link Iterator} objects
	 * @see Iterator
	 */
	public static Listifier<Iterator> iteratorListifier() {
		return (bc, iterator) -> stream(spliteratorUnknownSize(iterator, 0), false).toList();
	}

	/**
	 * Returns a listifier for {@link Enumeration} objects that converts them to lists.
	 *
	 * <p><b>Warning:</b> This listifier consumes the enumeration during conversion. After listification,
	 * the enumeration will be exhausted and cannot be used again.</p>
	 *
	 * <h5 class='section'>Behavior:</h5>
	 * <ul>
	 *    <li><b>Element extraction:</b> Consumes all remaining elements from the enumeration</li>
	 *    <li><b>Order preservation:</b> Maintains the enumeration's order in the resulting list</li>
	 *    <li><b>Enumeration exhaustion:</b> The enumeration becomes unusable after conversion</li>
	 * </ul>
	 *
	 * <h5 class='section'>Usage Examples:</h5>
	 * <p class='bjava'>
	 *    <jc>// Test with Vector enumeration</jc>
	 *    <jk>var</jk> <jv>vector</jv> = <jk>new</jk> Vector&lt;&gt;(List.<jsm>of</jsm>(<js>"x"</js>, <js>"y"</js>, <js>"z"</js>));
	 *    <jk>var</jk> <jv>enumeration</jv> = <jv>vector</jv>.elements();
	 *    <jsm>assertList</jsm>(<jv>enumeration</jv>, <js>"x"</js>, <js>"y"</js>, <js>"z"</js>);
	 *
	 *    <jc>// Test with Hashtable enumeration</jc>
	 *    <jk>var</jk> <jv>table</jv> = <jk>new</jk> Hashtable&lt;&gt;(Map.<jsm>of</jsm>(<js>"key1"</js>, <js>"value1"</js>));
	 *    <jk>var</jk> <jv>keys</jv> = <jv>table</jv>.keys();
	 *    <jsm>assertList</jsm>(<jv>keys</jv>, <js>"key1"</js>);
	 * </p>
	 *
	 * @return A {@link Listifier} for {@link Enumeration} objects
	 * @see Enumeration
	 */
	public static Listifier<Enumeration> enumerationListifier() {
		return (bc, enumeration) -> list(enumeration);
	}

	/**
	 * Returns a listifier for {@link Stream} objects that converts them to lists.
	 *
	 * <p><b>Warning:</b> This listifier terminates the stream during conversion. After listification,
	 * the stream is closed and cannot be used again.</p>
	 *
	 * <h5 class='section'>Behavior:</h5>
	 * <ul>
	 *    <li><b>Stream termination:</b> Calls toList() to collect all stream elements</li>
	 *    <li><b>Order preservation:</b> Maintains stream order in the resulting list</li>
	 *    <li><b>Stream closure:</b> The stream becomes unusable after conversion</li>
	 * </ul>
	 *
	 * <h5 class='section'>Usage Examples:</h5>
	 * <p class='bjava'>
	 *    <jc>// Test with filtered stream</jc>
	 *    <jk>var</jk> <jv>numbers</jv> = IntStream.<jsm>range</jsm>(<jv>1</jv>, <jv>10</jv>)
	 *       .filter(<jv>n</jv> -&gt; <jv>n</jv> % <jv>2</jv> == <jv>0</jv>)
	 *       .boxed();
	 *    <jsm>assertList</jsm>(<jv>numbers</jv>, <jv>2</jv>, <jv>4</jv>, <jv>6</jv>, <jv>8</jv>);
	 *
	 *    <jc>// Test with mapped stream</jc>
	 *    <jk>var</jk> <jv>words</jv> = Stream.<jsm>of</jsm>(<js>"hello"</js>, <js>"world"</js>)
	 *       .map(String::toUpperCase);
	 *    <jsm>assertList</jsm>(<jv>words</jv>, <js>"HELLO"</js>, <js>"WORLD"</js>);
	 * </p>
	 *
	 * <h5 class='section'>Important Notes:</h5>
	 * <ul>
	 *    <li><b>One-time use:</b> The stream is terminated and cannot be reused</li>
	 *    <li><b>Lazy evaluation:</b> Stream operations are executed during listification</li>
	 *    <li><b>Exception handling:</b> Stream operation exceptions are propagated</li>
	 * </ul>
	 *
	 * @return A {@link Listifier} for {@link Stream} objects
	 * @see Stream
	 */
	public static Listifier<Stream> streamListifier() {
		return (bc, stream) -> stream.toList();
	}

	/**
	 * Returns a listifier for {@link Map} objects that converts them to lists of {@link Map.Entry} objects.
	 *
	 * <p>This listifier enables maps to be processed as lists in BCT assertions, making it easy
	 * to test map contents using list-based assertion methods.</p>
	 *
	 * <h5 class='section'>Behavior:</h5>
	 * <ul>
	 *    <li><b>Entry conversion:</b> Each key-value pair becomes a Map.Entry in the list</li>
	 *    <li><b>Order preservation:</b> Maintains the map's iteration order</li>
	 *    <li><b>Empty maps:</b> Returns empty list for empty maps</li>
	 * </ul>
	 *
	 * <h5 class='section'>Usage Examples:</h5>
	 * <p class='bjava'>
	 *    <jc>// Test map contents</jc>
	 *    <jk>var</jk> <jv>map</jv> = Map.<jsm>of</jsm>(<js>"key1"</js>, <js>"value1"</js>, <js>"key2"</js>, <js>"value2"</js>);
	 *    <jsm>assertSize</jsm>(<jv>map</jv>, <jv>2</jv>);
	 *
	 *    <jc>// Test empty map</jc>
	 *    <jk>var</jk> <jv>emptyMap</jv> = Map.<jsm>of</jsm>();
	 *    <jsm>assertEmpty</jsm>(<jv>emptyMap</jv>);
	 *
	 *    <jc>// Test map in object property</jc>
	 *    <jk>var</jk> <jv>config</jv> = <jk>new</jk> Configuration().setProperties(<jv>map</jv>);
	 *    <jsm>assertSize</jsm>(<jv>config</jv>, <js>"properties"</js>, <jv>2</jv>);
	 * </p>
	 *
	 * <h5 class='section'>Entry Processing:</h5>
	 * <p>The resulting Map.Entry objects can be further processed by other parts of the
	 * conversion system, typically being stringified to <js>"key=value"</js> format.</p>
	 *
	 * @return A {@link Listifier} for {@link Map} objects
	 * @see Map
	 * @see Map.Entry
	 */
	public static Listifier<Map> mapListifier() {
		return (bc, map) -> new ArrayList<>(map.entrySet());
	}
}
