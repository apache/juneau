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
		public <A extends Annotation> void getAnnotations(Class<A> type, Class<?> onClass, Predicate<A> predicate, Consumer<A> consumer) {
			if (type != null && onClass != null)
				for (A a : (A[])classAnnotationCache.get(onClass,type))
					consume(predicate, consumer, a);
		}

		@Override /* MetaProvider */
		public <A extends Annotation> A getAnnotation(Class<A> type, Class<?> onClass, Predicate<A> predicate) {
			if (type != null && onClass != null)
				for (A a : (A[])classAnnotationCache.get(onClass,type))
					if (passes(predicate, a))
						return a;
			return null;
		}

		@Override /* MetaProvider */
		public <A extends Annotation> void getDeclaredAnnotations(Class<A> type, Class<?> onClass, Predicate<A> predicate, Consumer<A> consumer) {
			if (type != null && onClass != null)
				for (A a : (A[])declaredClassAnnotationCache.get(onClass,type))
					consume(predicate, consumer, a);
		}

		@Override /* MetaProvider */
		public <A extends Annotation> A getDeclaredAnnotation(Class<A> type, Class<?> onClass, Predicate<A> predicate) {
			if (type != null && onClass != null)
				for (A a : (A[])declaredClassAnnotationCache.get(onClass,type))
					if (passes(predicate, a))
						return a;
			return null;
		}

		@Override /* MetaProvider */
		public <A extends Annotation> void getAnnotations(Class<A> type, Method onMethod, Predicate<A> predicate, Consumer<A> consumer) {
			if (type != null && onMethod != null)
				for (A a : (A[])methodAnnotationCache.get(onMethod,type))
					consume(predicate, consumer, a);
		}

		@Override /* MetaProvider */
		public <A extends Annotation> A getAnnotation(Class<A> type, Method onMethod, Predicate<A> predicate) {
			if (type != null && onMethod != null)
				for (A a : (A[])methodAnnotationCache.get(onMethod,type))
					if (passes(predicate, a))
						return a;
			return null;
		}

		@Override /* MetaProvider */
		public <A extends Annotation> void getAnnotations(Class<A> type, Field onField, Predicate<A> predicate, Consumer<A> consumer) {
			if (type != null && onField != null)
				for (A a : (A[])fieldAnnotationCache.get(onField,type))
					consume(predicate, consumer, a);
		}

		@Override /* MetaProvider */
		public <A extends Annotation> A getAnnotation(Class<A> type, Field onField, Predicate<A> predicate) {
			if (type != null && onField != null)
				for (A a : (A[])fieldAnnotationCache.get(onField,type))
					if (passes(predicate, a))
						return a;
			return null;
		}

		@Override /* MetaProvider */
		public <A extends Annotation> void getAnnotations(Class<A> type, Constructor<?> onConstructor, Predicate<A> predicate, Consumer<A> consumer) {
			if (type != null && onConstructor != null)
				for (A a : (A[])constructorAnnotationCache.get(onConstructor,type))
					consume(predicate, consumer, a);
		}

		@Override /* MetaProvider */
		public <A extends Annotation> A getAnnotation(Class<A> type, Constructor<?> onConstructor, Predicate<A> predicate) {
			if (type != null && onConstructor != null)
				for (A a : (A[])constructorAnnotationCache.get(onConstructor,type))
					if (passes(predicate, a))
						return a;
			return null;
		}
	};

	/**
	 * Finds the specified annotations on the specified class.
	 *
	 * @param type The annotation type to find.
	 * @param onClass The class to search on.
	 * @param predicate The predicate.
	 * @param consumer The consumer.
	 */
	<A extends Annotation> void getAnnotations(Class<A> type, Class<?> onClass, Predicate<A> predicate, Consumer<A> consumer);

	/**
	 * Finds the first annotation on the specified class matching the specified predicate.
	 *
	 * @param type The annotation type to find.
	 * @param onClass The class to search on.
	 * @param predicate The predicate.
	 * @return The annotations in an unmodifiable list, or an empty list if not found.
	 */
	<A extends Annotation> A getAnnotation(Class<A> type, Class<?> onClass, Predicate<A> predicate);

	/**
	 * Finds the specified declared annotations on the specified class.
	 *
	 * @param type The annotation type to find.
	 * @param onClass The class to search on.
	 * @param predicate The predicate.
	 * @param consumer The consumer.
	 */
	<A extends Annotation> void getDeclaredAnnotations(Class<A> type, Class<?> onClass, Predicate<A> predicate, Consumer<A> consumer);

	/**
	 * Finds the specified declared annotations on the specified class that match the specified predicate.
	 *
	 * @param type The annotation type to find.
	 * @param onClass The class to search on.
	 * @param predicate The predicate.
	 * @return The matched annotation, or <jk>null</jk> if no annotations matched.
	 */
	<A extends Annotation> A getDeclaredAnnotation(Class<A> type, Class<?> onClass, Predicate<A> predicate);

	/**
	 * Finds the specified annotations on the specified method.
	 *
	 * @param <A> The annotation type to find.
	 * @param type The annotation type to find.
	 * @param onMethod The method to search on.
	 * @param predicate The predicate.
	 * @param consumer The consumer.
	 */
	<A extends Annotation> void getAnnotations(Class<A> type, Method onMethod, Predicate<A> predicate, Consumer<A> consumer);

	/**
	 * Finds the specified annotations on the specified method.
	 *
	 * @param <A> The annotation type to find.
	 * @param type The annotation type to find.
	 * @param onMethod The method to search on.
	 * @param predicate The predicate.
	 * @return The matched annotation, or <jk>null</jk> if no annotations matched.
	 */
	<A extends Annotation> A getAnnotation(Class<A> type, Method onMethod, Predicate<A> predicate);

	/**
	 * Finds the specified annotations on the specified field.
	 *
	 * @param <A> The annotation type to find.
	 * @param type The annotation type to find.
	 * @param onField The field to search on.
	 * @param predicate The predicate.
	 * @param consumer The consumer.
	 */
	<A extends Annotation> void getAnnotations(Class<A> type, Field onField, Predicate<A> predicate, Consumer<A> consumer);

	/**
	 * Finds the specified annotations on the specified field.
	 *
	 * @param <A> The annotation type to find.
	 * @param type The annotation type to find.
	 * @param onField The field to search on.
	 * @param predicate The predicate.
	 * @return The matched annotation, or <jk>null</jk> if no annotations matched.
	 */
	<A extends Annotation> A getAnnotation(Class<A> type, Field onField, Predicate<A> predicate);

	/**
	 * Finds the specified annotations on the specified constructor.
	 *
	 * @param <A> The annotation type to find.
	 * @param type The annotation type to find.
	 * @param onConstructor The constructor to search on.
	 * @param predicate The predicate.
	 * @param consumer The consumer.
	 */
	<A extends Annotation> void getAnnotations(Class<A> type, Constructor<?> onConstructor, Predicate<A> predicate, Consumer<A> consumer);

	/**
	 * Finds the specified annotations on the specified constructor.
	 *
	 * @param <A> The annotation type to find.
	 * @param type The annotation type to find.
	 * @param onConstructor The constructor to search on.
	 * @param predicate The predicate to match the annotation against.
	 * @return The matched annotation, or <jk>null</jk> if no annotations matched.
	 */
	<A extends Annotation> A getAnnotation(Class<A> type, Constructor<?> onConstructor, Predicate<A> predicate);
}
