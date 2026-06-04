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
import java.util.concurrent.atomic.*;

/**
 * Discovery helper for {@link PropertyValidatorFactory} via {@link java.util.ServiceLoader ServiceLoader}.
 *
 * <p>
 * Used by the marshalling side (e.g. {@code MarshalledPropertyPostProcessor}) to obtain a validator factory at
 * {@link BeanMeta} construction time without taking a hard compile-time dependency on the
 * {@code juneau-bean-jsonschema} module.
 *
 * <p>
 * If no factory is registered, {@link #factory()} returns <jk>null</jk> and schema-validation mode becomes a silent
 * no-op.  Callers can override the resolved factory at runtime via {@link #setFactory(PropertyValidatorFactory)} as an
 * escape hatch for multi-classloader / OSGi environments where {@link ServiceLoader} discovery does not see provider
 * jars on the consumer classloader.
 *
 * @see PropertyValidator
 * @see PropertyValidatorFactory
 * @since 10.0.0
 */
public final class PropertyValidators {

	private static final AtomicReference<PropertyValidatorFactory> factory = new AtomicReference<>(resolve());

	private PropertyValidators() {
	}

	/**
	 * Returns the registered factory, or <jk>null</jk> if none is on the classpath.
	 *
	 * @return The factory, or <jk>null</jk>.
	 */
	public static PropertyValidatorFactory factory() {
		return factory.get();
	}

	/**
	 * Overrides the factory resolved via {@link ServiceLoader}.
	 *
	 * <p>
	 * Pass <jk>null</jk> to disable schema validation entirely.
	 *
	 * @param value The factory to use, or <jk>null</jk> to disable.
	 */
	public static void setFactory(PropertyValidatorFactory value) {
		factory.set(value);
	}

	private static PropertyValidatorFactory resolve() {
		var it = ServiceLoader.load(PropertyValidatorFactory.class).iterator();
		return it.hasNext() ? it.next() : null;
	}
}
