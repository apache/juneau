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

import static org.apache.juneau.commons.utils.AssertionUtils.*;
import static org.apache.juneau.commons.utils.Shorts.*;

import java.util.*;

import org.apache.juneau.commons.reflect.*;
import org.apache.juneau.commons.settings.*;

/**
 * Reusable, prefix-scoped whole-object config binder — usable with or without the
 * {@link ConfigProperties @ConfigProperties} annotation.
 *
 * <p>
 * For each non-static, non-synthetic, non-final field on the target, builds relaxed candidate keys of the form
 * {@code prefix.fieldName} (via {@link RelaxedPropertySource#candidates(String)}), resolves the first candidate
 * present in the given {@link Settings} chain, and converts it via {@link Settings#toType(String, Class)}. A key
 * absent from every candidate leaves the field's initializer value untouched ("bind-only-present" semantics). The
 * same bind-only-present rule governs a nested {@code @ConfigProperties} field that starts out <jk>null</jk>: the
 * nested type is only materialized if at least one key resolves anywhere under its sub-prefix; otherwise the field
 * is left <jk>null</jk>.
 *
 * <h5 class='section'>Usage</h5>
 * <p class='bjava'>
 * 	MyServiceProperties <jv>props</jv> = ConfigPropertiesBinder.<jsm>of</jsm>(<jk>new</jk> MyServiceProperties(), <js>"MyService"</js>).run();
 * </p>
 *
 * <h5 class='section'>Precedence (candidate-major)</h5>
 * <p>
 * Precedence is <b>candidate-major</b>: each spelling (in {@link RelaxedPropertySource#candidates(String)} order —
 * verbatim, upper-underscore, lower-dotted) is resolved through the full {@link Settings} source chain before the
 * next spelling is tried. This means a verbatim hit in a lower-precedence source can outrank a relaxed-spelling
 * override set on a higher-precedence source, if that override used a different spelling than the one that hit
 * first.
 * </p>
 *
 * <h5 class='section'>Caller-scoped sources</h5>
 * <p>
 * When a {@link #beanStore(BeanStore)} is supplied, its {@link PropertySource}-typed beans are
 * consulted <b>ahead of</b> the global {@link Settings} chain for each candidate key, but <b>after</b> any
 * {@link Settings#isOverridden(String) local/global test override} recorded on {@link #settings(Settings)} — a
 * {@code Settings.setLocal}/{@code setGlobal} override always wins over a scoped source, and among the scoped
 * sources the first one that reports the key present wins. This exactly mirrors the {@code @Value} resolution path
 * ({@link org.apache.juneau.commons.svl.vars.PropertyVar}), letting a caller layer overriding or supplementary
 * sources without mutating the shared {@link Settings#get() singleton}. Absent any scoped {@code PropertySource}
 * beans, resolution is identical to binding against {@link #settings(Settings)} alone.
 * </p>
 *
 * <h5 class='section'>Limitation — raw-type conversion only</h5>
 * <p>
 * Unlike {@link Value @Value}, conversion here is keyed on the field's <b>raw</b> {@link Class} (via
 * {@link FieldInfo#getFieldType()}), not its full generic {@link java.lang.reflect.Type}. Generic container types
 * such as {@code Optional<T>} or {@code Supplier<T>} are therefore <b>not</b> supported as {@code @ConfigProperties}
 * field types; use a plain field type instead.
 * </p>
 *
 * <h5 class='section'>Nested fields and circular nesting</h5>
 * <p>
 * A field whose type is itself {@code @ConfigProperties}-annotated is bound recursively under
 * {@code prefix.fieldName}. The set of nested types along the active recursion path is tracked, and re-entering a
 * type already on that path throws a {@link RuntimeException} instead of recursing indefinitely. A nested type
 * that needs to be materialized (see the bind-only-present rule above) must expose an accessible no-arg
 * constructor; if it doesn't, materialization throws a {@link RuntimeException} naming the offending field.
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/ConfigProperties">@ConfigProperties</a>
 * </ul>
 *
 * @param <T> The target type.
 * @since 10.0.0
 */
@SuppressWarnings({
	"java:S115" // Constants use ARG_lowerCamel convention to match the corresponding constructor parameter name (e.g., ARG_target → target).
})
public final class ConfigPropertiesBinder<T> {

	private static final String ARG_target = "target";
	private static final String ARG_prefix = "prefix";
	private static final String ARG_settings = "settings";
	private static final String ARG_validator = "validator";

	/**
	 * Creates a new binder for {@code target}'s fields under {@code prefix}.
	 *
	 * <p>
	 * Defaults (overridable via the fluent setters before calling {@link #run()}): {@link Settings#get()} for the
	 * settings chain, <jk>true</jk> for {@link #relaxed(boolean)}, and {@link ConfigPropertiesValidator#NO_OP} for
	 * {@link #validator(ConfigPropertiesValidator)}.
	 *
	 * @param <T> The target type.
	 * @param target The instance to populate. Must not be <jk>null</jk>.
	 * @param prefix The property-key prefix (no trailing dot). Must not be <jk>null</jk>.
	 * @return A new binder builder.
	 */
	public static <T> ConfigPropertiesBinder<T> of(T target, String prefix) {
		return new ConfigPropertiesBinder<>(target, prefix);
	}

	private final T target;
	private final String prefix;
	private Settings settings = Settings.get();
	private boolean relaxed = true;
	private ConfigPropertiesValidator validator = ConfigPropertiesValidator.NO_OP;
	private BeanStore beanStore;

	private ConfigPropertiesBinder(T target, String prefix) {
		this.target = assertArgNotNull(ARG_target, target);
		this.prefix = assertArgNotNull(ARG_prefix, prefix);
	}

	/**
	 * Sets the resolver chain to bind against (overridable for tests).
	 *
	 * @param value The settings chain. Must not be <jk>null</jk> when {@link #run()} is called.
	 * @return This object.
	 */
	public ConfigPropertiesBinder<T> settings(Settings value) {
		settings = value;
		return this;
	}

	/**
	 * Sets whether relaxed matching is enabled.
	 *
	 * @param value When <jk>true</jk>, tries {@link RelaxedPropertySource#candidates(String)} variants on a verbatim miss. Defaults to <jk>true</jk>.
	 * @return This object.
	 */
	public ConfigPropertiesBinder<T> relaxed(boolean value) {
		relaxed = value;
		return this;
	}

	/**
	 * Sets the post-bind validation hook invoked once on each fully-bound target.
	 *
	 * @param value The post-bind hook. Must not be <jk>null</jk> when {@link #run()} is called. Defaults to {@link ConfigPropertiesValidator#NO_OP}.
	 * @return This object.
	 */
	public ConfigPropertiesBinder<T> validator(ConfigPropertiesValidator value) {
		validator = value;
		return this;
	}

	/**
	 * Sets a caller-scoped {@link BeanStore} whose {@link PropertySource}-typed beans are consulted
	 * <b>ahead of</b> the global {@link Settings} chain when resolving each candidate key.
	 *
	 * <p>
	 * This mirrors the way {@code @Value} resolution layers a store's {@code PropertySource} beans over the
	 * global chain: {@link BeanStore#getBeansOfType(Class) beanStore.getBeansOfType(PropertySource.class)} walks
	 * parent&rarr;local, so a caller (e.g. a per-{@code RestContext} config bridge) can supply overriding or
	 * supplementary property sources without mutating the shared {@link Settings#get() singleton}. When the store
	 * has no {@code PropertySource} beans, behavior is identical to binding against {@link #settings(Settings)}
	 * alone (no allocation, no behavior change).
	 *
	 * @param value The caller-scoped bean store, or <jk>null</jk> for none (the default).
	 * @return This object.
	 */
	public ConfigPropertiesBinder<T> beanStore(BeanStore value) {
		beanStore = value;
		return this;
	}

	/**
	 * Runs the bind against {@code target}.
	 *
	 * @return The same target instance (for method chaining).
	 * @throws IllegalArgumentException If {@link #settings(Settings)} or {@link #validator(ConfigPropertiesValidator)}
	 * 	was set to <jk>null</jk>.
	 * @throws RuntimeException If circular {@code @ConfigProperties} nesting is detected, a property value could not
	 * 	be converted to its field's type, or a nested {@code @ConfigProperties} type could not be instantiated.
	 */
	public T run() {
		assertArgNotNull(ARG_settings, settings);
		assertArgNotNull(ARG_validator, validator);
		var scopedSources = scopedSources(beanStore);
		var path = new HashSet<Class<?>>();
		path.add(target.getClass());
		bind(target, prefix, settings, scopedSources, relaxed, validator, path);
		return target;
	}

	private static void bind(Object target, String prefix, Settings settings, PropertySource[] scopedSources, boolean relaxed, ConfigPropertiesValidator validator, Set<Class<?>> path) {
		var ci = ClassInfo.of(target);
		for (var field : ci.getAllFields()) {
			if (isBindable(field))
				bindField(target, prefix, field, settings, scopedSources, relaxed, validator, path);
		}
		validator.validate(target);
	}

	private static void bindField(Object target, String prefix, FieldInfo field, Settings settings, PropertySource[] scopedSources, boolean relaxed, ConfigPropertiesValidator validator, Set<Class<?>> path) {
		var key = prefix + "." + field.getName();
		var fieldType = field.getFieldType();
		if (fieldType.hasAnnotation(ConfigProperties.class)) {
			bindNested(target, key, field, fieldType, settings, scopedSources, relaxed, validator, path);
			return;
		}
		for (var candidate : candidateKeys(key, relaxed)) {
			var value = resolve(candidate, settings, scopedSources);
			if (value.isPresent()) {
				try {
					// Settings.toType only resolves wrapper types (e.g. Boolean, Integer) via reflection,
					// not raw primitive Class objects, so primitive fields must be unwrapped first.
					field.set(target, settings.toType(value.get(), fieldType.getWrapperIfPrimitive().inner()));
				} catch (RuntimeException e) {
					throw rex(e, "Could not bind property ''%s'' to field %s", candidate, field.getLabel());
				}
				return;
			}
		}
		// Bind-only-present: no candidate resolved anywhere in the chain, field initializer default stands.
	}

	private static void bindNested(Object target, String key, FieldInfo field, ClassInfo fieldType, Settings settings, PropertySource[] scopedSources, boolean relaxed, ConfigPropertiesValidator validator, Set<Class<?>> path) {
		if (! path.add(fieldType.inner()))
			throw rex("Circular @ConfigProperties nesting detected at %s", field.getLabel());
		try {
			var nested = field.get(target);
			if (nested == null) {
				// No-materialize contract: only instantiate the nested type if binding it would actually set something.
				if (! hasResolvableKey(fieldType, key, settings, scopedSources, relaxed, path))
					return;
				try {
					nested = fieldType.newInstance();
				} catch (RuntimeException e) {
					throw rex(e, "Could not instantiate nested @ConfigProperties field %s", field.getLabel());
				}
				bind(nested, key, settings, scopedSources, relaxed, validator, path);
				field.set(target, nested); // Materialize-if-absent: only assign when we created a new instance.
			} else {
				bind(nested, key, settings, scopedSources, relaxed, validator, path);
			}
		} finally {
			path.remove(fieldType.inner());
		}
	}

	/**
	 * Probes whether at least one bindable field under {@code type} — recursively through further nested
	 * {@code @ConfigProperties} fields — resolves to a present key under {@code prefix}, without binding
	 * anything. Used to decide whether a <jk>null</jk> nested field should be materialized.
	 */
	private static boolean hasResolvableKey(ClassInfo type, String prefix, Settings settings, PropertySource[] scopedSources, boolean relaxed, Set<Class<?>> path) {
		for (var field : type.getAllFields()) {
			if (isBindable(field)) {
				var key = prefix + "." + field.getName();
				var fieldType = field.getFieldType();
				if (fieldType.hasAnnotation(ConfigProperties.class)) {
					if (! path.add(fieldType.inner()))
						continue; // Already on the active path; the real bind() pass raises the circular-nesting error if this branch is ever taken.
					try {
						if (hasResolvableKey(fieldType, key, settings, scopedSources, relaxed, path))
							return true;
					} finally {
						path.remove(fieldType.inner());
					}
				} else {
					for (var candidate : candidateKeys(key, relaxed))
						if (resolve(candidate, settings, scopedSources).isPresent())
							return true;
				}
			}
		}
		return false;
	}

	/**
	 * A field is bindable if it's not static/final/synthetic and is not already owned by {@code @Value}/{@code @Inject}.
	 *
	 * <p>
	 * Skipping {@code @Value}/{@code @Inject} fields keeps whole-object config binding from silently overwriting a
	 * value already resolved by the injection engine (e.g. a {@code prefix.fieldName} key must not clobber a field
	 * that {@code @Value} owns).  {@code @Value}/{@code @Inject} field resolution is handled separately by
	 * {@link ClassInfo#inject(Object, BeanStore, Runnable)} before this binder runs.
	 */
	private static boolean isBindable(FieldInfo field) {
		return field.isNotFinal() && field.isNotStatic() && ! field.isSynthetic() && ! isValueOrInjectField(field);
	}

	private static boolean isValueOrInjectField(FieldInfo field) {
		return field.getAnnotations().stream().anyMatch(a -> JsrSupport.isValueAnnotation(a) || JsrSupport.isInjectAnnotation(a));
	}

	/**
	 * Resolves a single candidate key. Honors a {@link Settings#isOverridden(String) local/global test override} on
	 * {@code settings} first — matching {@link org.apache.juneau.commons.svl.vars.PropertyVar}'s
	 * override-before-scoped-sources contract, a {@code Settings.setLocal}/{@code setGlobal} override always wins
	 * over a scoped source — then consults the caller-scoped {@link PropertySource}s (if any) ahead of the global
	 * {@link Settings} chain, the first scoped source that reports the key present wins. Falls through to
	 * {@link Settings#get(String)} when neither an override nor a scoped source carries the key.
	 */
	private static Optional<String> resolve(String candidate, Settings settings, PropertySource[] scopedSources) {
		if (settings.isOverridden(candidate))
			return settings.getOverride(candidate);
		if (scopedSources != null) {
			for (var source : scopedSources) {
				if (source == null)
					continue; // getBeansOfType can yield a null element from a supplier returning null.
				var r = source.get(candidate);
				if (r.isPresent())
					return r.value();
			}
		}
		return settings.get(candidate).asOptional();
	}

	/**
	 * Returns the caller-scoped {@link PropertySource} array for the supplied {@link BeanStore}, or {@code null} if
	 * the store is <jk>null</jk> or carries no {@code PropertySource}-typed beans (matching the "no behavior change
	 * when no scoped sources are present" contract).  Iteration order is that of
	 * {@link BeanStore#getBeansOfType(Class)} (highest-priority scoped source first).
	 */
	@SuppressWarnings({
		"java:S1168" // Null is a meaningful "no scoped sources" signal that resolve() branches on to preserve the no-allocation / no-behavior-change contract; an empty array would be equivalent but wasteful.
	})
	private static PropertySource[] scopedSources(BeanStore beanStore) {
		if (beanStore == null)
			return null;
		var map = beanStore.getBeansOfType(PropertySource.class);
		if (map.isEmpty())
			return null;
		return map.values().toArray(new PropertySource[0]);
	}

	private static List<String> candidateKeys(String key, boolean relaxed) {
		return relaxed ? RelaxedPropertySource.candidates(key) : List.of(key);
	}
}
