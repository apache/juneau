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

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.commons.bean.*;
import org.apache.juneau.commons.httppart.*;
import org.apache.juneau.marshall.collections.*;
import org.junit.jupiter.api.*;

/**
 * Tests {@link JsonSchemaPropertyValidatorFactory} — the ServiceLoader-discovered bridge between
 * {@link PropertyValidatorFactory} and the {@link JsonSchemaValidator} implementation.
 */
class JsonSchemaPropertyValidatorFactory_Test extends TestBase {

	private static final JsonSchemaPropertyValidatorFactory FACTORY = new JsonSchemaPropertyValidatorFactory();

	@Test void a01_serviceLoader_discoversFactory() {
		var f = PropertyValidators.factory();
		assertNotNull(f);
		assertInstanceOf(JsonSchemaPropertyValidatorFactory.class, f);
	}

	@Test void a02_create_nullMap_returnsNull() {
		assertNull(FACTORY.create(null, String.class));
	}

	@Test void a03_create_emptyMap_returnsNull() {
		assertNull(FACTORY.create(new HashMap<>(), String.class));
	}

	@Test void a04_create_acceptsJsonMap() {
		var jm = new JsonMap().append("minLength", 2);
		var v = FACTORY.create(jm, String.class);
		assertNotNull(v);
		assertInstanceOf(JsonSchemaValidator.class, v);
		assertThrows(SchemaValidationException.class, () -> v.validate("a"));
		v.validate("ok");
	}

	@Test void a05_create_acceptsPlainMap() {
		// Triggers the `schemaMap instanceof JsonMap` == false branch — non-JsonMap input is wrapped.
		Map<String,Object> m = new LinkedHashMap<>();
		m.put("maxLength", 3);
		var v = FACTORY.create(m, String.class);
		assertNotNull(v);
		v.validate("ok");
		assertThrows(SchemaValidationException.class, () -> v.validate("toolong"));
	}

	@Test void a06_create_acceptsNullPropertyType() {
		var v = FACTORY.create(new JsonMap().append("minLength", 1), null);
		assertNotNull(v);
		v.validate("x");
	}
}
