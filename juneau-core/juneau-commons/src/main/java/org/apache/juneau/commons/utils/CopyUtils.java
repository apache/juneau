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
package org.apache.juneau.commons.utils;

import static java.util.stream.Collectors.*;

import java.util.*;
import java.util.function.*;

/**
 * Utility methods for making copies and clones of collections, maps, arrays, and cloneable objects.
 *
 * <p>
 * This class is the canonical home for the {@code copyOf(...)} family (collection/map/array defensive copies)
 * and {@link #cloneOf(Object)} (reflective null-safe clone).  It is intentionally <b>subclassable</b> (via a
 * <jk>protected</jk> constructor) so that per-module utility classes can add domain-specific
 * <c>copyOf(&lt;BeanType&gt;)</c> overloads that coexist with the inherited collection overloads via ordinary Java
 * overload resolution.  A single static-import-on-demand of such a subclass brings in <b>both</b> the inherited
 * collection overloads and the module's bean overloads.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='jc'>{@link CollectionUtils}
 * </ul>
 */
@SuppressWarnings({
	"java:S1118" // Utility class with static methods only — intentionally subclassable via a protected constructor.
})
public class CopyUtils {

	/** Constructor — this class is meant to be subclassed. */
	protected CopyUtils() {}

	/**
	 * Creates a new collection from the specified collection.
	 *
	 * @param <E> The element type.
	 * @param val The value to copy from.
	 * @return A new {@link LinkedHashSet}, or <jk>null</jk> if the input was null.
	 */
	public static <E> Collection<E> copyOf(Collection<E> val) {
		return val == null ? null : new LinkedHashSet<>(val);
	}

	/**
	 * Convenience method for copying a list.
	 *
	 * @param <E> The element type.
	 * @param value The list to copy.
	 * @return A new modifiable list.
	 */
	public static <E> List<E> copyOf(List<E> value) {
		return value == null ? null : new ArrayList<>(value);
	}

	/**
	 * Makes a deep copy of the specified list.
	 *
	 * @param l The list to copy.
	 * @param valueMapper The function to apply to each value in the list.
	 * @param <E> The entry type.
	 * @return A new list with the same values as the specified list, but with values transformed by the specified function.  Null if the list being copied was null.
	 */
	public static <E> List<E> copyOf(List<E> l, Function<? super E,? extends E> valueMapper) {
		return copyOf(l, valueMapper, LinkedList::new);
	}

	/**
	 * Makes a deep copy of the specified list.
	 *
	 * @param l The list to copy.
	 * @param valueMapper The function to apply to each value in the list.
	 * @param <E> The entry type.
	 * @param listFactory The factory for creating the list.
	 * @return A new list with the same values as the specified list, but with values transformed by the specified function.  Null if the list being copied was null.
	 */
	public static <E> List<E> copyOf(List<E> l, Function<? super E,? extends E> valueMapper, Supplier<List<E>> listFactory) {
		return l == null ? null : l.stream().map(valueMapper).collect(toCollection(listFactory));
	}

	/**
	 * Creates a new map from the specified map.
	 *
	 * @param <K> The key type.
	 * @param <V> The value type.
	 * @param val The value to copy from.
	 * @return A new {@link LinkedHashMap}, or <jk>null</jk> if the input was null.
	 */
	public static <K,V> Map<K,V> copyOf(Map<K,V> val) {
		return val == null ? null : new LinkedHashMap<>(val);
	}

	/**
	 * Makes a deep copy of the specified map.
	 *
	 * @param <K> The key type.
	 * @param <V> The value type.
	 * @param m The map to copy.
	 * @param valueMapper The function to apply to each value in the map.
	 * @return A new map with the same keys as the specified map, but with values transformed by the specified function.  Null if the map being copied was null.
	 */
	public static <K,V> Map<K,V> copyOf(Map<K,V> m, Function<? super V,? extends V> valueMapper) {
		return copyOf(m, valueMapper, LinkedHashMap::new);
	}

	/**
	 * Makes a deep copy of the specified map.
	 *
	 * @param <K> The key type.
	 * @param <V> The value type.
	 * @param m The map to copy.
	 * @param valueMapper The function to apply to each value in the map.
	 * @param mapFactory The factory for creating the map.
	 * @return A new map with the same keys as the specified map, but with values transformed by the specified function.  Null if the map being copied was null.
	 */
	public static <K,V> Map<K,V> copyOf(Map<K,V> m, Function<? super V,? extends V> valueMapper, Supplier<Map<K,V>> mapFactory) {
		return m == null ? null : m.entrySet().stream().collect(toMap(Map.Entry::getKey, e -> valueMapper.apply(e.getValue()), (a, b) -> b, mapFactory));
	}

	/**
	 * Creates a new set from the specified collection.
	 *
	 * @param <E> The element type.
	 * @param val The value to copy from.
	 * @return A new {@link LinkedHashSet}, or <jk>null</jk> if the input was null.
	 */
	public static <E> Set<E> copyOf(Set<E> val) {
		return val == null ? null : new LinkedHashSet<>(val);
	}

	/**
	 * Makes a deep copy of the specified list.
	 *
	 * @param l The list to copy.
	 * @param valueMapper The function to apply to each value in the list.
	 * @param <E> The entry type.
	 * @return A new list with the same values as the specified list, but with values transformed by the specified function.  Null if the list being copied was null.
	 */
	public static <E> Set<E> copyOf(Set<E> l, Function<? super E,? extends E> valueMapper) {
		return copyOf(l, valueMapper, LinkedHashSet::new);
	}

	/**
	 * Makes a deep copy of the specified list.
	 *
	 * @param l The list to copy.
	 * @param valueMapper The function to apply to each value in the list.
	 * @param <E> The entry type.
	 * @param setFactory The factory for creating sets.
	 * @return A new list with the same values as the specified list, but with values transformed by the specified function.  Null if the list being copied was null.
	 */
	public static <E> Set<E> copyOf(Set<E> l, Function<? super E,? extends E> valueMapper, Supplier<Set<E>> setFactory) {
		return l == null ? null : l.stream().map(valueMapper).collect(toCollection(setFactory));
	}

	/**
	 * Makes a copy of the specified array.
	 *
	 * @param array The array to copy.
	 * @param <T> The element type.
	 * @return A new copy of the array, or <jk>null</jk> if the array was <jk>null</jk>.s
	 */
	public static <T> T[] copyOf(T[] array) {
		return array == null ? null : Arrays.copyOf(array, array.length);
	}

	/**
	 * Makes a copy of the specified array.
	 *
	 * @param array The array to copy.
	 * @return A new copy of the array, or <jk>null</jk> if the array was <jk>null</jk>.
	 */
	public static byte[] copyOf(byte[] array) {
		return array == null ? null : Arrays.copyOf(array, array.length);
	}

	/**
	 * Makes a copy of the specified array.
	 *
	 * @param array The array to copy.
	 * @return A new copy of the array, or <jk>null</jk> if the array was <jk>null</jk>.
	 */
	public static char[] copyOf(char[] array) {
		return array == null ? null : Arrays.copyOf(array, array.length);
	}

	/**
	 * Makes a copy of the specified array.
	 *
	 * @param array The array to copy.
	 * @return A new copy of the array, or <jk>null</jk> if the array was <jk>null</jk>.
	 */
	public static int[] copyOf(int[] array) {
		return array == null ? null : Arrays.copyOf(array, array.length);
	}

	/**
	 * Makes a copy of the specified array.
	 *
	 * @param array The array to copy.
	 * @return A new copy of the array, or <jk>null</jk> if the array was <jk>null</jk>.
	 */
	public static long[] copyOf(long[] array) {
		return array == null ? null : Arrays.copyOf(array, array.length);
	}

	/**
	 * Makes a copy of the specified array.
	 *
	 * @param array The array to copy.
	 * @return A new copy of the array, or <jk>null</jk> if the array was <jk>null</jk>.
	 */
	public static short[] copyOf(short[] array) {
		return array == null ? null : Arrays.copyOf(array, array.length);
	}

	/**
	 * Makes a copy of the specified array.
	 *
	 * @param array The array to copy.
	 * @return A new copy of the array, or <jk>null</jk> if the array was <jk>null</jk>.
	 */
	public static boolean[] copyOf(boolean[] array) {
		return array == null ? null : Arrays.copyOf(array, array.length);
	}

	/**
	 * Makes a copy of the specified array.
	 *
	 * @param array The array to copy.
	 * @return A new copy of the array, or <jk>null</jk> if the array was <jk>null</jk>.
	 */
	public static double[] copyOf(double[] array) {
		return array == null ? null : Arrays.copyOf(array, array.length);
	}

	/**
	 * Makes a copy of the specified array.
	 *
	 * @param array The array to copy.
	 * @return A new copy of the array, or <jk>null</jk> if the array was <jk>null</jk>.
	 */
	public static float[] copyOf(float[] array) {
		return array == null ? null : Arrays.copyOf(array, array.length);
	}

	/**
	 * Null-safe clone of an object via its public {@code clone()} method.
	 *
	 * <p>
	 * Invokes the object's {@code clone()} method reflectively (since {@link Object#clone()} is <jk>protected</jk> and
	 * {@link Cloneable} declares no {@code clone()} method, a generic caller cannot invoke it directly), returning the
	 * result typed as the input.
	 *
	 * @param <T> The object type.
	 * @param value The value to clone.  Can be <jk>null</jk>.
	 * @return A clone of the value, or <jk>null</jk> if the value was <jk>null</jk>.
	 * @throws IllegalArgumentException if the value's runtime type does not expose a public {@code clone()} method, or the clone failed.
	 */
	@SuppressWarnings({
		"unchecked" // Reflective clone() returns Object; the cast to the input type T is safe.
	})
	public static <T> T cloneOf(T value) {
		if (value == null)
			return null;
		try {
			return (T) value.getClass().getMethod("clone").invoke(value);
		} catch (ReflectiveOperationException e) {
			throw new IllegalArgumentException("Value of type " + value.getClass().getName() + " is not cloneable via a public clone() method.", e);
		}
	}
}
