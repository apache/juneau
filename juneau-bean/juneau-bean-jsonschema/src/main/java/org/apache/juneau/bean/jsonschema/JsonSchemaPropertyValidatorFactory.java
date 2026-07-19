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

import static org.apache.juneau.commons.utils.Shorts.*;

import java.util.*;

import org.apache.juneau.commons.bean.*;
import org.apache.juneau.marshall.collections.*;

/**
 * {@link PropertyValidatorFactory} implementation backed by the typed {@link JsonSchema} bean.
 *
 * <p>
 * This factory is registered as a {@link ServiceLoader ServiceLoader} provider for
 * {@link PropertyValidatorFactory} via {@code META-INF/services/org.apache.juneau.commons.bean.PropertyValidatorFactory}.
 * Marshalling-side code discovers it through {@link PropertyValidators#factory()} at runtime.
 *
 * <p>
 * The factory converts the supplied JSON-Schema-shaped map into a {@link JsonSchema} bean via
 * {@link JsonSchemaBeanGenerator#toBean(JsonMap)} and wraps it in a {@link JsonSchemaValidator}.
 *
 * @see JsonSchemaValidator
 * @see PropertyValidatorFactory
 * @since 10.0.0
 */
public final class JsonSchemaPropertyValidatorFactory implements PropertyValidatorFactory {

	/** Public no-arg constructor required by {@link ServiceLoader ServiceLoader}. */
	public JsonSchemaPropertyValidatorFactory() {
		/* intentionally empty — required public no-arg constructor for ServiceLoader */
	}

	@Override
	public PropertyValidator create(Map<String,Object> schemaMap, Class<?> propertyType) {
		if (ie(schemaMap))
			return null;
		var jm = (schemaMap instanceof JsonMap schemaMap2) ? schemaMap2 : new JsonMap(schemaMap);
		var schema = JsonSchemaBeanGenerator.toBean(jm);
		return JsonSchemaValidator.of(schema);
	}
}
