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
package org.apache.juneau;

import java.lang.reflect.*;
import java.util.*;

import org.apache.juneau.collections.*;

/**
 * Subclass of {@link OMap} that avoids adding common default values.
 */
public class DefaultFilteringOMap extends OMap {

	private static final long serialVersionUID = 1L;

	/**
	 * Constructor.
	 *
	 * @param m
	 * 	The object map to copy from.
	 * 	<br>Can be <jk>null</jk>.
	 */
	public DefaultFilteringOMap(OMap m) {
		super();
		appendAll(m);
	}

	/**
	 * Constructor.
	 */
	public DefaultFilteringOMap() {
		super();
	}

	@Override /* OMap */
	public OMap append(String key, Object value) {
		if (! shouldSkip(value))
			super.append(key, value);
		return this;
	}

	@Override /* OMap */
	public OMap a(String key, Object value) {
		if (! shouldSkip(value))
			super.a(key, value);
		return this;
	}

	/**
	 * Returns <jk>true</jk> if the specified value should be skipped.
	 *
	 * @param value The value to check.
	 * @return <jk>true</jk> if the specified value should be skipped.
	 */
	protected boolean shouldSkip(Object value) {
		return
			value == null
			|| (value instanceof Boolean && value.equals(false))
			|| (value instanceof Number && ((Number)value).intValue() == -1)
			|| (value.getClass().isArray() && Array.getLength(value) == 0)
			|| (value instanceof Map && ((Map<?,?>)value).isEmpty())
			|| (value instanceof Collection && ((Collection<?>)value).isEmpty());

	}
}
