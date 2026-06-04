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

import static org.apache.juneau.commons.utils.CollectionUtils.*;

import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;

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
 * After the property-variable unification, the {@code ${key}} shortcut routes through that same {@code PropertyVar},
 * so {@code @Value("${db.url}")} and {@code @Value("$P{db.url}")} are equivalent.
 */
public final class ValueResolver {

	private ValueResolver() {}

	/**
	 * Per-expression {@link VarTemplate} cache shared by all {@code @Value} injection sites.
	 * The cache key is the {@code @Value} expression text —
	 * which is bounded by the number of distinct expressions in the application, not by user
	 * input — so the unbounded map cannot grow uncontrollably in practice. Compilation
	 * happens at most once per distinct expression; subsequent {@code @Value} resolutions
	 * read the cached {@link VarTemplate} and skip the tokenizer + var-registry-lookup
	 * pipeline entirely.
	 */
	private static final ConcurrentMap<String, VarTemplate> TEMPLATE_CACHE = new ConcurrentHashMap<>();

	/**
	 * Returns the compiled {@link VarTemplate} for the given expression, computing and caching
	 * on first use. Intended for use by reflection-layer injection sites
	 * ({@link FieldInfo#inject}, {@link ParameterInfo#resolveValue}) that want to amortize
	 * tokenization across repeated bean constructions.
	 *
	 * @param expression The {@code @Value} expression text. Must not be {@code null}.
	 * @return The cached compiled template (uses {@link VarResolver#DEFAULT}).
	 */
	public static VarTemplate getCompiledTemplate(String expression) {
		return TEMPLATE_CACHE.computeIfAbsent(expression, VarResolver.DEFAULT::compile);
	}

	/**
	 * Clears the internal {@link VarTemplate} cache. Primarily intended for tests that need to
	 * exercise the compile-time path repeatedly with different {@link VarResolver}s. Production
	 * code should not need to call this.
	 */
	public static void clearTemplateCache() {
		TEMPLATE_CACHE.clear();
	}

	/**
	 * Returns the {@code value()} expression carried by the first {@code @Value} annotation in the list,
	 * or {@code null} if no {@code @Value} (Juneau or Spring) is present.
	 *
	 * @param annotations The annotation list (from {@link FieldInfo#getAnnotations()},
	 * 	{@link ParameterInfo#getAnnotations()}, etc.).
	 * @return The expression, or {@code null} if not a {@code @Value} site.
	 */
	public static String findValueExpression(List<? extends AnnotationInfo<?>> annotations) {
		if (isEmpty(annotations))
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
		if (isEmpty(annotations))
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
		if (isEmpty(annotations))
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
		return resolve(expression, targetType, siteDescription, null);
	}

	/**
	 * Same as {@link #resolve(String, Class, String)} but consults caller-scoped
	 * {@link PropertySource} beans resolved from the supplied {@link BeanStore} between
	 * the {@link Settings} local/global override stores and the {@link Settings} sources chain.
	 *
	 * <p>
	 * The {@code beanStore} parameter is preserved by-reference; if its
	 * {@code PropertySource}-typed contents change between calls, the next resolution picks up
	 * the current state. Passing {@code null} (or a {@code BeanStore} with no
	 * {@code PropertySource}-typed beans) is byte-for-byte equivalent to the zero-argument
	 * overload &mdash; no extra allocation and no behavior change.
	 *
	 * @param expression The {@code @Value} expression.
	 * @param targetType The target Java type. May be a primitive.
	 * @param siteDescription A human-readable description of the site (used in error messages).
	 * @param beanStore Caller-scoped {@link BeanStore} consulted for {@link PropertySource} beans.
	 * 	May be {@code null}.
	 * @return The coerced value.
	 */
	public static Object resolve(String expression, Class<?> targetType, String siteDescription, BeanStore beanStore) {
		if (expression == null)
			return resolveCoerce(null, targetType, expression, siteDescription);
		var template = getCompiledTemplate(expression);
		var session = VarResolver.DEFAULT.createSession();
		var sources = scopedSources(beanStore);
		if (sources != null)
			session.bean(PropertySource[].class, sources);
		var resolved = template.resolve(session);
		return resolveCoerce(resolved, targetType, expression, siteDescription);
	}

	/**
	 * Generic-aware {@code @Value} resolution that honors {@link Supplier}{@code <T>}
	 * field types by returning a re-evaluating, threadsafe Supplier instead of a one-shot
	 * resolved value (field-type autodetect — bare {@code String} resolves once, {@code Supplier<T>}
	 * re-evaluates per {@code .get()}).
	 *
	 * <p>
	 * Behavior by {@code genericTargetType}:
	 * <ul>
	 * 	<li>{@code Supplier<String>} → returns a {@link Supplier} that re-evaluates the
	 * 		compiled template on every {@link Supplier#get()} call, each time against a
	 * 		fresh {@link VarResolverSession}. Safe to share across threads.
	 * 	<li>{@code Supplier<T>} for {@code T != String} → returns a {@link Supplier} that, on every
	 * 		{@link Supplier#get()} call, re-evaluates the template <i>and</i> coerces the resolved
	 * 		string into {@code T} via the same {@code String -> T} converter machinery that plain
	 * 		{@code @Value T} uses ({@link Settings#toType(String, Class)}). The coerced value is never
	 * 		memoized (late-binding contract); coercion failures surface as a {@link RuntimeException}
	 * 		from {@code .get()} naming the {@code @Value} key and target type. {@code T} is taken from
	 * 		any type the converter registry can build from a {@code String} (e.g. {@code Integer},
	 * 		{@code Long}, {@code Boolean}, {@code Duration}, {@code URI}, {@code Path}, enums).
	 * 	<li>Anything else (including raw {@code Supplier} and {@code Optional<Supplier<T>>}) → delegates
	 * 		to {@link #resolve(String, Class, String)} with the raw target class (preserves existing
	 * 		behavior; the {@code Optional<Supplier<T>>} nesting is intentionally not specially handled).
	 * </ul>
	 *
	 * @param expression The {@code @Value} expression. May be {@code null}.
	 * @param targetType The erased target class (e.g. {@code String.class} or {@code Supplier.class}).
	 * @param genericTargetType The declared generic type (e.g. {@code Supplier<Integer>}). May be
	 * 	{@code null}, in which case Supplier-detection is skipped.
	 * @param siteDescription Human-readable site description for error messages.
	 * @return Either a one-shot coerced value, or a {@code Supplier<T>} factory.
	 */
	public static Object resolve(String expression, Class<?> targetType, Type genericTargetType, String siteDescription) {
		return resolve(expression, targetType, genericTargetType, siteDescription, null);
	}

	/**
	 * Same as {@link #resolve(String, Class, Type, String)} but consults caller-scoped
	 * {@link PropertySource} beans resolved from the supplied {@link BeanStore}.
	 *
	 * <p>
	 * The {@code beanStore} reference is captured (not snapshotted) by the returned
	 * {@code Supplier<T>} when the declared field/parameter type is {@code Supplier<...>},
	 * so re-evaluating reads always see the current state of {@code beanStore.getBeansOfType(PropertySource.class)}.
	 *
	 * @param expression The {@code @Value} expression. May be {@code null}.
	 * @param targetType The erased target class.
	 * @param genericTargetType The declared generic type. May be {@code null}.
	 * @param siteDescription Human-readable site description for error messages.
	 * @param beanStore Caller-scoped {@link BeanStore} consulted for {@link PropertySource} beans.
	 * 	May be {@code null}.
	 * @return Either a one-shot coerced value, or a {@code Supplier<T>} factory.
	 */
	public static Object resolve(String expression, Class<?> targetType, Type genericTargetType, String siteDescription,
			BeanStore beanStore) {
		var elementType = supplierElementType(targetType, genericTargetType);
		if (elementType != null) {
			// Build the re-evaluating string Supplier once. This is the same factory the bare
			// Supplier<String> path has always used.
			var stringSupplier = buildStringSupplier(expression, beanStore);
			if (elementType == String.class)
				return stringSupplier;
			// Supplier<T> for T != String: coerce per .get() using the same String -> T converter
			// machinery that plain @Value T uses (resolveCoerce -> Settings.toType). Per the
			// late-binding contract, the string is re-resolved AND re-coerced on every .get() — the
			// coerced value is never memoized. Only the converter lookup is cached (Settings.toType
			// memoizes the discovered String -> T function per type in its toTypeFunctions registry,
			// so each .get() pays the coercion but not the reflective converter discovery). A coercion
			// failure surfaces as a BeanCreationException (a RuntimeException) from .get(), naming the
			// @Value expression and the target type.
			final Class<?> et = elementType;
			return (Supplier<Object>) () -> resolveCoerce(stringSupplier.get(), et, expression, siteDescription);
		}
		return resolve(expression, targetType, siteDescription, beanStore);
	}

	/**
	 * Builds the re-evaluating {@code Supplier<String>} used by the {@code @Value Supplier<...>}
	 * autodetect path. Each {@link Supplier#get()} re-resolves the compiled template against a fresh
	 * {@link VarResolverSession}, so live config changes are reflected (the late-binding contract).
	 *
	 * @param expression The {@code @Value} expression. May be {@code null}.
	 * @param beanStore Caller-scoped {@link BeanStore} consulted for {@link PropertySource} beans. May be {@code null}.
	 * @return A threadsafe, re-evaluating {@code Supplier<String>}.
	 */
	private static Supplier<String> buildStringSupplier(String expression, BeanStore beanStore) {
		if (expression == null)
			return () -> null;
		var template = getCompiledTemplate(expression);
		// Literal templates fast-path: capture the resolved string once and hand back a
		// constant Supplier. Saves a per-.get() session allocation when the expression
		// has no variables (e.g. @Value("literal") Supplier<String>).
		if (template.isLiteral()) {
			var literal = template.resolve(VarResolver.DEFAULT.createSession());
			return () -> literal;
		}
		if (beanStore == null)
			return template.asSupplierWithFreshSessions(VarResolver.DEFAULT);
		// Capture the BeanStore by-reference so re-evaluating reads see the current state of
		// its PropertySource-typed beans (matches the "by-reference, not snapshot" contract).
		final BeanStore scope = beanStore;
		return () -> {
			var s = VarResolver.DEFAULT.createSession();
			var sources = scopedSources(scope);
			if (sources != null)
				s.bean(PropertySource[].class, sources);
			return template.resolve(s);
		};
	}

	/**
	 * Returns the caller-scoped {@link PropertySource} array for the supplied {@link BeanStore},
	 * or {@code null} if the store has no {@code PropertySource}-typed beans (matching the
	 * "no behavior change when no scoped sources are present" contract).
	 *
	 * <p>
	 * Walks {@link BeanStore#getBeansOfType(Class) beanStore.getBeansOfType(PropertySource.class)}.
	 * The returned array preserves the iteration order of {@code getBeansOfType} (parent-chain
	 * beans before local beans before overriding-parent beans, then sorted by
	 * {@code @Order/@Primary/@Bean.priority()}).
	 */
	@SuppressWarnings({
		"java:S1168" // Null is a meaningful "no scoped sources" signal that callers branch on to preserve the no-allocation / no-behavior-change contract; an empty array would alter behavior.
	})
	private static PropertySource[] scopedSources(BeanStore beanStore) {
		if (beanStore == null)
			return null;
		var map = beanStore.getBeansOfType(PropertySource.class);
		if (map.isEmpty())
			return null;
		return map.values().toArray(new PropertySource[0]);
	}

	/**
	 * Returns the {@code T} element class of a declared {@code Supplier<T>} injection site, or
	 * {@code null} if the declared type is not a coercible {@code Supplier<T>}.
	 *
	 * <p>
	 * Returns {@code null} (i.e. "not a typed Supplier site, fall through to one-shot resolution")
	 * for raw {@code Supplier}, non-{@code Supplier} parameterized types such as
	 * {@code Optional<Supplier<T>>} (whose raw type is {@code Optional}, not {@code Supplier} — the
	 * nesting is intentionally not specially handled), and type arguments that are not a concrete
	 * {@link Class} (wildcards / type variables). {@code Supplier<String>} returns {@code String.class},
	 * which the caller treats as the pre-existing pass-through behavior.
	 */
	private static Class<?> supplierElementType(Class<?> targetType, Type genericTargetType) {
		if (targetType != Supplier.class)
			return null;
		if (!(genericTargetType instanceof ParameterizedType pt))
			return null;
		if (pt.getRawType() != Supplier.class)
			return null;
		var args = pt.getActualTypeArguments();
		if (args.length != 1)
			return null;
		return args[0] instanceof Class<?> c ? c : null;
	}

	private static Object resolveCoerce(String resolved, Class<?> targetType, String expression, String siteDescription) {
		var effectiveType = box(targetType);

		if (resolved == null || resolved.isEmpty()) {
			if (targetType.isPrimitive())
				throw new BeanCreationException("Could not resolve required @Value('" + expression + "') for primitive site " + siteDescription);
			if (resolved == null)
				return null;
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
