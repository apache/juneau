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
package org.apache.juneau.http;

import java.util.*;

import org.apache.http.*;
import org.apache.juneau.collections.*;

/**
 * A list of {@Link NameValuePair} objects.
 */
public class NameValuePairList extends AList<NameValuePair> {

	private static final long serialVersionUID = 1L;

	/**
	 * Static creator.
	 *
	 * @return An empty list.
	 */
	@SuppressWarnings("unchecked")
	public static NameValuePairList create() {
		return new NameValuePairList();
	}

	/**
	 * Static creator.
	 *
	 * @param values The initial contents of this list.
	 * @return An empty list.
	 */
	public static NameValuePairList of(NameValuePair...values) {
		NameValuePairList l = new NameValuePairList();
		l.a(values);
		return l;
	}

	/**
	 * Appends or replaces the name-value pairs in this list.
	 *
	 * <p>
	 * If the name already exists in this list, it will be replaced with the new value.
	 * Otherwise it will be appended to the end of this list.
	 *
	 * @param values The values to append or replace in this list.
	 * @return This object (for method chaining).
	 */
	public NameValuePairList appendUnique(NameValuePair...values) {
		for (NameValuePair h : values) {
			boolean replaced = false;
			for (ListIterator<NameValuePair> li = listIterator(); li.hasNext();) {
				NameValuePair h2 = li.next();
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
	 * Returns the contents of this list as a {@link NameValuePair} array.
	 *
	 * @return The contents of this list as a {@link NameValuePair} array.
	 */
	public NameValuePair[] asArray() {
		return asArrayOf(NameValuePair.class);
	}
}
