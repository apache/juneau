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
import java.util.function.*;

import org.apache.juneau.json.*;
import org.apache.juneau.serializer.*;

/**
 * A fluent {@link TreeSet}.
 *
 * <p>
 * Provides various convenience methods for creating and populating a sorted set with minimal code.
 *
 * <h5 class='figure'>Examples:</h5>
 * <p class='bjava'>
 * 	<jc>// A set of strings.</jc>
 * 	ASortedSet&lt;String&gt; <jv>set</jv> = ASortedSet.<jsm>of</jsm>(<js>"foo"</js>,<js>"bar"</js>);
 *
 * 	<jc>// Append to set.</jc>
 * 	<jv>set</jv>.a(<js>"baz"</js>).a(<js>"qux"</js>);
 *
 * 	<jc>// Create an unmodifiable view of this set.</jc>
 * 	Set&lt;String&gt; <jv>set2</jv> = <jv>set</jv>.unmodifiable();
 *
 * 	<jc>// Convert it to an array.</jc>
 * 	String[] <jv>array</jv> = <jv>set</jv>.asArrayOf(String.<jk>class</jk>);
 *
 * 	<jc>// Convert to simplified JSON.</jc>
 * 	String <jv>json</jv> = <jv>set</jv>.asString();
 *
 * 	<jc>// Convert to XML.</jc>
 * 	String <jv>json</jv> = <jv>set</jv>.asString(XmlSerializer.<jsf>DEFAULT</jsm>);
 * </p>
 *
 * <ul class='spaced-list'>
 * 	<li class='warn'>This class is not thread safe.
 * </ul>
 *
 * <ul class='seealso'>
 * 	<li class='extlink'>{@source}
 * </ul>
 *
 * @param <T> The entry type.
 * @serial exclude
 */
@SuppressWarnings({"unchecked"})
public class ASortedSet<T> extends TreeSet<T> {

	private static final long serialVersionUID = 1L;

	//------------------------------------------------------------------------------------------------------------------
	// Constructors
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
	// Creators
	//------------------------------------------------------------------------------------------------------------------

	/**
	 * Convenience method for creating a list of objects.
	 *
	 * @return A new list.
	 */
	public static <T> ASortedSet<T> create() {
		return new ASortedSet<>();
	}

	/**
	 * Convenience method for creating a list of objects.
	 *
	 * @param values The initial values.
	 * @return A new list.
	 */
	@SafeVarargs
	public static <T> ASortedSet<T> of(T...values) {
		return new ASortedSet<T>().a(values);
	}

	/**
	 * Convenience method for creating a list of objects.
	 *
	 * @param values The initial values.
	 * @return A new list.
	 */
	public static <T> ASortedSet<T> of(Collection<T> values) {
		return new ASortedSet<T>().a(values);
	}

	//------------------------------------------------------------------------------------------------------------------
	// Appenders
	//------------------------------------------------------------------------------------------------------------------

	/**
	 * Adds the value to this set.
	 *
	 * @param value The value to add to this set.
	 * @return This object.
	 */
	public ASortedSet<T> append(T value) {
		add(value);
		return this;
	}

	/**
	 * Adds all the values in the specified array to this set.
	 *
	 * @param values The values to add to this set.
	 * @return This object.
	 */
	public ASortedSet<T> append(T...values) {
		Collections.addAll(this, values);
		return this;
	}

	/**
	 * Adds all the values in the specified collection to this set.
	 *
	 * @param values The values to add to this set.
	 * @return This object.
	 */
	public ASortedSet<T> append(Collection<? extends T> values) {
		addAll(values);
		return this;
	}

	/**
	 * Same as {@link #append(Object)}.
	 *
	 * @param value The entry to add to this set.
	 * @return This object.
	 */
	public ASortedSet<T> a(T value) {
		return append(value);
	}

	/**
	 * Same as {@link #append(Object[])}.
	 *
	 * @param values The entries to add to this set.
	 * @return This object.
	 */
	public ASortedSet<T> a(T...values) {
		return append(values);
	}

	/**
	 * Same as {@link #append(Collection)}.
	 *
	 * @param values The entries to add to this set.
	 * @return This object.
	 */
	public ASortedSet<T> a(Collection<? extends T> values) {
		return append(values);
	}

	/**
	 * Adds a value to this set if the boolean value is <jk>true</jk>
	 *
	 * @param flag The boolean value.
	 * @param value The value to add.
	 * @return This object.
	 */
	public ASortedSet<T> appendIf(boolean flag, T value) {
		if (flag)
			a(value);
		return this;
	}

	/**
	 * Adds entries to this set skipping <jk>null</jk> values.
	 *
	 * @param values The objects to add to the list.
	 * @return This object.
	 */
	public ASortedSet<T> appendIfNotNull(T...values) {
		for (T o2 : values)
			if (o2 != null)
				a(o2);
		return this;
	}

	/**
	 * Add if predicate matches.
	 *
	 * @param test The predicate to match against.
	 * @param value The value to add if the predicate matches.
	 * @return This object.
	 */
	public ASortedSet<T> appendIf(Predicate<Object> test, T value) {
		return appendIf(test.test(value), value);
	}

	//------------------------------------------------------------------------------------------------------------------
	// Other methods
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
	public String asJson() {
		return SimpleJsonSerializer.DEFAULT.toString(this);
	}
}
