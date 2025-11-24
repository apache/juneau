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
package org.apache.juneau.common.annotation;

import static java.util.Arrays.*;
import static org.apache.juneau.common.utils.ThrowableUtils.*;
import static org.apache.juneau.common.utils.Utils.*;

import java.lang.annotation.*;
import java.lang.reflect.*;
import java.util.*;

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
 */
public class AnnotationObject implements Annotation {

	//-----------------------------------------------------------------------------------------------------------------
	// Static
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Builder for {@link AnnotationObject} objects.
	 */
	public static class Builder {

		private Class<? extends Annotation> annotationType;

		/**
		 * Constructor.
		 *
		 * @param annotationType The annotation type of the annotation implementation class.
		 */
		public Builder(Class<? extends Annotation> annotationType) {
			this.annotationType = annotationType;
		}

		/**
		 * Returns the annotation type being built.
		 *
		 * @return The annotation type being built.
		 */
		public Class<? extends Annotation> getAnnotationType() { return annotationType; }
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Instance
	//-----------------------------------------------------------------------------------------------------------------

	private final Class<? extends Annotation> annotationType;
	private int hashCode = -1;

	/**
	 * Constructor.
	 *
	 * @param b The builder used to instantiate the fields of this class.
	 */
	public AnnotationObject(Builder b) {
		this.annotationType = b.getAnnotationType();
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

	@Override /* Overridden from Object */
	public boolean equals(Object o) {
		if (! annotationType.isInstance(o))
			return false;
		return eq(this, (Annotation)o);
	}

	@Override /* Overridden from Object */
	public int hashCode() {
		if (hashCode == -1)
			throw illegalArg("Programming error. postConstruct() was never called on annotation.");
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
	public Map<String,Object> toMap() {
		var m = new LinkedHashMap<String,Object>();
		// @formatter:off
		stream(annotationType().getDeclaredMethods())
			.filter(x->x.getParameterCount() == 0 && x.getDeclaringClass().isAnnotation())
			.sorted(Comparator.comparing(Method::getName))
			.forEach(x -> m.put(x.getName(), safeSupplier(()->x.invoke(this))));
		// @formatter:on
		return m;
	}

	@Override /* Overridden from Object */
	public String toString() {
		return toMap().toString();
	}

	/**
	 * This method must be called at the end of initialization to calculate the hashCode one time.
	 */
	protected void postConstruct() {
		hashCode = AnnotationUtils.hash(this);
	}
}