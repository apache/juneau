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

import static org.apache.juneau.common.utils.Utils.*;

import java.lang.annotation.*;
import java.lang.reflect.*;

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
public abstract class AccessibleInfo {

	final AccessibleObject ao;

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
		} catch (Exception ex) {
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
	 * Returns annotations that are <em>present</em> on this element.
	 *
	 * <p>
	 * Same as calling {@link AccessibleObject#getAnnotations()}.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Get all annotations</jc>
	 * 	Annotation[] <jv>annotations</jv> = accessibleInfo.getAnnotations();
	 * </p>
	 *
	 * <p>
	 * <b>Note:</b> This method may be overridden by subclasses to provide enhanced annotation search capabilities.
	 *
	 * @return Annotations present on this element, or an empty array if there are none.
	 * @see AccessibleObject#getAnnotations()
	 */
	public Annotation[] getAnnotations() {
		return ao.getAnnotations();
	}

	/**
	 * Returns annotations that are <em>directly present</em> on this element (not inherited).
	 *
	 * <p>
	 * Same as calling {@link AccessibleObject#getDeclaredAnnotations()}.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Get declared annotations</jc>
	 * 	Annotation[] <jv>annotations</jv> = accessibleInfo.getDeclaredAnnotations();
	 * </p>
	 *
	 * <p>
	 * <b>Note:</b> This method may be overridden by subclasses to provide enhanced annotation search capabilities.
	 *
	 * @return Annotations directly present on this element, or an empty array if there are none.
	 * @see AccessibleObject#getDeclaredAnnotations()
	 */
	public Annotation[] getDeclaredAnnotations() {
		return ao.getDeclaredAnnotations();
	}

	/**
	 * Returns this element's annotations of the specified type (including repeated annotations).
	 *
	 * <p>
	 * Same as calling {@link AccessibleObject#getAnnotationsByType(Class)}.
	 *
	 * <p>
	 * This method handles repeatable annotations by "looking through" container annotations.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Get all @Author annotations (including repeated)</jc>
	 * 	Author[] <jv>authors</jv> = accessibleInfo.getAnnotationsByType(Author.<jk>class</jk>);
	 * </p>
	 *
	 * @param <A> The annotation type.
	 * @param annotationClass The Class object corresponding to the annotation type.
	 * @return All this element's annotations of the specified type, or an empty array if there are none.
	 * @see AccessibleObject#getAnnotationsByType(Class)
	 */
	public <A extends Annotation> A[] getAnnotationsByType(Class<A> annotationClass) {
		return ao.getAnnotationsByType(annotationClass);
	}

	/**
	 * Returns this element's declared annotations of the specified type (including repeated annotations).
	 *
	 * <p>
	 * Same as calling {@link AccessibleObject#getDeclaredAnnotationsByType(Class)}.
	 *
	 * <p>
	 * This method handles repeatable annotations by "looking through" container annotations,
	 * but only examines annotations directly declared on this element (not inherited).
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Get declared @Author annotations (including repeated)</jc>
	 * 	Author[] <jv>authors</jv> = accessibleInfo.getDeclaredAnnotationsByType(Author.<jk>class</jk>);
	 * </p>
	 *
	 * @param <A> The annotation type.
	 * @param annotationClass The Class object corresponding to the annotation type.
	 * @return All this element's declared annotations of the specified type, or an empty array if there are none.
	 * @see AccessibleObject#getDeclaredAnnotationsByType(Class)
	 */
	public <A extends Annotation> A[] getDeclaredAnnotationsByType(Class<A> annotationClass) {
		return ao.getDeclaredAnnotationsByType(annotationClass);
	}
}

