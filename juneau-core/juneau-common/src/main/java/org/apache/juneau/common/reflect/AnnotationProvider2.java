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

import static org.apache.juneau.common.utils.AssertionUtils.*;
import static org.apache.juneau.common.utils.ClassUtils.*;
import static org.apache.juneau.common.utils.CollectionUtils.*;
import static org.apache.juneau.common.utils.Utils.*;

import java.lang.annotation.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.stream.*;

import org.apache.juneau.common.collections.*;

/**
 * Enhanced annotation provider for classes that returns {@link AnnotationInfo} objects instead of raw {@link Annotation} objects.
 *
 * <p>
 * This class provides a modern API for retrieving class annotations with the following benefits:
 * <ul>
 * 	<li>Returns {@link AnnotationInfo} wrappers that provide additional methods and type safety
 * 	<li>Supports filtering by annotation type using streams
 * 	<li>Properly handles repeatable annotations
 * 	<li>Searches up the class hierarchy (class → parents → interfaces → package)
 * 	<li>Caches results for performance
 * </ul>
 *
 * <h5 class='section'>See Also:</h5>
 * <ul>
 * 	<li class='jc'>{@link AnnotationProvider}
 * 	<li class='jc'>{@link AnnotationInfo}
 * </ul>
 */
public class AnnotationProvider2 {

	/**
	 * Disable annotation caching.
	 */
	private static final boolean DISABLE_ANNOTATION_CACHING = Boolean.getBoolean("juneau.disableAnnotationCaching");

	/**
	 * Default instance.
	 */
	public static final AnnotationProvider2 INSTANCE = new AnnotationProvider2();

	// @formatter:off
	private final Cache<Class<?>,List<AnnotationInfo<Annotation>>> classAnnotationsInfo = Cache.<Class<?>,List<AnnotationInfo<Annotation>>>create().supplier(this::findClassAnnotations).disableCaching(DISABLE_ANNOTATION_CACHING).build();
	// @formatter:on


	//-----------------------------------------------------------------------------------------------------------------
	// Public API
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Finds all annotations on the specified class.
	 *
	 * <p>
	 * Returns annotations in child-to-parent order.
	 *
	 * @param onClass The class to search on.
	 * @return A list of {@link AnnotationInfo} objects representing annotations on the specified class and its parents.
	 * 	Never <jk>null</jk>.
	 */
	public List<AnnotationInfo<Annotation>> find(Class<?> onClass) {
		assertArgNotNull("onClass", onClass);
		return classAnnotationsInfo.get(onClass);
	}

	/**
	 * Finds all annotations of the specified type on the specified class.
	 *
	 * <p>
	 * Returns annotations in child-to-parent order.
	 *
	 * @param <A> The annotation type to find.
	 * @param type The annotation type to find.
	 * @param onClass The class to search on.
	 * @return A stream of {@link AnnotationInfo} objects representing annotations of the specified type on the specified class and its parents.
	 * 	Never <jk>null</jk>.
	 */
	@SuppressWarnings("unchecked")
	public <A extends Annotation> Stream<AnnotationInfo<A>> find(Class<A> type, Class<?> onClass) {
		assertArgNotNull("type", type);
		assertArgNotNull("onClass", onClass);
		return find(onClass).stream()
			.filter(a -> a.isType(type))
			.map(a -> (AnnotationInfo<A>)a);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Private implementation
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Finds all annotations on the specified class in child-to-parent order.
	 *
	 * <p>
	 * Annotations are appended in the following order:
	 * <ol>
	 * 	<li>On this class.
	 * 	<li>On parent classes ordered child-to-parent.
	 * 	<li>On interfaces ordered child-to-parent.
	 * 	<li>On the package of this class.
	 * </ol>
	 *
	 * @param forClass The class to find annotations on.
	 * @return A list of {@link AnnotationInfo} objects in child-to-parent order.
	 */
	private List<AnnotationInfo<Annotation>> findClassAnnotations(Class<?> forClass) {
		var ci = ClassInfo.of(forClass);
		var list = new ArrayList<AnnotationInfo<Annotation>>();

		// On this class
		findDeclaredAnnotations(list, forClass);

		// On parent classes ordered child-to-parent
		var parents = ci.getParents();
		for (int i = 0; i < parents.size(); i++)
			findDeclaredAnnotations(list, parents.get(i).inner());

		// On interfaces ordered child-to-parent
		var interfaces = ci.getInterfaces();
		for (int i = 0; i < interfaces.size(); i++)
			findDeclaredAnnotations(list, interfaces.get(i).inner());

		// On the package of this class
		var pkg = ci.getPackage();
		if (nn(pkg))
			findDeclaredAnnotations(list, pkg.inner());

		return u(list);
	}

	/**
	 * Finds all declared annotations on the specified class and appends them to the list.
	 *
	 * @param appendTo The list to append to.
	 * @param forClass The class to find declared annotations on.
	 */
	private void findDeclaredAnnotations(List<AnnotationInfo<Annotation>> appendTo, Class<?> forClass) {
		var ci = ClassInfo.of(forClass);
		for (var a : forClass.getDeclaredAnnotations())
			for (var a2 : splitRepeated(a))
				appendTo.add(AnnotationInfo.of(ci, a2));
	}

	/**
	 * Finds all annotations on the specified package and appends them to the list.
	 *
	 * @param appendTo The list to append to.
	 * @param forPackage The package to find annotations on.
	 */
	private void findDeclaredAnnotations(List<AnnotationInfo<Annotation>> appendTo, Package forPackage) {
		var pi = PackageInfo.of(forPackage);
		for (var a : forPackage.getAnnotations())
			for (var a2 : splitRepeated(a))
				appendTo.add(AnnotationInfo.of(pi, a2));
	}
}

