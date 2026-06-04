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
package org.apache.juneau.commons.bean;

import org.apache.juneau.commons.httppart.*;

/**
 * Validates a bean-property value against a schema.
 *
 * <p>
 * Implementations of this interface are built by a {@link PropertyValidatorFactory} and installed on the marshalling
 * side (via {@link BeanPropertyMeta.Builder#readTransform readTransform} / {@link BeanPropertyMeta.Builder#writeTransform writeTransform})
 * once per bean property at {@link BeanMeta} construction time.  The instance is then invoked from
 * {@link BeanPropertyMeta#get} / {@link BeanPropertyMeta#set} at runtime when schema validation is enabled.
 *
 * <p>
 * The commons-side SPI does not depend on any marshalling-side or {@code juneau-bean-jsonschema} types.  The concrete
 * implementation that ships with Juneau is backed by the typed {@code JsonSchema} bean and lives in
 * {@code juneau-bean-jsonschema}; it is discovered at runtime via {@link java.util.ServiceLoader ServiceLoader}.
 *
 * <h5 class='topic'>Thread safety</h5>
 * Implementations must be safe to invoke from multiple threads concurrently, since a single validator is shared by all
 * marshalling sessions for a given {@link BeanMeta}.
 *
 * @see PropertyValidatorFactory
 * @see PropertyValidators
 * @since 10.0.0
 */
@FunctionalInterface
public interface PropertyValidator {

	/**
	 * Validates the given value against the schema this validator was built from.
	 *
	 * @param value The value to validate.  May be <jk>null</jk>.
	 * @throws SchemaValidationException If the value violates the schema.
	 */
	void validate(Object value) throws SchemaValidationException;
}
