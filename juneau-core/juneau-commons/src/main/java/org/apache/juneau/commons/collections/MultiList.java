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
import static org.apache.juneau.commons.utils.Utils.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * A composite list that presents multiple lists as a single unified list.
 *
 * <p>
 * This class allows multiple lists to be viewed and accessed as if they were merged into
 * a single list, without actually copying the elements. Modifications made through the iterator
 * or list iterator affect the underlying lists.
 *
 * <h5 class='section'>Features:</h5>
 * <ul class='spaced-list'>
 * 	<li><b>Zero-Copy Composition:</b> No data is copied when creating a MultiList; it simply wraps the provided lists
 * 	<li><b>Transparent Access:</b> Accessing elements by index or iterating over a MultiList seamlessly traverses all underlying lists in order
 * 	<li><b>Modification Support:</b> Elements can be removed via the iterator's {@link Iterator#remove()} method
 * 	<li><b>Efficient Size Calculation:</b> The size is computed by summing the sizes of all underlying lists
 * 	<li><b>Enumeration Support:</b> Provides an {@link Enumeration} view via {@link #enumerator()}
 * </ul>
 *
 * <h5 class='section'>Usage:</h5>
 * <p class='bjava'>
 * 	<jc>// Create a MultiList from three separate lists</jc>
 * 	List&lt;String&gt; <jv>list1</jv> = List.of(<js>"a"</js>, <js>"b"</js>);
 * 	List&lt;String&gt; <jv>list2</jv> = List.of(<js>"c"</js>, <js>"d"</js>);
 * 	List&lt;String&gt; <jv>list3</jv> = List.of(<js>"e"</js>, <js>"f"</js>);
 *
 * 	MultiList&lt;String&gt; <jv>multiList</jv> = <jk>new</jk> MultiList&lt;&gt;(<jv>list1</jv>, <jv>list2</jv>, <jv>list3</jv>);
 *
 * 	<jc>// Access elements by index</jc>
 * 	<jv>multiList</jv>.get(0);  <jc>// Returns "a"</jc>
 * 	<jv>multiList</jv>.get(3);  <jc>// Returns "d"</jc>
 * 	<jv>multiList</jv>.get(5);  <jc>// Returns "f"</jc>
 *
 * 	<jc>// Iterate over all elements from all lists</jc>
 * 	<jk>for</jk> (String <jv>element</jv> : <jv>multiList</jv>) {
 * 		System.<jsf>out</jsf>.println(<jv>element</jv>); <jc>// Prints: a, b, c, d, e, f</jc>
 * 	}
 *
 * 	<jc>// Get total size across all lists</jc>
 * 	<jk>int</jk> <jv>totalSize</jv> = <jv>multiList</jv>.size(); <jc>// Returns: 6</jc>
 *
 * 	<jc>// Remove elements via iterator (affects underlying lists)</jc>
 * 	Iterator&lt;String&gt; <jv>it</jv> = <jv>multiList</jv>.iterator();
 * 	<jk>while</jk> (<jv>it</jv>.hasNext()) {
 * 		<jk>if</jk> (<jv>it</jv>.next().equals(<js>"b"</js>)) {
 * 			<jv>it</jv>.remove(); <jc>// Removes "b" from list1</jc>
 * 		}
 * 	}
 * </p>
 *
 * <h5 class='section'>Behavior Notes:</h5>
 * <ul class='spaced-list'>
 * 	<li>The order of access follows the order of lists as provided in the constructor
 * 	<li>Within each list, access order follows the list's natural order
 * 	<li>The underlying lists must not be <jk>null</jk>, but can be empty
 * 	<li>Modifications via {@link Iterator#remove()} or {@link ListIterator#remove()} are delegated to the underlying list's iterator
 * 	<li>This class does not support {@link #add(Object)}, {@link #add(int, Object)}, {@link #set(int, Object)}, or {@link #remove(int)} operations
 * 	<li>The {@link #size()} method recomputes the sum each time it's called (not cached)
 * 	<li>The {@link #get(int)} method locates the element by traversing lists until the correct index is found
 * </ul>
 *
 * <h5 class='section'>Thread Safety:</h5>
 * <p>
 * This class is not inherently thread-safe. If the underlying lists are modified concurrently
 * during iteration or access, the behavior is undefined. Synchronization must be handled externally if needed.
 *
 * <h5 class='section'>Example - Processing Multiple Data Sources:</h5>
 * <p class='bjava'>
 * 	<jc>// Combine results from database, cache, and defaults</jc>
 * 	List&lt;User&gt; <jv>dbUsers</jv> = fetchFromDatabase();
 * 	List&lt;User&gt; <jv>cachedUsers</jv> = getCachedUsers();
 * 	List&lt;User&gt; <jv>defaultUsers</jv> = getDefaultUsers();
 *
 * 	MultiList&lt;User&gt; <jv>allUsers</jv> = <jk>new</jk> MultiList&lt;&gt;(<jv>dbUsers</jv>, <jv>cachedUsers</jv>, <jv>defaultUsers</jv>);
 *
 * 	<jc>// Process all users from all sources</jc>
 * 	<jv>allUsers</jv>.forEach(user -&gt; processUser(user));
 *
 * 	<jc>// Access specific user by index</jc>
 * 	User <jv>user</jv> = <jv>allUsers</jv>.get(<jv>10</jv>);
 * </p>
 *
 * <h5 class='section'>See Also:</h5>
 * <ul>
 * 	<li class='link'><a class="doclink" href="../../../../../index.html#juneau-commons">Overview &gt; juneau-commons</a>
 * </ul>
 *
 * @param <E> The element type of this list.
 */
public class MultiList<E> extends AbstractList<E> {

	/**
	 * The underlying lists being wrapped by this MultiList.
	 * <p>
	 * These lists are accessed directly during iteration and index access without copying.
	 */
	final List<E>[] l;

	/**
	 * Creates a new MultiList that presents the specified lists as a single unified list.
	 *
	 * <p>
	 * The lists are stored by reference (not copied), so modifications made through the
	 * MultiList's iterator will affect the original lists.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	List&lt;String&gt; <jv>list1</jv> = <jk>new</jk> ArrayList&lt;&gt;(List.of(<js>"a"</js>, <js>"b"</js>));
	 * 	List&lt;String&gt; <jv>list2</jv> = <jk>new</jk> ArrayList&lt;&gt;(List.of(<js>"c"</js>, <js>"d"</js>));
	 *
	 * 	MultiList&lt;String&gt; <jv>multiList</jv> = <jk>new</jk> MultiList&lt;&gt;(<jv>list1</jv>, <jv>list2</jv>);
	 * 	<jc>// multiList now represents all elements from both lists</jc>
	 * </p>
	 *
	 * @param c Zero or more lists to combine into this list. Must not be <jk>null</jk>,
	 *           and no individual list can be <jk>null</jk> (but lists can be empty).
	 * @throws IllegalArgumentException if the lists array or any list within it is <jk>null</jk>.
	 */
	@SafeVarargs
	public MultiList(List<E>...c) {
		assertArgNotNull("c", c);
		for (var cc : c)
			assertArgNotNull("c", cc);
		l = c;
	}

	/**
	 * Returns an {@link Enumeration} view of this list.
	 *
	 * <p>
	 * This is useful for compatibility with legacy APIs that require an {@link Enumeration}
	 * rather than an {@link Iterator}.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	MultiList&lt;String&gt; <jv>multiList</jv> = <jk>new</jk> MultiList&lt;&gt;(list1, list2);
	 * 	Enumeration&lt;String&gt; <jv>enumeration</jv> = <jv>multiList</jv>.enumerator();
	 *
	 * 	<jk>while</jk> (<jv>enumeration</jv>.hasMoreElements()) {
	 * 		String <jv>element</jv> = <jv>enumeration</jv>.nextElement();
	 * 		<jc>// Process element</jc>
	 * 	}
	 * </p>
	 *
	 * @return An {@link Enumeration} that iterates over all elements in all underlying lists.
	 * @see #iterator()
	 */
	public Enumeration<E> enumerator() {
		return Collections.enumeration(this);
	}

	/**
	 * Returns the element at the specified position in this list.
	 *
	 * <p>
	 * The index is resolved by traversing the underlying lists in order until the
	 * correct list and position within that list is found.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	List&lt;String&gt; <jv>list1</jv> = List.of(<js>"a"</js>, <js>"b"</js>);        <jc>// indices 0-1</jc>
	 * 	List&lt;String&gt; <jv>list2</jv> = List.of(<js>"c"</js>, <js>"d"</js>, <js>"e"</js>); <jc>// indices 2-4</jc>
	 * 	MultiList&lt;String&gt; <jv>multiList</jv> = <jk>new</jk> MultiList&lt;&gt;(<jv>list1</jv>, <jv>list2</jv>);
	 *
	 * 	<jv>multiList</jv>.get(0); <jc>// Returns "a"</jc>
	 * 	<jv>multiList</jv>.get(2); <jc>// Returns "c"</jc>
	 * 	<jv>multiList</jv>.get(4); <jc>// Returns "e"</jc>
	 * </p>
	 *
	 * @param index The index of the element to return.
	 * @return The element at the specified position.
	 * @throws IndexOutOfBoundsException if the index is out of range (index &lt; 0 || index &gt;= size()).
	 */
	@Override /* List */
	public E get(int index) {
		if (index < 0)
			throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + size());
		var offset = 0;
		for (var list : l) {
			var size = list.size();
			if (index < offset + size)
				return list.get(index - offset);
			offset += size;
		}
		throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + size());
	}

	/**
	 * Returns an iterator over all elements in all underlying lists.
	 *
	 * <p>
	 * The iterator traverses each list in the order they were provided to the constructor.
	 * Within each list, the iteration order follows the list's natural order.
	 *
	 * <p>
	 * The returned iterator supports the {@link Iterator#remove()} operation, which removes
	 * the current element from its underlying list.
	 *
	 * <h5 class='section'>Behavior:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>Elements from the first list are iterated first, then the second, and so on
	 * 	<li>If a list is empty, it is skipped during iteration
	 * 	<li>Calling {@link Iterator#remove()} removes the element from the underlying list
	 * 	<li>Calling {@link Iterator#next()} when {@link Iterator#hasNext()} returns <jk>false</jk>
	 * 		throws {@link NoSuchElementException}
	 * 	<li>Calling {@link Iterator#remove()} before calling {@link Iterator#next()} or calling it twice
	 * 		in a row may throw {@link IllegalStateException} (behavior depends on underlying list)
	 * </ul>
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	List&lt;String&gt; <jv>list1</jv> = <jk>new</jk> ArrayList&lt;&gt;(List.of(<js>"a"</js>, <js>"b"</js>));
	 * 	List&lt;String&gt; <jv>list2</jv> = <jk>new</jk> ArrayList&lt;&gt;(List.of(<js>"c"</js>, <js>"d"</js>));
	 * 	MultiList&lt;String&gt; <jv>multiList</jv> = <jk>new</jk> MultiList&lt;&gt;(<jv>list1</jv>, <jv>list2</jv>);
	 *
	 * 	Iterator&lt;String&gt; <jv>it</jv> = <jv>multiList</jv>.iterator();
	 * 	<jk>while</jk> (<jv>it</jv>.hasNext()) {
	 * 		String <jv>element</jv> = <jv>it</jv>.next();
	 * 		<jk>if</jk> (<jv>element</jv>.equals(<js>"b"</js>)) {
	 * 			<jv>it</jv>.remove(); <jc>// Removes "b" from list1</jc>
	 * 		}
	 * 	}
	 * </p>
	 *
	 * @return An iterator over all elements in all underlying lists.
	 */
	@Override /* List */
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
					throw new IllegalStateException();
				i2.remove();
			}
		};
	}

	/**
	 * Returns a list iterator over all elements in all underlying lists.
	 *
	 * <p>
	 * The list iterator traverses each list in the order they were provided to the constructor.
	 * The iterator starts at the beginning of the first list.
	 *
	 * <p>
	 * The returned list iterator supports the {@link ListIterator#remove()} operation, which removes
	 * the current element from its underlying list.
	 *
	 * <h5 class='section'>Behavior:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>Elements from the first list are iterated first, then the second, and so on
	 * 	<li>If a list is empty, it is skipped during iteration
	 * 	<li>Calling {@link ListIterator#remove()} removes the element from the underlying list
	 * 	<li>Bidirectional navigation is supported, but may be less efficient than forward-only iteration
	 * </ul>
	 *
	 * @return A list iterator over all elements in all underlying lists, starting at the beginning.
	 */
	@Override /* List */
	public ListIterator<E> listIterator() {
		return listIterator(0);
	}

	/**
	 * Returns a list iterator over all elements in all underlying lists, starting at the specified position.
	 *
	 * <p>
	 * The list iterator traverses each list in the order they were provided to the constructor.
	 * The iterator starts at the specified index.
	 *
	 * @param index The index to start the iterator at.
	 * @return A list iterator over all elements in all underlying lists, starting at the specified index.
	 * @throws IndexOutOfBoundsException if the index is out of range (index &lt; 0 || index &gt; size()).
	 */
	@Override /* List */
	public ListIterator<E> listIterator(int index) {
		if (index < 0 || index > size())
			throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + size());
		return new ListIterator<>() {
			int currentIndex = index;
			int listIndex = 0;
			int offset = 0;
			ListIterator<E> currentIterator = null;

			{
				// Initialize to the correct position
				for (var i = 0; i < l.length; i++) {
					var size = l[i].size();
					if (index < offset + size) {
						listIndex = i;
						currentIterator = l[i].listIterator(index - offset);
						break;
					}
					offset += size;
				}
				if (currentIterator == null && l.length > 0) {
					// Index is at the end, position at the last list
					listIndex = l.length - 1;
					currentIterator = l[listIndex].listIterator(l[listIndex].size());
				}
			}

			@Override
			public boolean hasNext() {
				if (currentIterator == null)
					return false;
				if (currentIterator.hasNext())
					return true;
				for (var j = listIndex + 1; j < l.length; j++)
					if (l[j].size() > 0)
						return true;
				return false;
			}

			@Override
			public E next() {
				if (currentIterator == null)
					throw new NoSuchElementException();
				while (! currentIterator.hasNext()) {
					if (listIndex + 1 >= l.length)
						throw new NoSuchElementException();
					listIndex++;
					currentIterator = l[listIndex].listIterator();
				}
				currentIndex++;
				return currentIterator.next();
			}

			@Override
			public boolean hasPrevious() {
				if (currentIterator == null)
					return false;
				if (currentIterator.hasPrevious())
					return true;
				for (var j = listIndex - 1; j >= 0; j--)
					if (l[j].size() > 0)
						return true;
				return false;
			}

			@Override
			public E previous() {
				if (currentIterator == null)
					throw new NoSuchElementException();
				while (! currentIterator.hasPrevious()) {
					if (listIndex == 0)
						throw new NoSuchElementException();
					listIndex--;
					currentIterator = l[listIndex].listIterator(l[listIndex].size());
				}
				currentIndex--;
				return currentIterator.previous();
			}

			@Override
			public int nextIndex() {
				return currentIndex;
			}

			@Override
			public int previousIndex() {
				return currentIndex - 1;
			}

			@Override
			public void remove() {
				if (currentIterator == null)
					throw new IllegalStateException();
				currentIterator.remove();
				currentIndex--;
			}

			@Override
			public void set(E e) {
				if (currentIterator == null)
					throw new IllegalStateException();
				currentIterator.set(e);
			}

			@Override
			public void add(E e) {
				throw new UnsupportedOperationException("MultiList does not support add operations");
			}
		};
	}

	/**
	 * Returns the total number of elements across all underlying lists.
	 *
	 * <p>
	 * This method computes the size by summing the {@link List#size()} of each
	 * underlying list. The size is recalculated each time this method is called
	 * (it is not cached).
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	List&lt;String&gt; <jv>list1</jv> = List.of(<js>"a"</js>, <js>"b"</js>);        <jc>// size = 2</jc>
	 * 	List&lt;String&gt; <jv>list2</jv> = List.of(<js>"c"</js>, <js>"d"</js>, <js>"e"</js>); <jc>// size = 3</jc>
	 * 	MultiList&lt;String&gt; <jv>multiList</jv> = <jk>new</jk> MultiList&lt;&gt;(<jv>list1</jv>, <jv>list2</jv>);
	 *
	 * 	<jk>int</jk> <jv>totalSize</jv> = <jv>multiList</jv>.size(); <jc>// Returns: 5</jc>
	 * </p>
	 *
	 * @return The sum of sizes of all underlying lists.
	 */
	@Override /* List */
	public int size() {
		var i = 0;
		for (var list : l)
			i += list.size();
		return i;
	}

	/**
	 * Returns a string representation of this MultiList.
	 *
	 * <p>
	 * The format is <c>"[[...],[...],...]"</c> where each <c>[...]</c> is the standard
	 * {@link List#toString()} representation of each underlying list.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	List&lt;String&gt; <jv>list1</jv> = List.of(<js>"a"</js>, <js>"b"</js>);
	 * 	List&lt;String&gt; <jv>list2</jv> = List.of(<js>"c"</js>, <js>"d"</js>);
	 * 	MultiList&lt;String&gt; <jv>multiList</jv> = <jk>new</jk> MultiList&lt;&gt;(<jv>list1</jv>, <jv>list2</jv>);
	 * 	<jv>multiList</jv>.toString(); <jc>// Returns: "[[a, b], [c, d]]"</jc>
	 * </p>
	 *
	 * @return A string representation of this MultiList.
	 */
	@Override
	public String toString() {
		return Arrays.stream(l).map(Object::toString).collect(Collectors.joining(", ", "[", "]"));
	}

	/**
	 * Compares the specified object with this list for equality.
	 *
	 * <p>
	 * Returns <jk>true</jk> if and only if the specified object is also a list, both lists have the
	 * same size, and all corresponding pairs of elements in the two lists are <i>equal</i>. In other
	 * words, two lists are defined to be equal if they contain the same elements in the same order.
	 *
	 * <p>
	 * This implementation first checks if the specified object is this list. If so, it returns
	 * <jk>true</jk>; if not, it checks if the specified object is a list. If not, it returns
	 * <jk>false</jk>; if so, it iterates over both lists, comparing corresponding pairs of elements.
	 *
	 * @param o The object to be compared for equality with this list.
	 * @return <jk>true</jk> if the specified object is equal to this list.
	 */
	@Override
	public boolean equals(Object o) {
		if (!(o instanceof List))
			return false;
		return eq(this, (List<?>)o, (x, y) -> {
			var e1 = x.listIterator();
			var e2 = y.listIterator();
			while (e1.hasNext() && e2.hasNext()) {
				var o1 = e1.next();
				var o2 = e2.next();
				if (!eq(o1, o2))
					return false;
			}
			return !(e1.hasNext() || e2.hasNext());
		});
	}

	/**
	 * Returns the hash code value for this list.
	 *
	 * <p>
	 * The hash code of a list is defined to be the result of the following calculation:
	 * <p class='bcode w800'>
	 * 	<jk>int</jk> hashCode = 1;
	 * 	<jk>for</jk> (E e : list)
	 * 		hashCode = 31 * hashCode + (e == <jk>null</jk> ? 0 : e.hashCode());
	 * </p>
	 *
	 * <p>
	 * This ensures that <c>list1.equals(list2)</c> implies that
	 * <c>list1.hashCode()==list2.hashCode()</c> for any two lists <c>list1</c> and <c>list2</c>,
	 * as required by the general contract of {@link Object#hashCode()}.
	 *
	 * @return The hash code value for this list.
	 */
	@Override
	public int hashCode() {
		int hashCode = 1;
		for (E e : this)
			hashCode = 31 * hashCode + (e == null ? 0 : e.hashCode());
		return hashCode;
	}
}

