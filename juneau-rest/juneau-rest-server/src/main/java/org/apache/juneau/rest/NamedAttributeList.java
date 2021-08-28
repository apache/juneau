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
package org.apache.juneau.rest;

import java.util.*;

import org.apache.juneau.collections.*;

/**
 * A list of {@link NamedAttribute} objects.
 */
public class NamedAttributeList extends AList<NamedAttribute> {

	private static final long serialVersionUID = 1L;

	/**
	 * Static creator.
	 *
	 * @return An empty list.
	 */
	@SuppressWarnings("unchecked")
	public static NamedAttributeList create() {
		return new NamedAttributeList();
	}

	/**
	 * Static creator.
	 *
	 * @param values The initial contents of this list.
	 * @return An empty list.
	 */
	public static NamedAttributeList of(NamedAttribute...values) {
		NamedAttributeList l = new NamedAttributeList();
		l.a(values);
		return l;
	}

	/**
	 * Returns a copy of this list.
	 *
	 * @return A new copy of this list.
	 */
	public NamedAttributeList copy() {
		return NamedAttributeList.of(asArray());
	}

	/**
	 * Appends or replaces the named attribute values in this list.
	 *
	 * <p>
	 * If the named attribute already exists in this list, it will be replaced with the new value.
	 * Otherwise it will be appended to the end of this list.
	 *
	 * @param values The values to append or replace in this list.
	 * @return This object (for method chaining).
	 */
	public NamedAttributeList appendUnique(List<NamedAttribute> values) {
		for (NamedAttribute h : values) {
			boolean replaced = false;
			for (ListIterator<NamedAttribute> li = listIterator(); li.hasNext();) {
				NamedAttribute h2 = li.next();
				if (h2.getName().equalsIgnoreCase(h.getName())) {
					li.set(h);
					replaced = true;
					break;
				}
			}
			if (! replaced)
				add(h);
		}
		return this;
	}

	/**
	 * Appends or replaces the named attribute values in this list.
	 *
	 * <p>
	 * If the named attribute already exists in this list, it will be replaced with the new value.
	 * Otherwise it will be appended to the end of this list.
	 *
	 * @param values The values to append or replace in this list.
	 * @return This object (for method chaining).
	 */
	public NamedAttributeList appendUnique(NamedAttribute...values) {
		return appendUnique(Arrays.asList(values));
	}

	/**
	 * Returns the contents of this list as a {@link NamedAttribute} array.
	 *
	 * @return The contents of this list as a {@link NamedAttribute} array.
	 */
	public NamedAttribute[] asArray() {
		return asArrayOf(NamedAttribute.class);
	}
}
