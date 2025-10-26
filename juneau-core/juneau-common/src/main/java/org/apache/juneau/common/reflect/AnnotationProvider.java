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

import java.lang.annotation.*;
import java.lang.reflect.*;
import java.util.function.*;

import org.apache.juneau.common.collections.*;
import org.apache.juneau.common.utils.*;

/**
 * Interface that provides the ability to look up annotations on classes/methods/constructors/fields.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * </ul>
 */
public interface AnnotationProvider {

	/**
	 * Disable annotation caching.
	 */
	boolean DISABLE_ANNOTATION_CACHING = Boolean.getBoolean("juneau.disableAnnotationCaching");

	/**
	 * Default metadata provider.
	 */
	@SuppressWarnings("unchecked")
	AnnotationProvider DEFAULT = new AnnotationProvider() {

		// @formatter:off
		private final Concurrent2KeyHashMap<Class<?>,Class<? extends Annotation>,Annotation[]> classAnnotationCache = new Concurrent2KeyHashMap<>(DISABLE_ANNOTATION_CACHING, Class::getAnnotationsByType);
		private final Concurrent2KeyHashMap<Class<?>,Class<? extends Annotation>,Annotation[]> declaredClassAnnotationCache = new Concurrent2KeyHashMap<>(DISABLE_ANNOTATION_CACHING, Class::getDeclaredAnnotationsByType);
		private final Concurrent2KeyHashMap<Method,Class<? extends Annotation>,Annotation[]> methodAnnotationCache = new Concurrent2KeyHashMap<>(DISABLE_ANNOTATION_CACHING, Method::getAnnotationsByType);
		private final Concurrent2KeyHashMap<Field,Class<? extends Annotation>,Annotation[]> fieldAnnotationCache = new Concurrent2KeyHashMap<>(DISABLE_ANNOTATION_CACHING, Field::getAnnotationsByType);
		private final Concurrent2KeyHashMap<Constructor<?>,Class<? extends Annotation>,Annotation[]> constructorAnnotationCache = new Concurrent2KeyHashMap<>(DISABLE_ANNOTATION_CACHING, Constructor::getAnnotationsByType);
		// @formatter:on

		@Override /* Overridden from MetaProvider */
		public <A extends Annotation> A firstAnnotation(Class<A> type, Class<?> onClass, Predicate<A> filter) {
			if (type != null && onClass != null)
				for (A a : annotations(type, onClass))
					if (PredicateUtils.test(filter, a))
						return a;
			return null;
		}

		@Override /* Overridden from MetaProvider */
		public <A extends Annotation> A firstAnnotation(Class<A> type, Constructor<?> onConstructor, Predicate<A> filter) {
			if (type != null && onConstructor != null)
				for (A a : annotations(type, onConstructor))
					if (PredicateUtils.test(filter, a))
						return a;
			return null;
		}

		@Override /* Overridden from MetaProvider */
		public <A extends Annotation> A firstAnnotation(Class<A> type, Field onField, Predicate<A> filter) {
			if (type != null && onField != null)
				for (A a : annotations(type, onField))
					if (PredicateUtils.test(filter, a))
						return a;
			return null;
		}

		@Override /* Overridden from MetaProvider */
		public <A extends Annotation> A firstAnnotation(Class<A> type, Method onMethod, Predicate<A> filter) {
			if (type != null && onMethod != null)
				for (A a : annotations(type, onMethod))
					if (PredicateUtils.test(filter, a))
						return a;
			return null;
		}

		@Override /* Overridden from MetaProvider */
		public <A extends Annotation> A firstDeclaredAnnotation(Class<A> type, Class<?> onClass, Predicate<A> filter) {
			if (type != null && onClass != null)
				for (A a : declaredAnnotations(type, onClass))
					if (PredicateUtils.test(filter, a))
						return a;
			return null;
		}

		@Override /* Overridden from MetaProvider */
		public <A extends Annotation> void forEachAnnotation(Class<A> type, Class<?> onClass, Predicate<A> filter, Consumer<A> action) {
			if (type != null && onClass != null)
				for (A a : annotations(type, onClass))
					PredicateUtils.consumeIf(filter, action, a);
		}

		@Override /* Overridden from MetaProvider */
		public <A extends Annotation> void forEachAnnotation(Class<A> type, Constructor<?> onConstructor, Predicate<A> filter, Consumer<A> action) {
			if (type != null && onConstructor != null)
				for (A a : annotations(type, onConstructor))
					PredicateUtils.consumeIf(filter, action, a);
		}

		@Override /* Overridden from MetaProvider */
		public <A extends Annotation> void forEachAnnotation(Class<A> type, Field onField, Predicate<A> filter, Consumer<A> action) {
			if (type != null && onField != null)
				for (A a : annotations(type, onField))
					PredicateUtils.consumeIf(filter, action, a);
		}

		@Override /* Overridden from MetaProvider */
		public <A extends Annotation> void forEachAnnotation(Class<A> type, Method onMethod, Predicate<A> filter, Consumer<A> action) {
			if (type != null && onMethod != null)
				for (A a : annotations(type, onMethod))
					PredicateUtils.consumeIf(filter, action, a);
		}

		@Override /* Overridden from MetaProvider */
		public <A extends Annotation> void forEachDeclaredAnnotation(Class<A> type, Class<?> onClass, Predicate<A> filter, Consumer<A> action) {
			if (type != null && onClass != null)
				for (A a : declaredAnnotations(type, onClass))
					PredicateUtils.consumeIf(filter, action, a);
		}

		@Override /* Overridden from MetaProvider */
		public <A extends Annotation> A lastAnnotation(Class<A> type, Class<?> onClass, Predicate<A> filter) {
			A x = null;
			if (type != null && onClass != null)
				for (A a : annotations(type, onClass))
					if (PredicateUtils.test(filter, a))
						x = a;
			return x;
		}

		@Override /* Overridden from MetaProvider */
		public <A extends Annotation> A lastAnnotation(Class<A> type, Constructor<?> onConstructor, Predicate<A> filter) {
			A x = null;
			if (type != null && onConstructor != null)
				for (A a : annotations(type, onConstructor))
					if (PredicateUtils.test(filter, a))
						x = a;
			return x;
		}

		@Override /* Overridden from MetaProvider */
		public <A extends Annotation> A lastAnnotation(Class<A> type, Field onField, Predicate<A> filter) {
			A x = null;
			if (type != null && onField != null)
				for (A a : annotations(type, onField))
					if (PredicateUtils.test(filter, a))
						x = a;
			return x;
		}

		@Override /* Overridden from MetaProvider */
		public <A extends Annotation> A lastAnnotation(Class<A> type, Method onMethod, Predicate<A> filter) {
			A x = null;
			if (type != null && onMethod != null)
				for (A a : annotations(type, onMethod))
					if (PredicateUtils.test(filter, a))
						x = a;
			return x;
		}

		@Override /* Overridden from MetaProvider */
		public <A extends Annotation> A lastDeclaredAnnotation(Class<A> type, Class<?> onClass, Predicate<A> filter) {
			A x = null;
			if (type != null && onClass != null)
				for (A a : declaredAnnotations(type, onClass))
					if (PredicateUtils.test(filter, a))
						x = a;
			return x;
		}

		private <A extends Annotation> A[] annotations(Class<A> type, Class<?> onClass) {
			return (A[])classAnnotationCache.get(onClass, type);
		}

		private <A extends Annotation> A[] annotations(Class<A> type, Constructor<?> onConstructor) {
			return (A[])constructorAnnotationCache.get(onConstructor, type);
		}

		private <A extends Annotation> A[] annotations(Class<A> type, Field onField) {
			return (A[])fieldAnnotationCache.get(onField, type);
		}

		private <A extends Annotation> A[] annotations(Class<A> type, Method onMethod) {
			return (A[])methodAnnotationCache.get(onMethod, type);
		}

		private <A extends Annotation> A[] declaredAnnotations(Class<A> type, Class<?> onClass) {
			return (A[])declaredClassAnnotationCache.get(onClass, type);
		}
	};

	/**
	 * Finds the first matching annotation on the specified class.
	 *
	 * @param <A> The annotation type to find.
	 * @param type The annotation type to find.
	 * @param onClass The class to search on.
	 * @param filter A predicate to apply to the entries to determine if value should be used.  Can be <jk>null</jk>.
	 * @return The matched annotation, or <jk>null</jk> if not found.
	 */
	<A extends Annotation> A firstAnnotation(Class<A> type, Class<?> onClass, Predicate<A> filter);

	/**
	 * Finds the first matching annotation on the specified constructor.
	 *
	 * @param <A> The annotation type to find.
	 * @param type The annotation type to find.
	 * @param onConstructor The constructor to search on.
	 * @param filter A predicate to apply to the entries to determine if value should be used.  Can be <jk>null</jk>.
	 * @return The matched annotation, or <jk>null</jk> if no annotations matched.
	 */
	<A extends Annotation> A firstAnnotation(Class<A> type, Constructor<?> onConstructor, Predicate<A> filter);

	/**
	 * Finds the first matching annotation on the specified field.
	 *
	 * @param <A> The annotation type to find.
	 * @param type The annotation type to find.
	 * @param onField The field to search on.
	 * @param filter A predicate to apply to the entries to determine if value should be used.  Can be <jk>null</jk>.
	 * @return The matched annotation, or <jk>null</jk> if no annotations matched.
	 */
	<A extends Annotation> A firstAnnotation(Class<A> type, Field onField, Predicate<A> filter);

	/**
	 * Finds the first matching annotation on the specified method.
	 *
	 * @param <A> The annotation type to find.
	 * @param type The annotation type to find.
	 * @param onMethod The method to search on.
	 * @param filter A predicate to apply to the entries to determine if value should be used.  Can be <jk>null</jk>.
	 * @return The matched annotation, or <jk>null</jk> if no annotations matched.
	 */
	<A extends Annotation> A firstAnnotation(Class<A> type, Method onMethod, Predicate<A> filter);

	/**
	 * Finds the first matching declared annotations on the specified class.
	 *
	 * @param <A> The annotation type to find.
	 * @param type The annotation type to find.
	 * @param onClass The class to search on.
	 * @param filter A predicate to apply to the entries to determine if value should be used.  Can be <jk>null</jk>.
	 * @return The matched annotation, or <jk>null</jk> if no annotations matched.
	 */
	<A extends Annotation> A firstDeclaredAnnotation(Class<A> type, Class<?> onClass, Predicate<A> filter);

	/**
	 * Performs an action on the matching annotations on the specified class.
	 *
	 * @param <A> The annotation type to find.
	 * @param type The annotation type to find.
	 * @param onClass The class to search on.
	 * @param filter A predicate to apply to the entries to determine if action should be performed.  Can be <jk>null</jk>.
	 * @param action An action to perform on the entry.
	 */
	<A extends Annotation> void forEachAnnotation(Class<A> type, Class<?> onClass, Predicate<A> filter, Consumer<A> action);

	/**
	 * Performs an action on the matching annotations on the specified constructor.
	 *
	 * @param <A> The annotation type to find.
	 * @param type The annotation type to find.
	 * @param onConstructor The constructor to search on.
	 * @param filter A predicate to apply to the entries to determine if action should be performed.  Can be <jk>null</jk>.
	 * @param action An action to perform on the entry.
	 */
	<A extends Annotation> void forEachAnnotation(Class<A> type, Constructor<?> onConstructor, Predicate<A> filter, Consumer<A> action);

	/**
	 * Performs an action on the matching annotations on the specified field.
	 *
	 * @param <A> The annotation type to find.
	 * @param type The annotation type to find.
	 * @param onField The field to search on.
	 * @param filter A predicate to apply to the entries to determine if action should be performed.  Can be <jk>null</jk>.
	 * @param action An action to perform on the entry.
	 */
	<A extends Annotation> void forEachAnnotation(Class<A> type, Field onField, Predicate<A> filter, Consumer<A> action);

	/**
	 * Performs an action on the matching annotations on the specified method.
	 *
	 * @param <A> The annotation type to find.
	 * @param type The annotation type to find.
	 * @param onMethod The method to search on.
	 * @param filter A predicate to apply to the entries to determine if action should be performed.  Can be <jk>null</jk>.
	 * @param action An action to perform on the entry.
	 */
	<A extends Annotation> void forEachAnnotation(Class<A> type, Method onMethod, Predicate<A> filter, Consumer<A> action);

	/**
	 * Performs an action on the matching declared annotations on the specified class.
	 *
	 * @param <A> The annotation type to find.
	 * @param type The annotation type to find.
	 * @param onClass The class to search on.
	 * @param filter A predicate to apply to the entries to determine if action should be performed.  Can be <jk>null</jk>.
	 * @param action An action to perform on the entry.
	 */
	<A extends Annotation> void forEachDeclaredAnnotation(Class<A> type, Class<?> onClass, Predicate<A> filter, Consumer<A> action);

	/**
	 * Finds the last matching annotation on the specified class.
	 *
	 * @param <A> The annotation type to find.
	 * @param type The annotation type to find.
	 * @param onClass The class to search on.
	 * @param filter A predicate to apply to the entries to determine if value should be used.  Can be <jk>null</jk>.
	 * @return The matched annotation, or <jk>null</jk> if not found.
	 */
	<A extends Annotation> A lastAnnotation(Class<A> type, Class<?> onClass, Predicate<A> filter);

	/**
	 * Finds the last matching annotation on the specified constructor.
	 *
	 * @param <A> The annotation type to find.
	 * @param type The annotation type to find.
	 * @param onConstructor The constructor to search on.
	 * @param filter A predicate to apply to the entries to determine if value should be used.  Can be <jk>null</jk>.
	 * @return The matched annotation, or <jk>null</jk> if no annotations matched.
	 */
	<A extends Annotation> A lastAnnotation(Class<A> type, Constructor<?> onConstructor, Predicate<A> filter);

	/**
	 * Finds the last matching annotation on the specified field.
	 *
	 * @param <A> The annotation type to find.
	 * @param type The annotation type to find.
	 * @param onField The field to search on.
	 * @param filter A predicate to apply to the entries to determine if value should be used.  Can be <jk>null</jk>.
	 * @return The matched annotation, or <jk>null</jk> if no annotations matched.
	 */
	<A extends Annotation> A lastAnnotation(Class<A> type, Field onField, Predicate<A> filter);

	/**
	 * Finds the last matching annotation on the specified method.
	 *
	 * @param <A> The annotation type to find.
	 * @param type The annotation type to find.
	 * @param onMethod The method to search on.
	 * @param filter A predicate to apply to the entries to determine if value should be used.  Can be <jk>null</jk>.
	 * @return The matched annotation, or <jk>null</jk> if no annotations matched.
	 */
	<A extends Annotation> A lastAnnotation(Class<A> type, Method onMethod, Predicate<A> filter);

	/**
	 * Finds the last matching declared annotations on the specified class.
	 *
	 * @param <A> The annotation type to find.
	 * @param type The annotation type to find.
	 * @param onClass The class to search on.
	 * @param filter A predicate to apply to the entries to determine if value should be used.  Can be <jk>null</jk>.
	 * @return The matched annotation, or <jk>null</jk> if no annotations matched.
	 */
	<A extends Annotation> A lastDeclaredAnnotation(Class<A> type, Class<?> onClass, Predicate<A> filter);
}