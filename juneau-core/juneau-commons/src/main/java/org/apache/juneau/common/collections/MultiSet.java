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
package org.apache.juneau.common.collections;

import static org.apache.juneau.common.utils.AssertionUtils.*;

import java.util.*;

/**
 * A composite set that presents multiple collections as a single unified set.
 *
 * <p>
 * This class allows multiple collections to be viewed and iterated over as if they were merged into
 * a single set, without actually copying the elements. Modifications made through the iterator affect
 * the underlying collections.
 *
 * <h5 class='section'>Features:</h5>
 * <ul class='spaced-list'>
 * 	<li><b>Zero-Copy Composition:</b> No data is copied when creating a MultiSet; it simply wraps the provided collections
 * 	<li><b>Transparent Iteration:</b> Iterating over a MultiSet seamlessly traverses all underlying collections in order
 * 	<li><b>Modification Support:</b> Elements can be removed via the iterator's {@link Iterator#remove()} method
 * 	<li><b>Efficient Size Calculation:</b> The size is computed by summing the sizes of all underlying collections
 * 	<li><b>Enumeration Support:</b> Provides an {@link Enumeration} view via {@link #enumerator()}
 * </ul>
 *
 * <h5 class='section'>Usage:</h5>
 * <p class='bjava'>
 * 	<jc>// Create a MultiSet from three separate collections</jc>
 * 	List&lt;String&gt; <jv>list1</jv> = List.of(<js>"a"</js>, <js>"b"</js>);
 * 	Set&lt;String&gt; <jv>set1</jv> = Set.of(<js>"c"</js>, <js>"d"</js>);
 * 	List&lt;String&gt; <jv>list2</jv> = List.of(<js>"e"</js>, <js>"f"</js>);
 *
 * 	MultiSet&lt;String&gt; <jv>multiSet</jv> = <jk>new</jk> MultiSet&lt;&gt;(<jv>list1</jv>, <jv>set1</jv>, <jv>list2</jv>);
 *
 * 	<jc>// Iterate over all elements from all collections</jc>
 * 	<jk>for</jk> (String <jv>element</jv> : <jv>multiSet</jv>) {
 * 		System.<jsf>out</jsf>.println(<jv>element</jv>); <jc>// Prints: a, b, c, d, e, f</jc>
 * 	}
 *
 * 	<jc>// Get total size across all collections</jc>
 * 	<jk>int</jk> <jv>totalSize</jv> = <jv>multiSet</jv>.size(); <jc>// Returns: 6</jc>
 *
 * 	<jc>// Remove elements via iterator (affects underlying collections)</jc>
 * 	Iterator&lt;String&gt; <jv>it</jv> = <jv>multiSet</jv>.iterator();
 * 	<jk>while</jk> (<jv>it</jv>.hasNext()) {
 * 		<jk>if</jk> (<jv>it</jv>.next().equals(<js>"b"</js>)) {
 * 			<jv>it</jv>.remove(); <jc>// Removes "b" from list1</jc>
 * 		}
 * 	}
 * </p>
 *
 * <h5 class='section'>Behavior Notes:</h5>
 * <ul class='spaced-list'>
 * 	<li>The order of iteration follows the order of collections as provided in the constructor
 * 	<li>Within each collection, iteration order is determined by that collection's iterator
 * 	<li>The underlying collections must not be <jk>null</jk>, but can be empty
 * 	<li>Modifications via {@link Iterator#remove()} are delegated to the underlying collection's iterator
 * 	<li>This class does not support {@link #add(Object)} or {@link #remove(Object)} operations
 * 	<li>The {@link #size()} method recomputes the sum each time it's called (not cached)
 * </ul>
 *
 * <h5 class='section'>Thread Safety:</h5>
 * <p>
 * This class is not inherently thread-safe. If the underlying collections are modified concurrently
 * during iteration, the behavior is undefined. Synchronization must be handled externally if needed.
 *
 * <h5 class='section'>Example - Processing Multiple Data Sources:</h5>
 * <p class='bjava'>
 * 	<jc>// Combine results from database, cache, and defaults</jc>
 * 	List&lt;User&gt; <jv>dbUsers</jv> = fetchFromDatabase();
 * 	Set&lt;User&gt; <jv>cachedUsers</jv> = getCachedUsers();
 * 	List&lt;User&gt; <jv>defaultUsers</jv> = getDefaultUsers();
 *
 * 	MultiSet&lt;User&gt; <jv>allUsers</jv> = <jk>new</jk> MultiSet&lt;&gt;(<jv>dbUsers</jv>, <jv>cachedUsers</jv>, <jv>defaultUsers</jv>);
 *
 * 	<jc>// Process all users from all sources</jc>
 * 	<jv>allUsers</jv>.forEach(user -&gt; processUser(user));
 * </p>
 *
 * <h5 class='section'>See Also:</h5>
 * <ul>
 * 	<li class='link'><a class="doclink" href="../../../../../index.html#juneau-common">Overview &gt; juneau-common</a>
 * </ul>
 *
 * @param <E> The element type of this set.
 */
public class MultiSet<E> extends AbstractSet<E> {

	/**
	 * The underlying collections being wrapped by this MultiSet.
	 * <p>
	 * These collections are accessed directly during iteration without copying.
	 */
	final Collection<E>[] l;

	/**
	 * Creates a new MultiSet that presents the specified collections as a single unified set.
	 *
	 * <p>
	 * The collections are stored by reference (not copied), so modifications made through the
	 * MultiSet's iterator will affect the original collections.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	List&lt;String&gt; <jv>list1</jv> = <jk>new</jk> ArrayList&lt;&gt;(List.of(<js>"a"</js>, <js>"b"</js>));
	 * 	List&lt;String&gt; <jv>list2</jv> = <jk>new</jk> ArrayList&lt;&gt;(List.of(<js>"c"</js>, <js>"d"</js>));
	 *
	 * 	MultiSet&lt;String&gt; <jv>multiSet</jv> = <jk>new</jk> MultiSet&lt;&gt;(<jv>list1</jv>, <jv>list2</jv>);
	 * 	<jc>// multiSet now represents all elements from both lists</jc>
	 * </p>
	 *
	 * @param c Zero or more collections to combine into this set. Must not be <jk>null</jk>,
	 *           and no individual collection can be <jk>null</jk> (but collections can be empty).
	 * @throws IllegalArgumentException if the collections array or any collection within it is <jk>null</jk>.
	 */
	@SafeVarargs
	public MultiSet(Collection<E>...c) {
		assertArgNotNull("c", c);
		for (var cc : c)
			assertArgNotNull("c", cc);
		l = c;
	}

	/**
	 * Returns an {@link Enumeration} view of this set.
	 *
	 * <p>
	 * This is useful for compatibility with legacy APIs that require an {@link Enumeration}
	 * rather than an {@link Iterator}.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	MultiSet&lt;String&gt; <jv>multiSet</jv> = <jk>new</jk> MultiSet&lt;&gt;(list1, list2);
	 * 	Enumeration&lt;String&gt; <jv>enumeration</jv> = <jv>multiSet</jv>.enumerator();
	 *
	 * 	<jk>while</jk> (<jv>enumeration</jv>.hasMoreElements()) {
	 * 		String <jv>element</jv> = <jv>enumeration</jv>.nextElement();
	 * 		<jc>// Process element</jc>
	 * 	}
	 * </p>
	 *
	 * @return An {@link Enumeration} that iterates over all elements in all underlying collections.
	 * @see #iterator()
	 */
	public Enumeration<E> enumerator() {
		return Collections.enumeration(this);
	}

	/**
	 * Returns an iterator over all elements in all underlying collections.
	 *
	 * <p>
	 * The iterator traverses each collection in the order they were provided to the constructor.
	 * Within each collection, the iteration order is determined by that collection's iterator.
	 *
	 * <p>
	 * The returned iterator supports the {@link Iterator#remove()} operation, which removes
	 * the current element from its underlying collection.
	 *
	 * <h5 class='section'>Behavior:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>Elements from the first collection are iterated first, then the second, and so on
	 * 	<li>If a collection is empty, it is skipped during iteration
	 * 	<li>Calling {@link Iterator#remove()} removes the element from the underlying collection
	 * 	<li>Calling {@link Iterator#next()} when {@link Iterator#hasNext()} returns <jk>false</jk>
	 * 		throws {@link NoSuchElementException}
	 * 	<li>Calling {@link Iterator#remove()} before calling {@link Iterator#next()} or calling it twice
	 * 		in a row may throw {@link IllegalStateException} (behavior depends on underlying collection)
	 * </ul>
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	List&lt;String&gt; <jv>list1</jv> = <jk>new</jk> ArrayList&lt;&gt;(List.of(<js>"a"</js>, <js>"b"</js>));
	 * 	List&lt;String&gt; <jv>list2</jv> = <jk>new</jk> ArrayList&lt;&gt;(List.of(<js>"c"</js>, <js>"d"</js>));
	 * 	MultiSet&lt;String&gt; <jv>multiSet</jv> = <jk>new</jk> MultiSet&lt;&gt;(<jv>list1</jv>, <jv>list2</jv>);
	 *
	 * 	Iterator&lt;String&gt; <jv>it</jv> = <jv>multiSet</jv>.iterator();
	 * 	<jk>while</jk> (<jv>it</jv>.hasNext()) {
	 * 		String <jv>element</jv> = <jv>it</jv>.next();
	 * 		<jk>if</jk> (<jv>element</jv>.equals(<js>"b"</js>)) {
	 * 			<jv>it</jv>.remove(); <jc>// Removes "b" from list1</jc>
	 * 		}
	 * 	}
	 * </p>
	 *
	 * @return An iterator over all elements in all underlying collections.
	 */
	@Override /* Set */
	public Iterator<E> iterator() {
		return new Iterator<>() {
			int i = 0;
			Iterator<E> i2 = (l.length > 0 ? l[i++].iterator() : null);

			@Override /* Overridden from Iterator */
			public boolean hasNext() {
				if (i2 == null)
					return false;
				if (i2.hasNext())
					return true;
				for (var j = i; j < l.length; j++)
					if (l[j].size() > 0)
						return true;
				return false;
			}

			@Override /* Overridden from Iterator */
			public E next() {
				if (i2 == null)
					throw new NoSuchElementException();
				while (! i2.hasNext()) {
					if (i >= l.length)
						throw new NoSuchElementException();
					i2 = l[i++].iterator();
				}
				return i2.next();
			}

			@Override /* Overridden from Iterator */
			public void remove() {
				if (i2 == null)
					throw new NoSuchElementException();
				i2.remove();
			}
		};
	}

	/**
	 * Returns the total number of elements across all underlying collections.
	 *
	 * <p>
	 * This method computes the size by summing the {@link Collection#size()} of each
	 * underlying collection. The size is recalculated each time this method is called
	 * (it is not cached).
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	List&lt;String&gt; <jv>list1</jv> = List.of(<js>"a"</js>, <js>"b"</js>);        <jc>// size = 2</jc>
	 * 	List&lt;String&gt; <jv>list2</jv> = List.of(<js>"c"</js>, <js>"d"</js>, <js>"e"</js>); <jc>// size = 3</jc>
	 * 	MultiSet&lt;String&gt; <jv>multiSet</jv> = <jk>new</jk> MultiSet&lt;&gt;(<jv>list1</jv>, <jv>list2</jv>);
	 *
	 * 	<jk>int</jk> <jv>totalSize</jv> = <jv>multiSet</jv>.size(); <jc>// Returns: 5</jc>
	 * </p>
	 *
	 * @return The sum of sizes of all underlying collections.
	 */
	@Override /* Set */
	public int size() {
		var i = 0;
		for (var c : l)
			i += c.size();
		return i;
	}
}