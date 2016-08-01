/***************************************************************************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations under the License.
 ***************************************************************************************************************************/
package org.apache.juneau.dto.jsonschema;

import java.util.*;

/**
 * Represents a list of {@link JsonType} objects.
 * <p>
 * 	Refer to {@link org.apache.juneau.dto.jsonschema} for usage information.
 *
 * @author James Bognar (james.bognar@salesforce.com)
 */
public final class JsonTypeArray extends LinkedList<JsonType> {

	private static final long serialVersionUID = 1L;

	/**
	 * Default constructor.
	 */
	public JsonTypeArray() {}

	/**
	 * Constructor with predefined types to add to this list.
	 *
	 * @param types The list of types to add to the list.
	 */
	public JsonTypeArray(JsonType...types) {
		addAll(types);
	}

	/**
	 * Convenience method for adding one or more {@link JsonType} objects to
	 * 	this array.
	 *
	 * @param types The {@link JsonType} objects to add to this array.
	 * @return This object (for method chaining).
	 */
	public JsonTypeArray addAll(JsonType...types) {
		for (JsonType t : types)
			add(t);
		return this;
	}
}
