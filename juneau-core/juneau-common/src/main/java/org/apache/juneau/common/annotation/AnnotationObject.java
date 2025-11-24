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
import static org.apache.juneau.common.utils.AssertionUtils.*;
import static org.apache.juneau.common.utils.Utils.*;

import java.lang.annotation.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.function.*;

import org.apache.juneau.common.utils.*;

/**
 * A concrete implementation of a Java annotation that can be created programmatically at runtime.
 *
 * <p>
 * This class provides a base for creating annotation instances without requiring them to be declared on
 * program elements at compile-time. It allows annotations to be constructed using a builder pattern and
 * follows all standard Java annotation semantics for equality, hashcode, and string representation.
 *
 * <h5 class='section'>Overview:</h5>
 * <p>
 * Java annotations are typically declared statically on classes, methods, fields, etc. at compile-time:
 * <p class='bjava'>
 * 	<ja>@Bean</ja>(sort=<jk>true</jk>)
 * 	<jk>public class</jk> MyClass {...}
 * </p>
 *
 * <p>
 * This class allows you to create those same annotations programmatically:
 * <p class='bjava'>
 * 	Bean <jv>annotation</jv> = BeanAnnotation
 * 		.<jsm>create</jsm>()
 * 		.sort(<jk>true</jk>)
 * 		.build();
 * </p>
 *
 * <h5 class='section'>Equality and Hashcode:</h5>
 * <p>
 * Follows the standard Java conventions for annotation equality and hashcode calculation as defined in
 * {@link Annotation#equals(Object)} and {@link Annotation#hashCode()}. This ensures that programmatically-created
 * annotations are equivalent to compile-time declared annotations if they have the same type and properties.
 *
 * <p class='bjava'>
 * 	<jc>// These two annotations are equal:</jc>
 * 	<ja>@Bean</ja>(sort=<jk>true</jk>)
 * 	<jk>class</jk> MyClass {}
 *
 * 	Bean <jv>declared</jv> = MyClass.<jk>class</jk>.getAnnotation(Bean.<jk>class</jk>);
 * 	Bean <jv>programmatic</jv> = BeanAnnotation.<jsm>create</jsm>().sort(<jk>true</jk>).build();
 *
 * 	<jsm>assertEquals</jsm>(<jv>declared</jv>, <jv>programmatic</jv>);  <jc>// true</jc>
 * 	<jsm>assertEquals</jsm>(<jv>declared</jv>.hashCode(), <jv>programmatic</jv>.hashCode());  <jc>// true</jc>
 * </p>
 *
 * <h5 class='section'>Hashcode Caching:</h5>
 * <p>
 * For performance reasons, the hashcode is calculated once and cached on first access.
 * The hash is computed lazily when {@link #hashCode()} is first called and then stored for subsequent calls.
 *
 * <p class='bjava'>
 * 	<jk>public</jk> MyAnnotation(Builder <jv>builder</jv>) {
 * 		<jk>super</jk>(<jv>builder</jv>);
 * 		<jk>this</jk>.<jf>myField</jf> = <jv>builder</jv>.<jf>myField</jf>;
 * 	}
 * </p>
 *
 * <h5 class='section'>Builder Pattern:</h5>
 * <p>
 * Subclasses should provide a nested {@link Builder} class that extends {@link AnnotationObject.Builder}
 * to construct instances using a fluent builder pattern. The builder should:
 * <ul class='spaced-list'>
 * 	<li>Provide setter methods for each annotation property
 * 	<li>Return <c>this</c> (or the builder type) from each setter for method chaining
 * 	<li>Provide a {@code build()} method that constructs the final annotation object
 * </ul>
 *
 * <h5 class='section'>Example Implementation:</h5>
 * <p class='bjava'>
 * 	<jk>public class</jk> MyAnnotationObject <jk>extends</jk> AnnotationObject <jk>implements</jk> MyAnnotation {
 *
 * 		<jk>private final</jk> String <jf>value</jf>;
 *
 * 		<jk>public static class</jk> Builder <jk>extends</jk> AnnotationObject.Builder {
 * 			String <jf>value</jf> = <js>""</js>;
 *
 * 			<jk>public</jk> Builder() {
 * 				<jk>super</jk>(MyAnnotation.<jk>class</jk>);
 * 			}
 *
 * 			<jk>public</jk> Builder value(String <jv>value</jv>) {
 * 				<jk>this</jk>.<jf>value</jf> = <jv>value</jv>;
 * 				<jk>return this</jk>;
 * 			}
 *
 * 			<jk>public</jk> MyAnnotation build() {
 * 				<jk>return new</jk> MyAnnotationObject(<jk>this</jk>);
 * 			}
 * 		}
 *
 * 		<jk>public</jk> MyAnnotationObject(Builder <jv>builder</jv>) {
 * 			<jk>super</jk>(<jv>builder</jv>);
 * 			<jk>this</jk>.<jf>value</jf> = <jv>builder</jv>.<jf>value</jf>;
 * 		}
 *
 * 		<ja>@Override</ja>
 * 		<jk>public</jk> String value() {
 * 			<jk>return</jk> <jf>value</jf>;
 * 		}
 * 	}
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='jc'>{@link AppliedAnnotationObject} - For annotations with dynamic targeting support
 * 	<li class='link'><a class="doclink" href="../../../../../overview-summary.html#juneau-common.Annotations">Overview &gt; juneau-common &gt; Annotations</a>
 * </ul>
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
			assertArgNotNull("annotationType", annotationType);
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
	private Supplier<Integer> hashCode = memoize(() -> AnnotationUtils.hash(this));

	/**
	 * Constructor.
	 *
	 * @param b The builder used to instantiate the fields of this class.
	 */
	public AnnotationObject(Builder b) {
		assertArgNotNull("b", b);
		annotationType = b.getAnnotationType();
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
		return o instanceof Annotation o2 && annotationType.isInstance(o) && eq(this, o2);
	}

	@Override /* Overridden from Object */
	public int hashCode() {
		return hashCode.get();
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
			// Note: isAnnotation() check is defensive code. For properly-formed AnnotationObject instances,
			// annotationType() always returns an annotation interface, so this condition is always true.
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
}