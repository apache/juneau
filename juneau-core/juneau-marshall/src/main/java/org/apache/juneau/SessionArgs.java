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

import org.apache.juneau.json.*;

/**
 * Runtime arguments common to all bean, serializer, and parser sessions.
 */
public class SessionArgs {

	/**
	 * Default empty session arguments.
	 */
	public static final SessionArgs DEFAULT = new SessionArgs();

	ObjectMap properties;

	/**
	 * Constructor.
	 */
	public SessionArgs() {}

	//-----------------------------------------------------------------------------------------------------------------
	// Properties.
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Session-level properties.
	 *
	 * <p>
	 * Overrides context-level properties.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk>.
	 * @return This object (for method chaining).
	 */
	public SessionArgs properties(ObjectMap value) {
		this.properties = value;
		return this;
	}

	/**
	 * Adds a property to this session.
	 *
	 * @param key The property key.
	 * @param value The property value.
	 * @return This object (for method chaining).
	 */
	public SessionArgs property(String key, Object value) {
		if (value == null) {
			if (properties != null)
				properties.remove(key);
		} else {
			if (properties == null)
				properties = new ObjectMap();
			properties.put(key, value);
		}
		return this;
	}

	/**
	 * Returns a property on this session.
	 *
	 * @param key The property key.
	 * @return The property value, or <jk>null</jk> if not set.
	 */
	public Object getProperty(String key) {
		if (properties != null)
			return properties.get(key);
		return null;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Other methods
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Returns the properties defined on this object as a simple map for debugging purposes.
	 *
	 * @return A new map containing the properties defined on this object.
	 */
	public ObjectMap toMap() {
		return new DefaultFilteringObjectMap()
			.append("SessionArgs", new DefaultFilteringObjectMap()
				.append("properties", properties)
			);
	}

	@Override /* Object */
	public String toString() {
		return SimpleJsonSerializer.DEFAULT_READABLE.toString(toMap());
	}
}
