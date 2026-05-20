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
package org.apache.juneau.bean.jsonpatch;

import java.util.*;

/**
 * Represents a JSON Patch document per
 * <a href="https://datatracker.ietf.org/doc/html/rfc6902">RFC 6902</a>.
 *
 * <p>
 * A JSON Patch document is a JSON array of operation objects; this type therefore extends
 * {@link LinkedList} of {@link JsonPatchOperation}. Mirrors the top-level-array pattern used by
 * {@code JsonSchemaArray} - no {@code @Json(wrapperAttr=...)} wrapping.
 *
 * @serial exclude
 */
public class JsonPatch extends LinkedList<JsonPatchOperation> {

	private static final long serialVersionUID = 1L;

	/**
	 * Default constructor.
	 */
	public JsonPatch() {}

	/**
	 * Constructor with predefined operations to add to this patch.
	 *
	 * @param value The operations in this patch.
	 */
	public JsonPatch(JsonPatchOperation...value) {
		append(value);
	}

	/**
	 * Convenience method for appending one or more {@link JsonPatchOperation} objects to this patch.
	 *
	 * @param value The operations to append.
	 * @return This object.
	 */
	public JsonPatch append(JsonPatchOperation...value) {
		Collections.addAll(this, value);
		return this;
	}
}
