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
package org.apache.juneau.rest.util;

import static org.apache.juneau.common.utils.AssertionUtils.*;
import static org.apache.juneau.common.utils.CollectionUtils.*;
import static org.apache.juneau.common.utils.Utils.*;

import java.util.*;

/**
 * A list of default implementation classes.
 *
 * <h5 class='section'>Notes:</h5><ul>
 * 	<li class='warn'>This class is not thread safe.
 * </ul>
 *
 */
public class DefaultClassList {
	/**
	 * Static creator.
	 *
	 * @return A new object.
	 */
	public static DefaultClassList create() {
		return new DefaultClassList();
	}

	/**
	 * Static creator.
	 *
	 * @param values Initial entries in this list.
	 * @return A new object initialized with the specified values.
	 */
	public static DefaultClassList of(Class<?>...values) {
		return new DefaultClassList().add(values);
	}

	private final List<Class<?>> entries;

	/**
	 * Copy constructor
	 *
	 * @param value The object to copy.
	 */
	public DefaultClassList(DefaultClassList value) {
		entries = copyOf(value.entries);
	}

	/**
	 * Constructor.
	 */
	protected DefaultClassList() {
		entries = list();
	}

	/**
	 * Prepends the specified values to the beginning of this list.
	 *
	 * @param values The values to prepend to this list.
	 * @return This object.
	 */
	public DefaultClassList add(Class<?>...values) {
		prependAll(entries, values);
		return this;
	}

	/**
	 * Creates a copy of this list.
	 *
	 * @return A copy of this list.
	 */
	public DefaultClassList copy() {
		return new DefaultClassList(this);
	}

	/**
	 * Returns the first class in this list which is a subclass of (or same as) the specified type.
	 *
	 * @param <T> The parent type.
	 * @param type The parent type to check for.
	 * @return The first class in this list which is a subclass of the specified type.
	 */
	@SuppressWarnings("unchecked")
	public <T> Optional<Class<? extends T>> get(Class<T> type) {
		assertArgNotNull("type", type);
		for (var e : entries)
			if (nn(e) && type.isAssignableFrom(e))
				return opt((Class<? extends T>)e);
		return opte();
	}
}