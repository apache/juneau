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
package org.apache.juneau.dto.jsonschema;

import static org.apache.juneau.internal.StringUtils.*;
import java.io.*;
import java.net.*;
import java.util.concurrent.*;

import org.apache.juneau.*;
import org.apache.juneau.json.*;

/**
 * A container for retrieving JSON {@link JsonSchema} objects by URI.
 *
 * <p>
 * Subclasses must implement one of the following methods to load schemas from external sources:
 * <ul class='spaced-list'>
 * 	<li>
 * 		{@link #getReader(URI)} - If schemas should be loaded from readers and automatically parsed.
 * 	<li>
 * 		{@link #load(URI)} - If you want control over construction of {@link JsonSchema} objects.
 * </ul>
 *
 * <h5 class='section'>See Also:</h5>
 * <ul class='doctree'>
 * 	<li class='jp'>{@doc package-summary.html#TOC org.apache.juneau.dto.jsonschema}
 * </ul>
 */
public abstract class JsonSchemaMap extends ConcurrentHashMap<URI,JsonSchema> {

	private static final long serialVersionUID = 1L;

	/**
	 * Return the {@link JsonSchema} object at the specified URI.
	 *
	 * <p>
	 * If this schema object has not been loaded yet, calls {@link #load(URI)}.
	 *
	 * <p>
	 * The value can be of any of the following types: {@link URI}, {@link URL}, {@link String}.
	 * Strings must be valid URIs.
	 *
	 * <p>
	 * URIs defined by {@link UriResolver} can be used for values.
	 *
	 * @param uri The URI of the schema to retrieve.
	 * @return The JsonSchema, or <jk>null</jk> if schema was not located and could not be loaded.
	 */
	@Override /* Map */
	public JsonSchema get(Object uri) {
		URI u = toURI(uri);
		JsonSchema s = super.get(u);
		if (s != null)
			return s;
		synchronized(this) {
			s = load(u);
			if (s != null) {
				// Note:  Can't use add(Schema...) since the ID property may not be set.
				s.setSchemaMap(this);
				put(u, s);
			}
			return s;
		}
	}

	/**
	 * Convenience method for pre-populating this map with the specified schemas.
	 *
	 * <p>
	 * The schemas passed in through this method MUST have their ID properties set.
	 *
	 * @param schemas The set of schemas to add to this map.
	 * @return This object (for method chaining).
	 * @throws RuntimeException If one or more schema objects did not have their ID property set.
	 */
	public JsonSchemaMap add(JsonSchema...schemas) {
		for (JsonSchema schema : schemas) {
			if (schema.getId() == null)
				throw new RuntimeException("Schema with no ID passed to JsonSchemaMap.add(Schema...)");
			put(schema.getId(), schema);
			schema.setSchemaMap(this);
		}
		return this;
	}

	/**
	 * Subclasses must implement either this method or {@link #getReader(URI)} to load the schema with the specified URI.
	 *
	 * <p>
	 * It's up to the implementer to decide where these come from.
	 *
	 * <p>
	 * The default implementation calls {@link #getReader(URI)} and parses the schema document.
	 * If {@link #getReader(URI)} returns <jk>null</jk>, this method returns <jk>null</jk> indicating this is an
	 * unreachable document.
	 *
	 * @param uri The URI to load the schema from.
	 * @return The parsed schema.
	 */
	public JsonSchema load(URI uri) {
		try (Reader r = getReader(uri)) {
			if (r == null)
				return null;
			return JsonParser.DEFAULT.parse(r, JsonSchema.class);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Subclasses must implement either this method or {@link #load(URI)} to load the schema with the specified URI.
	 *
	 * <p>
	 * It's up to the implementer to decide where these come from.
	 *
	 * <p>
	 * The default implementation returns <jk>null</jk>.
	 *
	 * @param uri The URI to connect to and retrieve the contents.
	 * @return The reader from reading the specified URI.
	 */
	public Reader getReader(URI uri) {
		return null;
	}
}
