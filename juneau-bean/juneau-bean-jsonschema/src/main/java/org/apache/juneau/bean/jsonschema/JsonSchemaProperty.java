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

/**
 * Represents a JSON property in the JSON-Schema core specification.
 *
 * @param <SELF> The self type for fluent setters.
 */
@SuppressWarnings({
	"java:S119" // 'SELF' (CRTP self-type) is intentional and clearer than a single-letter name.
})
public class JsonSchemaProperty<SELF extends JsonSchemaProperty<SELF>> extends JsonSchema<SELF> {

	/**
	 * Default constructor.
	 */
	public JsonSchemaProperty() {}

	/**
	 * Convenience constructor.
	 *
	 * @param name The name of this property.  Can be <jk>null</jk>.
	 */
	public JsonSchemaProperty(String name) {
		setName(name);
	}

	/**
	 * Convenience constructor.
	 *
	 * @param name The name of this property.  Can be <jk>null</jk>.
	 * @param type The JSON type of this property.  Can be <jk>null</jk>.
	 */
	public JsonSchemaProperty(String name, JsonType type) {
		setName(name);
		setType(type);
	}
}
