// ***************************************************************************************************************************
// * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file *
// * distributed with this work for additional information regarding copyright ownership.  The ASF licenses this file        *
// * to you under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance            *
// * with the License.  You may obtain a copy of the License at                                                              *
// *                                                                                                                         *
// *  http://www.apache.org/licenses/LICENSE-2.0                                                                             *
// *                                                                                                                         *
// * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an  *
// * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the License for the        *
// * specific language governing permissions and limitations under the License.                                              *
// ***************************************************************************************************************************
package org.apache.juneau;

import static org.apache.juneau.internal.ConsumerUtils.*;

import java.lang.annotation.*;
import java.lang.reflect.*;
import java.util.function.*;

import org.apache.juneau.internal.*;

/**
 * Interface that provides the ability to look up annotations on classes/methods/constructors/fields.
 *
 * <ul class='seealso'>
 * 	<li class='extlink'>{@source}
 * </ul>
 */
public interface AnnotationProvider {

	/**
	 * Disable annotation caching.
	 */
	static final boolean DISABLE_ANNOTATION_CACHING = Boolean.getBoolean("juneau.disableAnnotationCaching");

	/**
	 * Default metadata provider.
	 */
	@SuppressWarnings("unchecked")
	public static final AnnotationProvider DEFAULT = new AnnotationProvider() {

		private final TwoKeyConcurrentCache<Class<?>,Class<? extends Annotation>,Annotation[]> classAnnotationCache = new TwoKeyConcurrentCache<>(DISABLE_ANNOTATION_CACHING, (k1,k2) -> k1.getAnnotationsByType(k2));
		private final TwoKeyConcurrentCache<Class<?>,Class<? extends Annotation>,Annotation[]> declaredClassAnnotationCache = new TwoKeyConcurrentCache<>(DISABLE_ANNOTATION_CACHING, (k1,k2) -> k1.getDeclaredAnnotationsByType(k2));
		private final TwoKeyConcurrentCache<Method,Class<? extends Annotation>,Annotation[]> methodAnnotationCache = new TwoKeyConcurrentCache<>(DISABLE_ANNOTATION_CACHING, (k1,k2) -> k1.getAnnotationsByType(k2));
		private final TwoKeyConcurrentCache<Field,Class<? extends Annotation>,Annotation[]> fieldAnnotationCache = new TwoKeyConcurrentCache<>(DISABLE_ANNOTATION_CACHING, (k1,k2) -> k1.getAnnotationsByType(k2));
		private final TwoKeyConcurrentCache<Constructor<?>,Class<? extends Annotation>,Annotation[]> constructorAnnotationCache = new TwoKeyConcurrentCache<>(DISABLE_ANNOTATION_CACHING, (k1,k2) -> k1.getAnnotationsByType(k2));

		@Override /* MetaProvider */
		public <A extends Annotation> void forEachAnnotation(Class<A> type, Class<?> onClass, Predicate<A> filter, Consumer<A> action) {
			if (type != null && onClass != null)
				for (A a : (A[])classAnnotationCache.get(onClass,type))
					consume(filter, action, a);
		}

		@Override /* MetaProvider */
		public <A extends Annotation> A getAnnotation(Class<A> type, Class<?> onClass, Predicate<A> filter) {
			if (type != null && onClass != null)
				for (A a : (A[])classAnnotationCache.get(onClass,type))
					if (passes(filter, a))
						return a;
			return null;
		}

		@Override /* MetaProvider */
		public <A extends Annotation> void forEachDeclaredAnnotation(Class<A> type, Class<?> onClass, Predicate<A> filter, Consumer<A> action) {
			if (type != null && onClass != null)
				for (A a : (A[])declaredClassAnnotationCache.get(onClass,type))
					consume(filter, action, a);
		}

		@Override /* MetaProvider */
		public <A extends Annotation> A getDeclaredAnnotation(Class<A> type, Class<?> onClass, Predicate<A> filter) {
			if (type != null && onClass != null)
				for (A a : (A[])declaredClassAnnotationCache.get(onClass,type))
					if (passes(filter, a))
						return a;
			return null;
		}

		@Override /* MetaProvider */
		public <A extends Annotation> void forEachAnnotation(Class<A> type, Method onMethod, Predicate<A> filter, Consumer<A> action) {
			if (type != null && onMethod != null)
				for (A a : (A[])methodAnnotationCache.get(onMethod,type))
					consume(filter, action, a);
		}

		@Override /* MetaProvider */
		public <A extends Annotation> A getAnnotation(Class<A> type, Method onMethod, Predicate<A> filter) {
			if (type != null && onMethod != null)
				for (A a : (A[])methodAnnotationCache.get(onMethod,type))
					if (passes(filter, a))
						return a;
			return null;
		}

		@Override /* MetaProvider */
		public <A extends Annotation> void forEachAnnotation(Class<A> type, Field onField, Predicate<A> filter, Consumer<A> action) {
			if (type != null && onField != null)
				for (A a : (A[])fieldAnnotationCache.get(onField,type))
					consume(filter, action, a);
		}

		@Override /* MetaProvider */
		public <A extends Annotation> A getAnnotation(Class<A> type, Field onField, Predicate<A> filter) {
			if (type != null && onField != null)
				for (A a : (A[])fieldAnnotationCache.get(onField,type))
					if (passes(filter, a))
						return a;
			return null;
		}

		@Override /* MetaProvider */
		public <A extends Annotation> void forEachAnnotation(Class<A> type, Constructor<?> onConstructor, Predicate<A> filter, Consumer<A> action) {
			if (type != null && onConstructor != null)
				for (A a : (A[])constructorAnnotationCache.get(onConstructor,type))
					consume(filter, action, a);
		}

		@Override /* MetaProvider */
		public <A extends Annotation> A getAnnotation(Class<A> type, Constructor<?> onConstructor, Predicate<A> filter) {
			if (type != null && onConstructor != null)
				for (A a : (A[])constructorAnnotationCache.get(onConstructor,type))
					if (passes(filter, a))
						return a;
			return null;
		}
	};

	/**
	 * Performs an action on the matching annotations on the specified class.
	 *
	 * @param type The annotation type to find.
	 * @param onClass The class to search on.
	 * @param filter A predicate to apply to the entries to determine if action should be performed.  Can be <jk>null</jk>.
	 * @param action An action to perform on the entry.
	 */
	<A extends Annotation> void forEachAnnotation(Class<A> type, Class<?> onClass, Predicate<A> filter, Consumer<A> action);

	/**
	 * Finds the first matching annotation on the specified class.
	 *
	 * @param type The annotation type to find.
	 * @param onClass The class to search on.
	 * @param filter A predicate to apply to the entries to determine if value should be used.  Can be <jk>null</jk>.
	 * @return The annotations in an unmodifiable list, or an empty list if not found.
	 */
	<A extends Annotation> A getAnnotation(Class<A> type, Class<?> onClass, Predicate<A> filter);

	/**
	 * Performs an action on the matching declared annotations on the specified class.
	 *
	 * @param type The annotation type to find.
	 * @param onClass The class to search on.
	 * @param filter A predicate to apply to the entries to determine if action should be performed.  Can be <jk>null</jk>.
	 * @param action An action to perform on the entry.
	 */
	<A extends Annotation> void forEachDeclaredAnnotation(Class<A> type, Class<?> onClass, Predicate<A> filter, Consumer<A> action);

	/**
	 * Finds the first matching declared annotations on the specified class.
	 *
	 * @param type The annotation type to find.
	 * @param onClass The class to search on.
	 * @param filter A predicate to apply to the entries to determine if value should be used.  Can be <jk>null</jk>.
	 * @return The matched annotation, or <jk>null</jk> if no annotations matched.
	 */
	<A extends Annotation> A getDeclaredAnnotation(Class<A> type, Class<?> onClass, Predicate<A> filter);

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
	 * Finds the first matching annotation on the specified method.
	 *
	 * @param <A> The annotation type to find.
	 * @param type The annotation type to find.
	 * @param onMethod The method to search on.
	 * @param filter A predicate to apply to the entries to determine if value should be used.  Can be <jk>null</jk>.
	 * @return The matched annotation, or <jk>null</jk> if no annotations matched.
	 */
	<A extends Annotation> A getAnnotation(Class<A> type, Method onMethod, Predicate<A> filter);

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
	 * Finds the first matching annotation on the specified field.
	 *
	 * @param <A> The annotation type to find.
	 * @param type The annotation type to find.
	 * @param onField The field to search on.
	 * @param filter A predicate to apply to the entries to determine if value should be used.  Can be <jk>null</jk>.
	 * @return The matched annotation, or <jk>null</jk> if no annotations matched.
	 */
	<A extends Annotation> A getAnnotation(Class<A> type, Field onField, Predicate<A> filter);

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
	 * Finds the first matching annotation on the specified constructor.
	 *
	 * @param <A> The annotation type to find.
	 * @param type The annotation type to find.
	 * @param onConstructor The constructor to search on.
	 * @param filter A predicate to apply to the entries to determine if value should be used.  Can be <jk>null</jk>.
	 * @return The matched annotation, or <jk>null</jk> if no annotations matched.
	 */
	<A extends Annotation> A getAnnotation(Class<A> type, Constructor<?> onConstructor, Predicate<A> filter);
}
