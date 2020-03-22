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
package org.apache.juneau.collections;

import static java.util.Collections.*;

import java.lang.reflect.*;
import java.util.*;

import org.apache.juneau.json.*;
import org.apache.juneau.serializer.*;

/**
 * A fluent {@link TreeSet}.
 *
 * <p>
 * Provides various convenience methods for creating and populating a sorted set with minimal code.
 *
 * <h5 class='figure'>Examples:</h5>
 * <p class='bcode w800'>
 * 	<jc>// A set of strings.</jc>
 * 	ASortedSet&lt;String&gt; s = ASortedSet.<jsm>of</jsm>(<js>"foo"</js>,<js>"bar"</js>);
 *
 * 	<jc>// Append to set.</jc>
 * 	s.a(<js>"baz"</js>).a(<js>"qux"</js>);
 *
 * 	<jc>// Create an unmodifiable view of this set.</jc>
 * 	Set&lt;String&gt; s2 = s.unmodifiable();
 *
 * 	<jc>// Convert it to an array.</jc>
 * 	String[] array = s.asArrayOf(String.<jk>class</jk>);
 *
 * 	<jc>// Convert to simplified JSON.</jc>
 * 	String json = s.asString();
 *
 * 	<jc>// Convert to XML.</jc>
 * 	String json = s.asString(XmlSerializer.<jsf>DEFAULT</jsm>);
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
		return new ASortedSet<T>().a(t);
	}

	//------------------------------------------------------------------------------------------------------------------
	// Appenders.
	//------------------------------------------------------------------------------------------------------------------

	/**
	 * Add.
	 *
	 * <p>
	 * Adds an entry to this set.
	 *
	 * @param t The entry to add to this set.
	 * @return This object (for method chaining).
	 */
	public ASortedSet<T> a(T t) {
		add(t);
		return this;
	}

	/**
	 * Add.
	 *
	 * <p>
	 * Adds multiple entries to this set.
	 *
	 * @param t The entries to add to this set.
	 * @return This object (for method chaining).
	 */
	public ASortedSet<T> a(T...t) {
		Collections.addAll(this, t);
		return this;
	}

	/**
	 * Add all.
	 *
	 * <p>
	 * Adds multiple entries to this set.
	 *
	 * @param c The entries to add to this set.
	 * @return This object (for method chaining).
	 */
	public ASortedSet<T> aa(Collection<T> c) {
		if (c != null)
			addAll(c);
		return this;
	}

	/**
	 * Add if.
	 *
	 * <p>
	 * Adds a value to this set if the boolean value is <jk>true</jk>
	 *
	 * @param b The boolean value.
	 * @param t The value to add.
	 * @return This object (for method chaining).
	 */
	public ASortedSet<T> aif(boolean b, T t) {
		if (b)
			a(t);
		return this;
	}

	/**
	 * Add if not null.
	 *
	 * <p>
	 * Adds entries to this set skipping <jk>null</jk> values.
	 *
	 * @param t The objects to add to the list.
	 * @return This object (for method chaining).
	 */
	public ASortedSet<T> aifnn(T...t) {
		for (T o2 : t)
			if (o2 != null)
				a(o2);
		return this;
	}

	//------------------------------------------------------------------------------------------------------------------
	// Other methods.
	//------------------------------------------------------------------------------------------------------------------

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

	/**
	 * Convert to a string using the specified serializer.
	 *
	 * @param ws The serializer to use to serialize this collection.
	 * @return This collection serialized to a string.
	 */
	public String asString(WriterSerializer ws) {
		return ws.toString(this);
	}

	/**
	 * Convert to Simplified JSON.
	 *
	 * @return This collection serialized to a string.
	 */
	public String asString() {
		return SimpleJsonSerializer.DEFAULT.toString(this);
	}

	/**
	 * Convert to Simplified JSON.
	 */
	@Override /* Object */
	public String toString() {
		return asString(SimpleJsonSerializer.DEFAULT);
	}
}
