/*******************************************************************************
 * Licensed Materials - Property of IBM
 * (c) Copyright IBM Corporation 2015. All Rights Reserved.
 *
 *  The source code for this program is not published or otherwise
 *  divested of its trade secrets, irrespective of what has been
 *  deposited with the U.S. Copyright Office.
 *******************************************************************************/
package com.ibm.juno.core.utils;

import static com.ibm.juno.core.utils.ThrowableUtils.*;

import java.util.*;

/**
 * Utility class for defining an iterator over one or more iterables.
 * @param <E> The element class type.
 */
public class MultiIterable<E> implements Iterable<E> {

	final List<Iterator<E>> iterators = new LinkedList<Iterator<E>>();

	/**
	 * Constructor.
	 *
	 * @param iterators The list of iterators to iterate over.
	 */
	public MultiIterable(Iterator<E>...iterators) {
		for (Iterator<E> i : iterators)
			append(i);
	}

	/**
	 * Appends the specified iterator to this list of iterators.
	 *
	 * @param iterator The iterator to append.
	 * @return This object (for method chaining).
	 */
	public MultiIterable<E> append(Iterator<E> iterator) {
		assertFieldNotNull(iterator, "iterator");
		this.iterators.add(iterator);
		return this;
	}

	@Override /* Iterable */
	public Iterator<E> iterator() {
		return new Iterator<E>() {
			Iterator<Iterator<E>> i1 = iterators.iterator();
			Iterator<E> i2 = i1.hasNext() ? i1.next() : null;

			@Override /* Iterator */
			public boolean hasNext() {
				while (i2 != null && ! i2.hasNext())
					i2 = (i1.hasNext() ? i1.next() : null);
				return (i2 != null);
			}

			@Override /* Iterator */
			public E next() {
				hasNext();
				if (i2 == null)
					throw new NoSuchElementException();
				return i2.next();
			}

			@Override /* Iterator */
			public void remove() {
				if (i2 == null)
					throw new NoSuchElementException();
				i2.remove();
			}
		};
	}
}