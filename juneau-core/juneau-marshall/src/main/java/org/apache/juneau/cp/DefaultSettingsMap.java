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
package org.apache.juneau.cp;

import static org.apache.juneau.internal.CollectionUtils.*;

import java.util.*;

/**
 * A list of default settings.
 *
 * <p>
 * Consists of a simple string-keyed map of arbitrary objects.
 *
 * <h5 class='section'>Notes:</h5><ul>
 * 	<li class='warn'>This class is not thread safe.
 * </ul>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * </ul>
 */
public class DefaultSettingsMap {

	//-----------------------------------------------------------------------------------------------------------------
	// Static
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Static creator.
	 *
	 * @return A new object.
	 */
	public static DefaultSettingsMap create() {
		return new DefaultSettingsMap();
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Instance
	//-----------------------------------------------------------------------------------------------------------------

	private final Map<String,Object> entries;

	/**
	 * Constructor.
	 */
	protected DefaultSettingsMap() {
		entries = map();
	}

	/**
	 * Copy constructor
	 *
	 * @param value The object to copy.
	 */
	public DefaultSettingsMap(DefaultSettingsMap value) {
		entries = copyOf(value.entries);
	}

	/**
	 * Sets the specified setting value.
	 *
	 * @param name The setting name.
	 * @param value The setting value.
	 * @return This object.
	 */
	public DefaultSettingsMap set(String name, Object value) {
		entries.put(name, value);
		return this;
	}

	/**
	 * Returns the value of the specified setting if it exists.
	 *
	 * @param <T> The return type.
	 * @param type The setting type.
	 * @param name The setting name.
	 * @return The setting value.
	 */
	@SuppressWarnings("unchecked")
	public <T> Optional<T> get(Class<T> type, String name) {
		return optional((T)entries.get(name));
	}

	/**
	 * Creates a copy of this map.
	 *
	 * @return A copy of this map.
	 */
	public DefaultSettingsMap copy() {
		return new DefaultSettingsMap(this);
	}
}
