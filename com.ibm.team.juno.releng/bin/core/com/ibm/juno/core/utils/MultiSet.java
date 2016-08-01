/*******************************************************************************
 * Licensed Materials - Property of IBM
 * (c) Copyright IBM Corporation 2011, 2015. All Rights Reserved.
 *
 *  The source code for this program is not published or otherwise
 *  divested of its trade secrets, irrespective of what has been
 *  deposited with the U.S. Copyright Office.
 *******************************************************************************/
package com.ibm.juno.core.utils;

import static com.ibm.juno.core.utils.ThrowableUtils.*;

import java.util.*;

/**
 * Encapsulates multiple collections so they can be iterated over as if they
 * were all part of the same collection.
 *
 * @author James Bognar (jbognar@us.ibm.com)
 * @param <E> The object type of this set.
 */
public class MultiSet<E> extends AbstractSet<E> {

	/** Inner collections. */
	private List<Collection<E>> l = new ArrayList<Collection<E>>();

	/**
	 * Create a new Set that consists as a coalesced set of the specified collections.
	 *
	 * @param c Zero or more collections to add to this set.
	 */
	public MultiSet(Collection<E>...c) {
		for (Collection<E> cc : c)
			append(cc);
	}

	/**
	 * Appends the specified collection to this set of collections.
	 *
	 * @param c The collection to append to this set of collections.
	 * @return This object (for method chaining).
	 */
	public MultiSet<E> append(Collection<E> c) {
		assertFieldNotNull(c, "c");
		l.add(c);
		return this;
	}

	/**
	 * Iterates over all entries in all collections.
	 */
	@Override /* Set */
	public Iterator<E> iterator() {
		return new Iterator<E>() {
			int i = 0;
			Iterator<E> i2 = (l.size() > 0 ? l.get(i++).iterator() : null);

			@Override /* Iterator */
			public boolean hasNext() {
				if (i2 == null)
					return false;
				if (i2.hasNext())
					return true;
				for (int j = i; j < l.size(); j++)
					if (l.get(j).size() > 0)
						return true;
				return false;
			}

			@Override /* Iterator */
			public E next() {
				if (i2 == null)
					throw new NoSuchElementException();
				while (! i2.hasNext()) {
					if (i >= l.size())
						throw new NoSuchElementException();
					i2 = l.get(i++).iterator();
				}
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

	/**
	 * Enumerates over all entries in all collections.
	 *
	 * @return An enumeration wrapper around this set.
	 */
	public Enumeration<E> enumerator() {
		return Collections.enumeration(this);
	}

	@Override /* Set */
	public int size() {
		int i = 0;
		for (Collection<E> c : l)
			i += c.size();
		return i;
	}
}