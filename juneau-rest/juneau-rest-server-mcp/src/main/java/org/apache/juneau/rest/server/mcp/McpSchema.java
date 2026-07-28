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
package org.apache.juneau.rest.server.mcp;

import org.apache.juneau.marshall.collections.*;

/**
 * Revision-neutral carrier for a tool's input schema.
 *
 * <p>
 * Deliberately a carrier, not a structured bean: the core has no opinion about which JSON Schema
 * dialect or keyword set a revision supports. Each revision's adapter is responsible for validating
 * that the carried map is expressible in its own wire schema type, and for rejecting configurations
 * that are not.
 *
 * <p>
 * The supplied map is <em>not</em> copied; callers must not mutate a map after handing it over.
 */
public final class McpSchema {

	private final JsonMap raw;

	private McpSchema(JsonMap raw) {
		this.raw = raw;
	}

	/**
	 * Creates a schema carrier around a raw JSON Schema object.
	 *
	 * @param raw The schema as a JSON object. Can be <jk>null</jk>, which yields an empty schema.
	 * @return A new carrier. Never <jk>null</jk>.
	 */
	public static McpSchema of(JsonMap raw) {
		return new McpSchema(raw == null ? new JsonMap() : raw);
	}

	/**
	 * The carried schema.
	 *
	 * @return The schema map. Never <jk>null</jk>; not a copy.
	 */
	public JsonMap toJsonMap() {
		return raw;
	}
}
