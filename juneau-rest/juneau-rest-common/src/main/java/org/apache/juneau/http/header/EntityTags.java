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
package org.apache.juneau.http.header;

import static org.apache.juneau.common.internal.StringUtils.*;
import static org.apache.juneau.internal.ArrayUtils.copyOf;
import static org.apache.juneau.internal.CollectionUtils.*;

import java.util.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.internal.*;

/**
 * A list of {@link EntityTag} beans.
 */
@BeanIgnore
public class EntityTags {
	//-----------------------------------------------------------------------------------------------------------------
	// Static
	//-----------------------------------------------------------------------------------------------------------------

	/** Represents an empty entity tags object. */
	public static final EntityTags EMPTY = new EntityTags("");

	private static final Cache<String,EntityTags> CACHE = Cache.of(String.class, EntityTags.class).build();

	/**
	 * Returns a parsed entity tags header value.
	 *
	 * @param value The raw header value.
	 * @return A parsed header value.
	 */
	public static EntityTags of(String value) {
		return isEmpty(value) ? EMPTY : CACHE.get(value, ()->new EntityTags(value));
	}

	/**
	 * Returns a parsed entity tags header value.
	 *
	 * @param value The header value.
	 * @return A parsed header value.
	 */
	public static EntityTags of(EntityTag...value) {
		return value == null ? null : new EntityTags(value);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Instance
	//-----------------------------------------------------------------------------------------------------------------

	private final EntityTag[] value;
	private final String string;

	/**
	 * Constructor.
	 *
	 * @param value The header value.
	 */
	public EntityTags(String value) {
		this.string = value;
		this.value = parse(value);
	}

	/**
	 * Constructor.
	 *
	 * @param value The header value.
	 */
	public EntityTags(EntityTag...value) {
		this.string = join(value, ", ");
		this.value = copyOf(value);
	}

	private EntityTag[] parse(String value) {
		if (value == null)
			return null;
		String[] s = split(value);
		EntityTag[] v = new EntityTag[s.length];
		for (int i = 0; i < s.length; i++)
			v[i] = EntityTag.of(s[i]);
		return v;
	}

	/**
	 * Returns the entity tags in this object as a list.
	 *
	 * <p>
	 * Returns an unmodifiable list.
	 *
	 * @return The entity tags in this object as a list.  Can be <jk>null</jk>.
	 */
	public List<EntityTag> toList() {
		return ulist(value);
	}

	/**
	 * Returns the entity tags in this object as an array.
	 *
	 * <p>
	 * Returns a copy of the entity tags.
	 *
	 * @return The entity tags in this object as an array.  Can be <jk>null</jk>.
	 */
	public EntityTag[] toArray() {
		return copyOf(value);
	}

	@Override /* Object */
	public String toString() {
		return string;
	}
}
