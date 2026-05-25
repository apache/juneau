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
package org.apache.juneau.commons.inject;

import java.util.*;

import org.apache.juneau.commons.reflect.*;
import org.apache.juneau.commons.settings.*;
import org.apache.juneau.commons.svl.*;

/**
 * Internal helper used by the reflection layer to resolve {@code @Value}-annotated injection sites.
 *
 * <p>
 * Splits the {@code @Value} handling into three small pieces so callers (field / parameter / method)
 * can share the same coercion + error-formatting code without duplicating the lookup logic:
 *
 * <ol>
 * 	<li>{@link #findValueExpression(List)} — given an annotation list, return the first {@code @Value}
 * 		expression (or {@code null} if no {@code @Value} is present).
 * 	<li>{@link #checkInjectConflict(List, String)} — throw {@link BeanCreationException} if both
 * 		{@code @Value} and {@code @Inject}/{@code @Autowired} are present on the same site.
 * 	<li>{@link #resolve(String, Class, String)} — pass the expression through {@code VarResolver},
 * 		coerce to the target type via {@link Settings#toType(String, Class)}, and wrap failures in
 * 		{@link BeanCreationException}.
 * </ol>
 *
 * <p>
 * The resolver intentionally uses {@link VarResolver#DEFAULT}, which already includes
 * {@code PropertyVar} (the {@code $P{key[,default]}} variable that reads from {@link Settings}).
 * With Phase 2 of TODO-79 landed, the {@code ${key}} shortcut routes through that same {@code PropertyVar},
 * so {@code @Value("${db.url}")} and {@code @Value("$P{db.url}")} are equivalent.
 */
public final class ValueResolver {

	private ValueResolver() {}

	/**
	 * Returns the {@code value()} expression carried by the first {@code @Value} annotation in the list,
	 * or {@code null} if no {@code @Value} (Juneau or Spring) is present.
	 *
	 * @param annotations The annotation list (from {@link FieldInfo#getAnnotations()},
	 * 	{@link ParameterInfo#getAnnotations()}, etc.).
	 * @return The expression, or {@code null} if not a {@code @Value} site.
	 */
	public static String findValueExpression(List<? extends AnnotationInfo<?>> annotations) {
		if (annotations == null || annotations.isEmpty())
			return null;
		return annotations.stream()
			.map(JsrSupport::valueExpression)
			.filter(Objects::nonNull)
			.findFirst()
			.orElse(null);
	}

	/**
	 * Returns {@code true} if any annotation in the list is a {@code @Value} marker.
	 *
	 * @param annotations The annotation list.
	 * @return {@code true} if at least one annotation matches {@link JsrSupport#isValueAnnotation(AnnotationInfo)}.
	 */
	public static boolean hasValueAnnotation(List<? extends AnnotationInfo<?>> annotations) {
		if (annotations == null || annotations.isEmpty())
			return false;
		return annotations.stream().anyMatch(JsrSupport::isValueAnnotation);
	}

	/**
	 * Throws {@link BeanCreationException} if an injection site carries both {@code @Value} and an
	 * {@code @Inject}/{@code @Autowired} marker.
	 *
	 * <p>
	 * {@code @Value} resolves strings/primitives; {@code @Inject} resolves beans. Combining them on
	 * the same site is ambiguous — fail fast with a clear message.
	 *
	 * @param annotations The annotation list.
	 * @param siteDescription A human-readable description of the site (used in the error message).
	 */
	public static void checkInjectConflict(List<? extends AnnotationInfo<?>> annotations, String siteDescription) {
		if (annotations == null || annotations.isEmpty())
			return;
		var hasValue = annotations.stream().anyMatch(JsrSupport::isValueAnnotation);
		if (! hasValue)
			return;
		var hasInject = annotations.stream().anyMatch(JsrSupport::isInjectAnnotation);
		if (hasInject)
			throw new BeanCreationException("@Value and @Inject are mutually exclusive on " + siteDescription);
	}

	/**
	 * Resolves a {@code @Value} expression against the default {@link VarResolver} and coerces the
	 * result to {@code targetType} via {@link Settings#toType(String, Class)}.
	 *
	 * <p>
	 * Lookup semantics:
	 * <ul>
	 * 	<li>If the resolved value is {@code null} and {@code targetType} is a primitive, throws
	 * 		{@link BeanCreationException} (primitives cannot be {@code null}).
	 * 	<li>If the resolved value is {@code null} and {@code targetType} is a reference type, returns
	 * 		{@code null}.
	 * 	<li>If coercion via {@link Settings#toType(String, Class)} fails, the underlying exception is
	 * 		wrapped in {@link BeanCreationException} with a message that includes the expression, the
	 * 		resolved string, and the target type.
	 * </ul>
	 *
	 * @param expression The {@code @Value} expression (typically {@code "${key}"} or {@code "${key:default}"}).
	 * @param targetType The target Java type. May be a primitive.
	 * @param siteDescription A human-readable description of the site (used in error messages).
	 * @return The coerced value.
	 */
	public static Object resolve(String expression, Class<?> targetType, String siteDescription) {
		// Resolve placeholders via VarResolver.DEFAULT — this picks up the ${...} shortcut from
		// Phase 2 (lowered to $P{...}) and routes through Settings.get(key) for the final lookup.
		var resolved = VarResolver.DEFAULT.resolve(expression);
		var effectiveType = box(targetType);

		if (resolved == null || resolved.isEmpty()) {
			if (targetType.isPrimitive())
				throw new BeanCreationException("Could not resolve required @Value('" + expression + "') for primitive site " + siteDescription);
			if (resolved == null)
				return null;
			// Empty string is a legitimate value for String / CharSequence target types.
			if (effectiveType == String.class)
				return resolved;
		}

		// Character has no usable valueOf(String) — Settings.toType can't coerce; handle manually.
		if (effectiveType == Character.class) {
			if (resolved.length() == 1)
				return resolved.charAt(0);
			throw new BeanCreationException(
				"Could not coerce @Value('" + expression + "') resolving to '" + resolved + "' to char for " + siteDescription);
		}

		try {
			return Settings.get().toType(resolved, effectiveType);
		} catch (RuntimeException e) {
			throw new BeanCreationException(
				"Could not coerce @Value('" + expression + "') resolving to '" + resolved + "' to "
					+ targetType.getName() + " for " + siteDescription,
				e);
		}
	}

	/**
	 * Returns the boxed counterpart of a primitive type, or {@code type} itself if not primitive.
	 *
	 * <p>
	 * {@link Settings#toType(String, Class)} uses reflection for {@code valueOf(String)} / {@code (String)}
	 * constructor lookup, which doesn't work for raw primitives ({@code int.class} etc.). Box them first.
	 *
	 * @param type The (possibly primitive) target type.
	 * @return The boxed equivalent, or {@code type} unchanged.
	 */
	private static Class<?> box(Class<?> type) {
		if (! type.isPrimitive())
			return type;
		if (type == int.class) return Integer.class;
		if (type == long.class) return Long.class;
		if (type == boolean.class) return Boolean.class;
		if (type == double.class) return Double.class;
		if (type == float.class) return Float.class;
		if (type == short.class) return Short.class;
		if (type == byte.class) return Byte.class;
		if (type == char.class) return Character.class;
		return type;  // void or unreachable
	}
}
