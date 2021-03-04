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

import java.util.*;

/**
 * An unmodifiable group of HTTP parts.
 */
public class PartGroup {

	/** Predefined instance. */
	public static final PartGroup INSTANCE = create().build();

	private static final Part[] EMPTY = new Part[] {};

	private final List<Part> parts;

	/**
	 * Instantiates a new builder for this bean.
	 *
	 * @return A new builder.
	 */
	public static PartGroupBuilder create() {
		return new PartGroupBuilder();
	}

	/**
	 * Constructor.
	 *
	 * @param builder The builder containing the settings for this bean.
	 */
	public PartGroup(PartGroupBuilder builder) {
		this.parts = new ArrayList<>(builder.parts);
	}

	/**
	 * Returns a builder initialized with the contents of this bean.
	 *
	 * @return A new builder object.
	 */
	public PartGroupBuilder builder() {
		return create().add(parts);
	}

	/**
	 * Gets all of the headers with the given name.
	 *
	 * <p>
	 * The returned array maintains the relative order in which the headers were added.
	 *
	 * <p>
	 * Part name comparison is case insensitive.
	 *
	 * @param name The header name.
	 *
	 * @return An array containing all matching headers, or an empty array if none are found.
	 */
	public Part[] getParts(final String name) {
		// HTTPCORE-361 : we don't use the for-each syntax, i.e.
		//	 for (Part header : headers)
		// as that creates an Iterator that needs to be garbage-collected
		List<Part> l = null;
		for (int i = 0; i < parts.size(); i++) {
			Part x = parts.get(i);
			if (x.getName().equals(name)) {
				if (l == null)
					l = new ArrayList<>();
				l.add(x);
			}
		}
		return l != null ? l.toArray(new Part[l.size()]) : EMPTY;
	}

	/**
	 * Gets the first header with the given name.
	 *
	 * <p>
	 * Part name comparison is case insensitive.
	 *
	 * @param name The header name.
	 * @return The first matching header, or <jk>null</jk> if not found.
	 */
	public Part getFirstPart(String name) {
		// HTTPCORE-361 : we don't use the for-each syntax, i.e.
		//	 for (Part header : headers)
		// as that creates an Iterator that needs to be garbage-collected
		for (int i = 0; i < parts.size(); i++) {
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
	 * Part name comparison is case insensitive.
	 *
	 * @param name The header name.
	 * @return The last matching header, or <jk>null</jk> if not found.
	 */
	public Part getLastPart(String name) {
		for (int i = parts.size() - 1; i >= 0; i--) {
			Part x = parts.get(i);
			if (x.getName().equals(name))
				return x;
		}
		return null;
	}

	/**
	 * Gets all of the headers contained within this group.
	 *
	 * @return An array containing all the headers within this group, or an empty array if no headers are present.
	 */
	public List<Part> getAllParts() {
		return Collections.unmodifiableList(parts);
	}

	/**
	 * Tests if headers with the given name are contained within this group.
	 *
	 * <p>
	 * Part name comparison is case insensitive.
	 *
	 * @param name The header name.
	 * @return <jk>true</jk> if at least one header with the name is present.
	 */
	public boolean containsPart(String name) {
		// HTTPCORE-361 : we don't use the for-each syntax, i.e.
		//	 for (Part header : headers)
		// as that creates an Iterator that needs to be garbage-collected
		for (int i = 0; i < parts.size(); i++) {
			Part x = parts.get(i);
			if (x.getName().equals(name))
				return true;
		}
		return false;
	}

	/**
	 * Returns a copy of this object.
	 *
	 * <p>
	 * Individual header values are not cloned.
	 *
	 * @return A copy of this object.
	 */
	public PartGroup copy() {
		return builder().build();
	}

	@Override /* Object */
	public String toString() {
		return parts.toString();
	}
}
