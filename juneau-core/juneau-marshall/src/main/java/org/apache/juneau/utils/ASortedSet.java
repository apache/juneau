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
package org.apache.juneau.utils;

import static java.util.Collections.*;

import java.lang.reflect.*;
import java.util.*;

/**
 * An extension of {@link TreeSet} with a convenience {@link #append(Object)} method.
 *
 * <p>
 * Primarily used for testing purposes for quickly creating populated sets.
 * <p class='bcode w800'>
 * 	<jc>// Example:</jc>
 * 	TreeSet&lt;String&gt; s = <jk>new</jk> ATreeSet&lt;String&gt;().append(<js>"foo"</js>).append(<js>"bar"</js>);
 * </p>
 *
 * @param <T> The entry type.
 */
@SuppressWarnings({"unchecked"})
public final class ASortedSet<T> extends TreeSet<T> {

	private static final long serialVersionUID = 1L;

	//------------------------------------------------------------------------------------------------------------------
	// Constructors.
	//------------------------------------------------------------------------------------------------------------------

	/**
	 * Constructor.
	 */
	public ASortedSet() {}

	/**
	 * Constructor.
	 *
	 * @param c Comparator.
	 */
	public ASortedSet(Comparator<T> c) {
		super(c);
	}

	/**
	 * Copy constructor.
	 *
	 * @param c Initial contents.  Can be <jk>null</jk>.
	 */
	public ASortedSet(Collection<T> c) {
		super(c == null ? emptySet() : c);
	}

	//------------------------------------------------------------------------------------------------------------------
	// Creators.
	//------------------------------------------------------------------------------------------------------------------

	/**
	 * Convenience method for creating a list of objects.
	 *
	 * @param t The initial values.
	 * @return A new list.
	 */
	@SafeVarargs
	public static <T> ASortedSet<T> of(T...t) {
		return new ASortedSet<T>().appendAll(t);
	}

	//------------------------------------------------------------------------------------------------------------------
	// Methods.
	//------------------------------------------------------------------------------------------------------------------

	/**
	 * Adds an entry to this set.
	 *
	 * @param t The entry to add to this set.
	 * @return This object (for method chaining).
	 */
	public ASortedSet<T> append(T t) {
		add(t);
		return this;
	}

	/**
	 * Adds multiple entries to this set.
	 *
	 * @param t The entries to add to this set.
	 * @return This object (for method chaining).
	 */
	public ASortedSet<T> appendAll(T...t) {
		Collections.addAll(this, t);
		return this;
	}

	/**
	 * Adds multiple entries to this set.
	 *
	 * @param c The entries to add to this set.
	 * @return This object (for method chaining).
	 */
	public ASortedSet<T> appendAll(Collection<T> c) {
		if (c != null)
			addAll(c);
		return this;
	}

	/**
	 * Adds a value to this set if the boolean value is <jk>true</jk>
	 *
	 * @param b The boolean value.
	 * @param t The value to add.
	 * @return This object (for method chaining).
	 */
	public ASortedSet<T> appendIf(boolean b, T t) {
		if (b)
			append(t);
		return this;
	}

	/**
	 * Returns an unmodifiable view of this set.
	 *
	 * @return An unmodifiable view of this set.
	 */
	public SortedSet<T> unmodifiable() {
		return isEmpty() ? emptySortedSet() : unmodifiableSortedSet(this);
	}

	/**
	 * Convert the contents of this set into a new array.
	 *
	 * @param c The component type of the array.
	 * @return A new array.
	 */
	public <T2> T2[] asArrayOf(Class<T2> c) {
		return toArray((T2[])Array.newInstance(c, size()));
	}
}
