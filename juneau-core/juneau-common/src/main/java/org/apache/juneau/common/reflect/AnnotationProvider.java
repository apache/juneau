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

import static org.apache.juneau.common.utils.PredicateUtils.*;
import static org.apache.juneau.common.utils.Utils.*;

import java.lang.annotation.*;
import java.lang.reflect.*;
import java.util.function.*;

import org.apache.juneau.common.collections.*;

/**
 * Interface that provides the ability to look up annotations on classes/methods/constructors/fields.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * </ul>
 */
@SuppressWarnings("rawtypes")
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
		private final Cache2<Class<?>,Class<? extends Annotation>,Annotation[]> classAnnotationCache = (Cache2)Cache2.of(Class.class, Class.class, Annotation[].class).disableCaching(DISABLE_ANNOTATION_CACHING).supplier((k1, k2) -> k1.getAnnotationsByType(k2)).build();
		private final Cache2<Class<?>,Class<? extends Annotation>,Annotation[]> declaredClassAnnotationCache = (Cache2)Cache2.of(Class.class, Class.class, Annotation[].class).disableCaching(DISABLE_ANNOTATION_CACHING).supplier((k1, k2) -> k1.getDeclaredAnnotationsByType(k2)).build();
		private final Cache2<Method,Class<? extends Annotation>,Annotation[]> methodAnnotationCache = (Cache2)Cache2.of(Method.class, Class.class, Annotation[].class).disableCaching(DISABLE_ANNOTATION_CACHING).supplier((k1, k2) -> k1.getAnnotationsByType(k2)).build();
		private final Cache2<Field,Class<? extends Annotation>,Annotation[]> fieldAnnotationCache = (Cache2)Cache2.of(Field.class, Class.class, Annotation[].class).disableCaching(DISABLE_ANNOTATION_CACHING).supplier((k1, k2) -> k1.getAnnotationsByType(k2)).build();
		private final Cache2<Constructor<?>,Class<? extends Annotation>,Annotation[]> constructorAnnotationCache = (Cache2)Cache2.of(Constructor.class, Class.class, Annotation[].class).disableCaching(DISABLE_ANNOTATION_CACHING).supplier((k1, k2) -> k1.getAnnotationsByType(k2)).build();
		// @formatter:on

		@Override /* Overridden from MetaProvider */
		public <A extends Annotation> A firstAnnotation(Class<A> type, Class<?> onClass, Predicate<A> filter) {
			if (nn(type) && nn(onClass))
				for (A a : annotations(type, onClass))
					if (test(filter, a))
						return a;
			return null;
		}

		@Override /* Overridden from MetaProvider */
		public <A extends Annotation> A firstAnnotation(Class<A> type, Constructor<?> onConstructor, Predicate<A> filter) {
			if (nn(type) && nn(onConstructor))
				for (A a : annotations(type, onConstructor))
					if (test(filter, a))
						return a;
			return null;
		}

		@Override /* Overridden from MetaProvider */
		public <A extends Annotation> A firstAnnotation(Class<A> type, Field onField, Predicate<A> filter) {
			if (nn(type) && nn(onField))
				for (A a : annotations(type, onField))
					if (test(filter, a))
						return a;
			return null;
		}

		@Override /* Overridden from MetaProvider */
		public <A extends Annotation> A firstAnnotation(Class<A> type, Method onMethod, Predicate<A> filter) {
			if (nn(type) && nn(onMethod))
				for (A a : annotations(type, onMethod))
					if (test(filter, a))
						return a;
			return null;
		}

		@Override /* Overridden from MetaProvider */
		public <A extends Annotation> A firstDeclaredAnnotation(Class<A> type, Class<?> onClass, Predicate<A> filter) {
			if (nn(type) && nn(onClass))
				for (A a : declaredAnnotations(type, onClass))
					if (test(filter, a))
						return a;
			return null;
		}

		@Override /* Overridden from MetaProvider */
		public <A extends Annotation> void forEachAnnotation(Class<A> type, Class<?> onClass, Predicate<A> filter, Consumer<A> action) {
			if (nn(type) && nn(onClass))
				for (A a : annotations(type, onClass))
					consumeIf(filter, action, a);
		}

		@Override /* Overridden from MetaProvider */
		public <A extends Annotation> void forEachAnnotation(Class<A> type, Constructor<?> onConstructor, Predicate<A> filter, Consumer<A> action) {
			if (nn(type) && nn(onConstructor))
				for (A a : annotations(type, onConstructor))
					consumeIf(filter, action, a);
		}

		@Override /* Overridden from MetaProvider */
		public <A extends Annotation> void forEachAnnotation(Class<A> type, Field onField, Predicate<A> filter, Consumer<A> action) {
			if (nn(type) && nn(onField))
				for (A a : annotations(type, onField))
					consumeIf(filter, action, a);
		}

		@Override /* Overridden from MetaProvider */
		public <A extends Annotation> void forEachAnnotation(Class<A> type, Method onMethod, Predicate<A> filter, Consumer<A> action) {
			if (nn(type) && nn(onMethod))
				for (A a : annotations(type, onMethod))
					consumeIf(filter, action, a);
		}

		@Override /* Overridden from MetaProvider */
		public <A extends Annotation> void forEachDeclaredAnnotation(Class<A> type, Class<?> onClass, Predicate<A> filter, Consumer<A> action) {
			if (nn(type) && nn(onClass))
				for (A a : declaredAnnotations(type, onClass))
					consumeIf(filter, action, a);
		}

		@Override /* Overridden from MetaProvider */
		public <A extends Annotation> A lastAnnotation(Class<A> type, Class<?> onClass, Predicate<A> filter) {
			A x = null;
			if (nn(type) && nn(onClass))
				for (A a : annotations(type, onClass))
					if (test(filter, a))
						x = a;
			return x;
		}

		@Override /* Overridden from MetaProvider */
		public <A extends Annotation> A lastAnnotation(Class<A> type, Constructor<?> onConstructor, Predicate<A> filter) {
			A x = null;
			if (nn(type) && nn(onConstructor))
				for (A a : annotations(type, onConstructor))
					if (test(filter, a))
						x = a;
			return x;
		}

		@Override /* Overridden from MetaProvider */
		public <A extends Annotation> A lastAnnotation(Class<A> type, Field onField, Predicate<A> filter) {
			A x = null;
			if (nn(type) && nn(onField))
				for (A a : annotations(type, onField))
					if (test(filter, a))
						x = a;
			return x;
		}

		@Override /* Overridden from MetaProvider */
		public <A extends Annotation> A lastAnnotation(Class<A> type, Method onMethod, Predicate<A> filter) {
			A x = null;
			if (nn(type) && nn(onMethod))
				for (A a : annotations(type, onMethod))
					if (test(filter, a))
						x = a;
			return x;
		}

		@Override /* Overridden from MetaProvider */
		public <A extends Annotation> A lastDeclaredAnnotation(Class<A> type, Class<?> onClass, Predicate<A> filter) {
			A x = null;
			if (nn(type) && nn(onClass))
				for (A a : declaredAnnotations(type, onClass))
					if (test(filter, a))
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