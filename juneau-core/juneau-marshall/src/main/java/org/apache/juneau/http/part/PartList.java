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
package org.apache.juneau.http.part;

import static java.util.Collections.*;
import static org.apache.juneau.internal.StringUtils.*;
import static java.util.Arrays.*;

import java.util.*;

import org.apache.juneau.*;

/**
 * An unmodifiable list of HTTP parts.
 */
public class PartList implements Iterable<Part> {

	/** Represents no part supplier in annotations. */
	public static final class Null extends PartList {}

	/** Predefined instance. */
	public static final PartList EMPTY = create().build();

	final List<Part> parts;

	/**
	 * Instantiates a new builder for this bean.
	 *
	 * @return A new builder.
	 */
	public static PartListBuilder create() {
		return new PartListBuilder();
	}

	/**
	 * Creates a new {@link PartList} initialized with the specified parts.
	 *
	 * @param parts The parts to add to the list.  Can be <jk>null</jk>.  <jk>null</jk> entries are ignored.
	 * @return A new unmodifiable instance, never <jk>null</jk>.
	 */
	public static PartList of(List<Part> parts) {
		return parts == null || parts.isEmpty() ? EMPTY : new PartList(parts);
	}

	/**
	 * Creates a new {@link PartList} initialized with the specified parts.
	 *
	 * @param parts The parts to add to the list.  <jk>null</jk> entries are ignored.
	 * @return A new unmodifiable instance, never <jk>null</jk>.
	 */
	public static PartList of(Part...parts) {
		return parts == null || parts.length == 0 ? EMPTY : new PartList(asList(parts));
	}

	/**
	 * Creates a new {@link PartList} initialized with the specified name/value pairs.
	 *
	 * @param pairs
	 * 	Initial list of pairs.
	 * 	<br>Must be an even number of parameters representing key/value pairs.
	 * @throws RuntimeException If odd number of parameters were specified.
	 * @return A new instance.
	 */
	public static PartList ofPairs(Object...pairs) {
		if (pairs.length == 0)
			return EMPTY;
		if (pairs.length % 2 != 0)
			throw new BasicRuntimeException("Odd number of parameters passed into PartList.ofPairs()");
		PartListBuilder b = create();
		for (int i = 0; i < pairs.length; i+=2)
			b.add(stringify(pairs[i]), pairs[i+1]);
		return new PartList(b);
	}

	/**
	 * Constructor.
	 *
	 * @param builder The builder containing the settings for this bean.
	 */
	public PartList(PartListBuilder builder) {
		this.parts = new ArrayList<>(builder.parts);
	}

	/**
	 * Constructor.
	 *
	 * @param parts The initial list of parts.  <jk>null</jk> entries are ignored.
	 */
	protected PartList(List<Part> parts) {
		if (parts == null || parts.isEmpty())
			this.parts = emptyList();
		else {
			List<Part> l = new ArrayList<>();
			for (int i = 0; i < parts.size(); i++) {
				Part x = parts.get(i);
				if (x != null)
					l.add(x);
			}
			this.parts = unmodifiableList(l);
		}
	}

	/**
	 * Default constructor.
	 */
	protected PartList() {
		this.parts = emptyList();
	}

	/**
	 * Returns a builder initialized with the contents of this bean.
	 *
	 * @return A new builder object.
	 */
	public PartListBuilder copy() {
		return new PartListBuilder(this);
	}

	/**
	 * Gets all of the headers with the given name.
	 *
	 * <p>
	 * The returned array maintains the relative order in which the headers were added.
	 *
	 * <p>
	 * Part name comparison is case sensitive.
	 *
	 * @param name The header name.
	 *
	 * @return An array containing all matching headers, or an empty array if none are found.
	 */
	public List<Part> get(String name) {
		List<Part> l = null;
		for (int i = 0; i < parts.size(); i++) {  // See HTTPCORE-361
			Part x = parts.get(i);
			if (x.getName().equals(name)) {
				if (l == null)
					l = new ArrayList<>();
				l.add(x);
			}
		}
		return l == null ? emptyList() : l;
	}

	/**
	 * Gets the first header with the given name.
	 *
	 * <p>
	 * Part name comparison is case sensitive.
	 *
	 * @param name The header name.
	 * @return The first matching header, or <jk>null</jk> if not found.
	 */
	public Part getFirst(String name) {
		for (int i = 0; i < parts.size(); i++) {  // See HTTPCORE-361
			Part x = parts.get(i);
			if (x.getName().equals(name))
				return x;
		}
		return null;
	}

	/**
	 * Gets the last header with the given name.
	 *
	 * <p>
	 * Part name comparison is case sensitive.
	 *
	 * @param name The header name.
	 * @return The last matching header, or <jk>null</jk> if not found.
	 */
	public Part getLast(String name) {
		for (int i = parts.size() - 1; i >= 0; i--) {
			Part x = parts.get(i);
			if (x.getName().equals(name))
				return x;
		}
		return null;
	}

	/**
	 * Gets all of the headers contained within this list.
	 *
	 * @return An array containing all the parts within this list, or an empty list if no parts are present.
	 */
	public List<Part> getAll() {
		return unmodifiableList(parts);
	}

	/**
	 * Tests if headers with the given name are contained within this list.
	 *
	 * <p>
	 * Part name comparison is case sensitive.
	 *
	 * @param name The header name.
	 * @return <jk>true</jk> if at least one header with the name is present.
	 */
	public boolean contains(String name) {
		for (int i = 0; i < parts.size(); i++) {  // See HTTPCORE-361
			Part x = parts.get(i);
			if (x.getName().equals(name))
				return true;
		}
		return false;
	}

	@Override /* Iterable */
	public Iterator<Part> iterator() {
		return getAll().iterator();
	}

	/**
	 * Returns this list as a URL-encoded custom query.
	 */
	@Override /* Object */
	public String toString() {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < parts.size(); i++) {
			Part p = parts.get(i);
			String v = p.getValue();
			if (v != null) {
				if (sb.length() > 0)
					sb.append("&");
				sb.append(urlEncode(p.getName())).append('=').append(urlEncode(p.getValue()));
			}
		}
		return sb.toString();
	}
}
