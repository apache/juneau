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
package org.apache.juneau.common.reflect;

import static org.apache.juneau.common.utils.CollectionUtils.*;
import static org.apache.juneau.common.utils.Utils.*;

import java.lang.annotation.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.function.*;

import org.apache.juneau.common.collections.*;

/**
 * Base class for reflection info classes that wrap {@link AccessibleObject}.
 *
 * <p>
 * This class provides common functionality for {@link FieldInfo}, {@link MethodInfo}, and {@link ConstructorInfo}
 * that mirrors the {@link AccessibleObject} API.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * </ul>
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public abstract class AccessibleInfo {

	AccessibleObject ao;  // Effectively final

	// Cached annotation lists
	private final Supplier<List<Annotation>> annotations = memoize(() -> u(l(ao.getAnnotations())));
	private final Supplier<List<Annotation>> declaredAnnotations = memoize(() -> u(l(ao.getDeclaredAnnotations())));

	// Cache for parameterized annotation queries
	private final Cache annotationsByType = Cache.of(Class.class, List.class).supplier(k -> u(l(ao.getAnnotationsByType(k)))).build();
	private final Cache declaredAnnotationsByType = Cache.of(Class.class, List.class) .supplier(k -> u(l(ao.getDeclaredAnnotationsByType(k)))).build();

	/**
	 * Constructor.
	 *
	 * @param ao The {@link AccessibleObject} being wrapped.
	 */
	protected AccessibleInfo(AccessibleObject ao) {
		this.ao = ao;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Accessibility
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Attempts to call <code>x.setAccessible(<jk>true</jk>)</code> and quietly ignores security exceptions.
	 *
	 * @return <jk>true</jk> if call was successful.
	 */
	public boolean setAccessible() {
		try {
			if (nn(ao))
				ao.setAccessible(true);
			return true;
		} catch (@SuppressWarnings("unused") SecurityException e) {
			return false;
		}
	}

	/**
	 * Returns <jk>true</jk> if this object is accessible.
	 *
	 * <p>
	 * This method was added in Java 9. For earlier versions, this always returns <jk>false</jk>.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Check if accessible without security checks</jc>
	 * 	<jk>if</jk> (!accessibleInfo.isAccessible()) {
	 * 		accessibleInfo.setAccessible();
	 * 	}
	 * </p>
	 *
	 * @return <jk>true</jk> if this object is accessible, <jk>false</jk> otherwise or if not supported.
	 */
	public boolean isAccessible() {
		try {
			return (boolean) AccessibleObject.class.getMethod("isAccessible").invoke(ao);
		} catch (@SuppressWarnings("unused") Exception ex) {
			return false;
		}
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Annotations
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Returns this element's annotation for the specified type if such an annotation is <em>present</em>, else <jk>null</jk>.
	 *
	 * <p>
	 * Same as calling {@link AccessibleObject#getAnnotation(Class)}.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Get @Deprecated annotation</jc>
	 * 	Deprecated <jv>d</jv> = accessibleInfo.getAnnotation(Deprecated.<jk>class</jk>);
	 * </p>
	 *
	 * <p>
	 * <b>Note:</b> This method may be overridden by subclasses to provide enhanced annotation search capabilities
	 * (e.g., searching through class hierarchies or interfaces).
	 *
	 * @param <A> The annotation type.
	 * @param annotationClass The Class object corresponding to the annotation type.
	 * @return This element's annotation for the specified annotation type if present, else <jk>null</jk>.
	 * @see AccessibleObject#getAnnotation(Class)
	 */
	public <A extends Annotation> A getAnnotation(Class<A> annotationClass) {
		return ao.getAnnotation(annotationClass);
	}

	/**
	 * Returns <jk>true</jk> if this element has the specified annotation.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Check if method has @Deprecated annotation</jc>
	 * 	<jk>if</jk> (methodInfo.hasAnnotation(Deprecated.<jk>class</jk>)) {
	 * 		<jc>// Handle deprecated method</jc>
	 * 	}
	 * </p>
	 *
	 * @param <A> The annotation type.
	 * @param type The annotation to check for.
	 * @return <jk>true</jk> if this element has the specified annotation.
	 */
	public <A extends Annotation> boolean hasAnnotation(Class<A> type) {
		return ao.isAnnotationPresent(type);
	}

	/**
	 * Returns <jk>true</jk> if this element doesn't have the specified annotation.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Check if method is not deprecated</jc>
	 * 	<jk>if</jk> (methodInfo.hasNoAnnotation(Deprecated.<jk>class</jk>)) {
	 * 		<jc>// Handle non-deprecated method</jc>
	 * 	}
	 * </p>
	 *
	 * @param <A> The annotation type.
	 * @param type The annotation to check for.
	 * @return <jk>true</jk> if this element doesn't have the specified annotation.
	 */
	public <A extends Annotation> boolean hasNoAnnotation(Class<A> type) {
		return ! hasAnnotation(type);
	}

	/**
	 * Returns annotations that are <em>present</em> on this element.
	 *
	 * <p>
	 * Returns a cached, unmodifiable list of annotations.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Get all annotations</jc>
	 * 	List&lt;Annotation&gt; <jv>annotations</jv> = accessibleInfo.getAnnotations();
	 * </p>
	 *
	 * <p>
	 * <b>Note:</b> This method may be overridden by subclasses to provide enhanced annotation search capabilities.
	 *
	 * @return An unmodifiable list of annotations present on this element, or an empty list if there are none.
	 */
	public List<Annotation> getAnnotations() {
		return annotations.get();
	}

	/**
	 * Returns annotations that are <em>directly present</em> on this element (not inherited).
	 *
	 * <p>
	 * Returns a cached, unmodifiable list of annotations.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Get declared annotations</jc>
	 * 	List&lt;Annotation&gt; <jv>annotations</jv> = accessibleInfo.getDeclaredAnnotations();
	 * </p>
	 *
	 * <p>
	 * <b>Note:</b> This method may be overridden by subclasses to provide enhanced annotation search capabilities.
	 *
	 * @return An unmodifiable list of annotations directly present on this element, or an empty list if there are none.
	 */
	public List<Annotation> getDeclaredAnnotations() {
		return declaredAnnotations.get();
	}

	/**
	 * Returns this element's annotations of the specified type (including repeated annotations).
	 *
	 * <p>
	 * Returns a cached, unmodifiable list of annotations.
	 *
	 * <p>
	 * This method handles repeatable annotations by "looking through" container annotations.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Get all @Author annotations (including repeated)</jc>
	 * 	List&lt;Author&gt; <jv>authors</jv> = accessibleInfo.getAnnotationsByType(Author.<jk>class</jk>);
	 * </p>
	 *
	 * @param <A> The annotation type.
	 * @param annotationClass The Class object corresponding to the annotation type.
	 * @return An unmodifiable list of all this element's annotations of the specified type, or an empty list if there are none.
	 */
	@SuppressWarnings("unchecked")
	public <A extends Annotation> List<A> getAnnotationsByType(Class<A> annotationClass) {
		return (List<A>) annotationsByType.get(annotationClass);
	}

	/**
	 * Returns this element's declared annotations of the specified type (including repeated annotations).
	 *
	 * <p>
	 * Returns a cached, unmodifiable list of annotations.
	 *
	 * <p>
	 * This method handles repeatable annotations by "looking through" container annotations,
	 * but only examines annotations directly declared on this element (not inherited).
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Get declared @Author annotations (including repeated)</jc>
	 * 	List&lt;Author&gt; <jv>authors</jv> = accessibleInfo.getDeclaredAnnotationsByType(Author.<jk>class</jk>);
	 * </p>
	 *
	 * @param <A> The annotation type.
	 * @param annotationClass The Class object corresponding to the annotation type.
	 * @return An unmodifiable list of all this element's declared annotations of the specified type, or an empty list if there are none.
	 */
	@SuppressWarnings("unchecked")
	public <A extends Annotation> List<A> getDeclaredAnnotationsByType(Class<A> annotationClass) {
		return (List<A>) declaredAnnotationsByType.get(annotationClass);
	}
}