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

import java.util.*;

/**
 * Builds {@link PropertyValidator} instances from a JSON-Schema-shaped map of constraints.
 *
 * <p>
 * The factory is resolved at runtime via {@link ServiceLoader ServiceLoader} through
 * {@link PropertyValidators#factory()}.  If no factory is on the classpath (i.e. {@code juneau-bean-jsonschema} is
 * not present), schema-validation mode silently becomes a no-op.
 *
 * <p>
 * The marshalling side calls the factory once per bean property at {@link BeanMeta} construction time and installs
 * the resulting validator into the property's read/write transform slots.
 *
 * <h5 class='topic'>Thread safety</h5>
 * Factory implementations must be safe to invoke from multiple threads concurrently.
 *
 * @see PropertyValidator
 * @see PropertyValidators
 * @since 10.0.0
 */
@FunctionalInterface
public interface PropertyValidatorFactory {

	/**
	 * Builds a validator from a JSON-Schema-shaped map of constraints.
	 *
	 * @param schemaMap
	 * 	A map mirroring the JSON Schema constraint shape (the same shape produced by
	 * 	{@code SchemaAnnotation.asMap()}).  May be empty, in which case implementations should return <jk>null</jk>.
	 * @param propertyType
	 * 	The Java type of the property being validated.  May be used by the factory to choose narrower numeric or
	 * 	collection validators.  May be <jk>null</jk>.
	 * @return A validator, or <jk>null</jk> if the map carries no actionable constraints.
	 */
	PropertyValidator create(Map<String,Object> schemaMap, Class<?> propertyType);
}
