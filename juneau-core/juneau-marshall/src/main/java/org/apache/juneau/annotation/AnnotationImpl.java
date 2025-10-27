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
package org.apache.juneau.annotation;

import static java.util.Arrays.*;
import static org.apache.juneau.collections.JsonMap.*;
import static org.apache.juneau.common.utils.Utils.*;

import java.lang.annotation.*;
import java.lang.reflect.*;
import java.util.*;

import org.apache.juneau.collections.*;
import org.apache.juneau.common.utils.*;

/**
 * A concrete implementation of an annotation.
 *
 * <p>
 * Follows the standard Java conventions for equality and hashcode calculation for annotations.
 * Equivalent annotations defined programmatically and declaratively should match for equality and hashcode calculation.
 *
 * <p>
 * For performance reasons, the hashcode is calculated one time and cached at the end of object creation.
 * Constructors must call the {@link #postConstruct()} method after all fields have been set to trigger this calculation.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * </ul>
 */
public class AnnotationImpl implements Annotation {

	private final Class<? extends Annotation> annotationType;
	private final String[] description;
	private int hashCode = -1;

	/**
	 * Constructor.
	 *
	 * @param b The builder used to instantiate the fields of this class.
	 */
	public AnnotationImpl(AnnotationBuilder<?> b) {
		this.annotationType = b.annotationType;
		this.description = ArrayUtils.copyOf(b.description);
	}

	/**
	 * Implements the {@link Annotation#annotationType()} method for child classes.
	 *
	 * @return This class.
	 */
	@Override /* Overridden from Annotation */
	public Class<? extends Annotation> annotationType() {
		return annotationType;
	}

	/**
	 * Returns the annotation description.
	 *
	 * @return the annotation description.
	 * @since 9.2.0
	 */
	public String[] description() {
		return description;
	}

	@Override /* Overridden from Object */
	public boolean equals(Object o) {
		if (! annotationType.isInstance(o))
			return false;
		return eq(this, (Annotation)o);
	}

	@Override /* Overridden from Object */
	public int hashCode() {
		if (hashCode == -1)
			throw new IllegalArgumentException("Programming error. postConstruct() was never called on annotation.");
		return hashCode;
	}

	/**
	 * Returns this annotation as a map of key/value pairs.
	 *
	 * <p>
	 * Useful for debugging.
	 *
	 * @return This annotation as a map of key/value pairs.
	 */
	public JsonMap toMap() {
		JsonMap m = create();
		// @formatter:off
		stream(annotationType().getDeclaredMethods())
			.filter(x->x.getParameterCount() == 0 && x.getDeclaringClass().isAnnotation())
			.sorted(Comparator.comparing(Method::getName))
			.forEach(x -> m.append(x.getName(), safeSupplier(()->x.invoke(this))));
		// @formatter:on
		return m;
	}

	@Override /* Overridden from Object */
	public String toString() {
		return toMap().asString();
	}

	/**
	 * This method must be called at the end of initialization to calculate the hashCode one time.
	 */
	protected void postConstruct() {
		this.hashCode = AnnotationUtils.hash(this);
	}
}