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
package org.apache.juneau.commons.lang;

import static java.util.Arrays.*;

import java.lang.annotation.*;
import java.util.*;

import org.apache.juneau.commons.utils.*;

/**
 * Utility class for generating integer hash codes.
 *
 * <p>
 * General usage:
 * <p class='bjava'>
 * 	<jk>int</jk> <jv>hashCode</jv> = HashCode.<jsm>create</jsm>().add(<js>"foobar"</js>).add(<jv>myobject</jv>).add(123).get();
 * </p>
 *
 */
public class HashCode {

	/**
	 * Create a new HashCode object.
	 *
	 * @return A new HashCode object.
	 */
	public static final HashCode create() {
		return new HashCode();
	}

	/**
	 * Calculates a hash code over the specified objects.
	 *
	 * <p>
	 * Uses the same algorithm as {@link java.util.Objects#hash(Object...)} (31 * result + element hash).
	 *
	 * <p>
	 * Special handling is provided for:
	 * <ul>
	 * 	<li><b>Annotations:</b> Uses {@link AnnotationUtils#hash(Annotation)} to ensure consistent hashing
	 * 		according to the {@link java.lang.annotation.Annotation#hashCode()} contract.
	 * 	<li><b>Arrays:</b> Uses content-based hashing via {@link java.util.Arrays#hashCode(Object[])}
	 * 		instead of identity-based hashing.
	 * 	<li><b>Null values:</b> Treated as 0 in the hash calculation.
	 * </ul>
	 *
	 * @param objects The objects to calculate a hashcode over.
	 * @return A numerical hashcode value.
	 * @see #add(Object)
	 * @see AnnotationUtils#hash(Annotation)
	 * @see java.util.Objects#hash(Object...)
	 */
	public static final int of(Object...objects) {
		HashCode x = create();
		for (var oo : objects)
			x.add(oo);
		return x.get();
	}

	@SuppressWarnings("java:S1845") // Field name intentionally matches Object.hashCode() method name
	private int hashCode = 1;

	/**
	 * Hashes the hashcode into this object.
	 *
	 * <p>
	 * The formula is simply <c>hashCode = 31*hashCode + i;</c>
	 *
	 * @param i The hashcode to hash into this object's hashcode.
	 * @return This object.
	 */
	public HashCode add(int i) {
		hashCode = 31 * hashCode + i;
		return this;
	}

	/**
	 * Hashes the hashcode of the specified object into this object.
	 *
	 * <p>
	 * The formula is <c>hashCode = 31*hashCode + elementHash;</c>
	 *
	 * <p>
	 * Special handling is provided for:
	 * <ul>
	 * 	<li><b>Null values:</b> Adds 0 to the hash code.
	 * 	<li><b>Annotations:</b> Uses {@link AnnotationUtils#hash(Annotation)} to ensure consistent hashing
	 * 		according to the {@link java.lang.annotation.Annotation#hashCode()} contract.
	 * 	<li><b>Arrays:</b> Uses content-based hashing via {@link java.util.Arrays#hashCode(Object[])}
	 * 		instead of identity-based hashing. Supports all primitive array types and object arrays.
	 * 	<li><b>Other objects:</b> Uses the object's {@link Object#hashCode()} method.
	 * </ul>
	 *
	 * @param o The object whose hashcode will be hashed with this object.
	 * @return This object.
	 * @see AnnotationUtils#hash(Annotation)
	 * @see java.util.Arrays#hashCode(Object[])
	 */
	public HashCode add(Object o) {
		o = unswap(o);
		if (o == null) {
			add(0);
		} else if (o instanceof Annotation a) {
			add(AnnotationUtils.hash(a));
		} else if (o.getClass().isArray()) {
			// Use content-based hashcode for arrays
			if (o instanceof Object[] o2)
				add(deepHashCode(o2));
			else if (o instanceof int[] o2)
				add(Arrays.hashCode(o2));
			else if (o instanceof long[] o2)
				add(Arrays.hashCode(o2));
			else if (o instanceof short[] o2)
				add(Arrays.hashCode(o2));
			else if (o instanceof byte[] o2)
				add(Arrays.hashCode(o2));
			else if (o instanceof char[] o2)
				add(Arrays.hashCode(o2));
			else if (o instanceof boolean[] o2)
				add(Arrays.hashCode(o2));
			else if (o instanceof float[] o2)
				add(java.util.Arrays.hashCode(o2));
			else if (o instanceof double[] o2)
				add(java.util.Arrays.hashCode(o2));
		} else {
			add(o.hashCode());
		}
		return this;
	}

	/**
	 * Return the calculated hashcode value.
	 *
	 * @return The calculated hashcode.
	 */
	public int get() {
		return hashCode;
	}

	/**
	 * Converts the object to a normalized form before grabbing it's hashcode.
	 *
	 * <p>
	 * Subclasses can override this method to provide specialized handling (e.g. converting numbers to strings so that
	 * <c>123</c> and <js>"123"</js> end up creating the same hashcode.)
	 *
	 * <p>
	 * Default implementation does nothing.
	 *
	 * @param o The object to normalize before getting it's hashcode.
	 * @return The normalized object.
	 */
	protected Object unswap(Object o) {
		return o;
	}
}