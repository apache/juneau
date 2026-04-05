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
package org.apache.juneau.bean.jsonschema;

import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.net.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

@SuppressWarnings({
	"removal" // Tests deprecated getId() / setId() for backward compatibility
})
class JsonSchemaMap_Test extends TestBase {

	@Test void a01_add_withId() {
		var map = new JsonSchemaMap() {};
		var schema = new JsonSchema().setId("http://example.org/schema");
		var result = map.add(schema);
		assertSame(map, result);
		assertNotNull(map.get(URI.create("http://example.org/schema")));
	}

	@Test void a02_add_withoutId_throws() {
		var map = new JsonSchemaMap() {};
		var schema = new JsonSchema(); // no ID set
		assertThrows(IllegalArgumentException.class, () -> map.add(schema));
	}

	@Test void a03_get_existingSchema_returnsDirectly() {
		var map = new JsonSchemaMap() {};
		var uri = URI.create("http://example.org/schema");
		var schema = new JsonSchema().setId(uri.toString());
		map.add(schema);

		// Second get should return the cached entry without going through load()
		var result = map.get(uri);
		assertSame(schema, result);
	}

	@Test void a04_get_notFound_callsLoad_returnsNull() {
		// Default load() implementation calls getReader() which returns null → load() returns null
		var map = new JsonSchemaMap() {};
		var result = map.get(URI.create("http://missing.org/schema"));
		assertNull(result);
	}

	@Test void a05_get_notFound_callsLoad_returnsSchema() {
		var loadedSchema = new JsonSchema().setType(JsonType.OBJECT);
		var targetUri = URI.create("http://dynamic.org/schema");

		var map = new JsonSchemaMap() {
			@Override
			public JsonSchema load(URI uri) {
				if (uri.equals(targetUri))
					return loadedSchema;
				return null;
			}
		};

		var result = map.get(targetUri);
		assertSame(loadedSchema, result);
		// Schema should now be cached
		assertSame(loadedSchema, map.get(targetUri));
	}

	@Test void a06_load_withReader() {
		var schemaJson = "{type:'string'}";
		var targetUri = URI.create("http://example.org/via-reader");

		var map = new JsonSchemaMap() {
			@Override
			public Reader getReader(URI uri) {
				return new StringReader(schemaJson);
			}
		};

		var result = map.load(targetUri);
		assertNotNull(result);
	}

	@Test void a07_getReader_default_returnsNull() {
		var map = new JsonSchemaMap() {};
		assertNull(map.getReader(URI.create("http://example.org/schema")));
	}

	@Test void a08_load_withExceptionInReader_wrapsAsRuntimeException() {
		var map = new JsonSchemaMap() {
			@Override
			public Reader getReader(URI uri) {
				// Return a reader that throws when read
				return new Reader() {
					@Override
					public int read(char[] cbuf, int off, int len) throws java.io.IOException {
						throw new java.io.IOException("Simulated IO error");
					}
					@Override
					public void close() {}
				};
			}
		};
		assertThrows(RuntimeException.class, () -> map.load(URI.create("http://example.org/schema")));
	}
}
