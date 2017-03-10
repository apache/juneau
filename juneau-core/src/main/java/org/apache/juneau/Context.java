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

import org.apache.juneau.annotation.*;
import org.apache.juneau.json.*;
import org.apache.juneau.serializer.*;

/**
 * A reusable stateless thread-safe read-only configuration, typically used for creating one-time use {@link Session} objects.
 * <p>
 * Contexts are created through the {@link PropertyStore#getContext(Class)} method.
 * <p>
 * Subclasses MUST implement a constructor method that takes in a {@link PropertyStore} parameter.
 * Besides that restriction, a context object can do anything you desire.  However, it MUST
 * 	be thread-safe and all fields should be declared final to prevent modification.
 * It should NOT be used for storing temporary or state information.
 *
 * @see PropertyStore
 */
public abstract class Context {

	/**
	 * Constructor for this class.
	 * <p>
	 * Subclasses MUST implement the same constructor.
	 *
	 * @param propertyStore The factory that created this config.
	 */
	public Context(PropertyStore propertyStore) {}

	/**
	 * Returns the properties defined on this bean context as a simple map for debugging purposes.
	 *
	 * @return A new map containing the properties defined on this context.
	 */
	@Overrideable
	public ObjectMap asMap() {
		return new ObjectMap();
	}

	@Override /* Object */
	public final String toString() {
		try {
			return asMap().toString(JsonSerializer.DEFAULT_LAX_READABLE);
		} catch (SerializeException e) {
			return e.getLocalizedMessage();
		}
	}
}
