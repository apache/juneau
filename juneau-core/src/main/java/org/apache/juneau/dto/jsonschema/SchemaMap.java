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

import java.io.*;
import java.net.*;
import java.util.concurrent.*;

import org.apache.juneau.json.*;

/**
 * A container for retrieving JSON {@link Schema} objects by URI.
 * <p>
 * 	Subclasses must implement one of the following methods to load schemas from external sources:
 * <ul class='spaced-list'>
 * 	<li>{@link #getReader(URI)} - If schemas should be loaded from readers and automatically parsed.
 * 	<li>{@link #load(URI)} - If you want control over construction of {@link Schema} objects.
 * </ul>
 *
 * @author James Bognar (james.bognar@salesforce.com)
 */
public abstract class SchemaMap extends ConcurrentHashMap<URI,Schema> {

	private static final long serialVersionUID = 1L;

	@Override /* Map */
	public Schema get(Object uri) {
		if (uri == null)
			return null;
		return get(URI.create(uri.toString()));
	}

	/**
	 * Return the {@link Schema} object at the specified URI.
	 * If this schema object has not been loaded yet, calls {@link #load(URI)}.
	 *
	 * @param uri The URI of the schema to retrieve.
	 * @return The Schema, or <jk>null</jk> if schema was not located and could not be loaded.
	 */
	public Schema get(URI uri) {
		Schema s = super.get(uri);
		if (s != null)
			return s;
		synchronized(this) {
			s = load(uri);
			if (s != null) {
				// Note:  Can't use add(Schema...) since the ID property may not be set.
				s.setSchemaMap(this);
				put(uri, s);
			}
			return s;
		}
	}

	/**
	 * Convenience method for prepopulating this map with the specified schemas.
	 * <p>
	 * The schemas passed in through this method MUST have their ID properties set.
	 *
	 * @param schemas The set of schemas to add to this map.
	 * @return This object (for method chaining).
	 * @throws RuntimeException If one or more schema objects did not have their ID property set.
	 */
	public SchemaMap add(Schema...schemas) {
		for (Schema schema : schemas) {
			if (schema.getId() == null)
				throw new RuntimeException("Schema with no ID passed to SchemaMap.add(Schema...)");
			put(schema.getId(), schema);
			schema.setSchemaMap(this);
		}
		return this;
	}

	/**
	 * Subclasses must implement either this method or {@link #getReader(URI)} to load the schema with the specified URI.
	 * It's up to the implementer to decide where these come from.
	 * <p>
	 * The default implementation calls {@link #getReader(URI)} and parses the schema document.
	 * If {@link #getReader(URI)} returns <jk>null</jk>, this method returns <jk>null</jk> indicating this is an unreachable document.
	 *
	 * @param uri The URI to load the schema from.
	 * @return The parsed schema.
	 */
	public Schema load(URI uri) {
		Reader r = getReader(uri);
		if (r == null)
			return null;
		try {
			return JsonParser.DEFAULT.parse(r, Schema.class);
		} catch (Exception e) {
			throw new RuntimeException(e);
		} finally {
			try {
				r.close();
			} catch (IOException e) {
				// Ignore
			}
		}
	}

	/**
	 * Subclasses must implement either this method or {@link #load(URI)} to load the schema with the specified URI.
	 * It's up to the implementer to decide where these come from.
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
