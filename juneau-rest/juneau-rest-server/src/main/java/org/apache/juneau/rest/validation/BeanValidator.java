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
package org.apache.juneau.rest.validation;

import java.util.*;
import java.util.concurrent.atomic.*;
import java.util.logging.*;

import org.apache.juneau.commons.inject.*;
import org.apache.juneau.commons.reflect.*;

import jakarta.validation.*;

/**
 * Static dispatcher for the optional Jakarta Bean Validation 3.x integration.
 *
 * <p>
 * Acts as a thin shim between Juneau's REST argument resolvers ({@code ContentArg}, {@code FormDataArg},
 * {@code RequestBeanArg}) and the {@code jakarta.validation.Validator} engine. Per the package-level contract
 * (see {@link org.apache.juneau.rest.validation package overview}), <b>validation is off by default and never
 * runs automatically</b>: callers query {@link #isValidationRequested(ParameterInfo)} once at arg-resolver
 * construction time, then conditionally invoke {@link #validate(Object, BeanStore)} per request only when an
 * explicit {@code @jakarta.validation.Valid} (or equivalent) marker is present.
 *
 * <h5 class='topic'>Validator resolution order</h5>
 * <ol>
 * 	<li>A user-supplied {@code Validator} bean visible from the supplied {@link BeanStore} &mdash; typically
 * 		contributed via a {@code @Bean public Validator validator()} factory on a {@code @Rest}-annotated
 * 		resource. This is the recommended path when the application needs custom message interpolators,
 * 		group sequences, or constraint-validator factories.
 * 	<li>A lazily-built JVM-wide default obtained from
 * 		{@link Validation#buildDefaultValidatorFactory()}. Cached after the first successful build so the
 * 		factory cost is paid at most once per JVM.
 * 	<li>{@code null} &mdash; signals graceful degradation when neither a bean-store entry nor a runtime
 * 		provider is available. {@link #validate(Object, BeanStore)} returns the input bean unchanged in this
 * 		case and logs a single {@code WARNING} per JVM so misconfigured deployments are visible without
 * 		flooding the log.
 * </ol>
 *
 * <h5 class='topic'>Why static dispatch?</h5>
 * <p>
 * Each REST argument resolver is built once per {@code @RestOp} method at servlet startup, then invoked on
 * every request. By keeping the validator lookup off the arg-resolver constructor and inside this class's
 * cached static accessor, the cost of locating the {@code Validator} is paid once per JVM rather than once
 * per request &mdash; and resolvers for {@code @RestOp} methods that never opt in pay nothing at all (the
 * static initializer doesn't run until the first call to {@link #validate(Object, BeanStore)}).
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='jc'>{@link ValidationException}
 * 	<li class='jc'>{@link ValidationViolation}
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/RestServerValidation">REST Server &mdash; Jakarta Validation</a>
 * </ul>
 *
 * @since 9.5.0
 */
public final class BeanValidator {

	private static final Logger LOG = Logger.getLogger(BeanValidator.class.getName());

	/**
	 * Holds the lazily-resolved JVM-wide default {@link Validator} (built once on first need from
	 * {@link Validation#buildDefaultValidatorFactory()}). {@code null} until first successful build.
	 */
	private static final AtomicReference<Validator> DEFAULT_VALIDATOR = new AtomicReference<>();

	/**
	 * Flips to {@code true} once we've discovered &mdash; via either a {@link ValidationException} from the
	 * provider lookup or a {@link Throwable} from {@link Validation#buildDefaultValidatorFactory()} &mdash;
	 * that no runtime validator engine is reachable on this classpath. Short-circuits subsequent calls so we
	 * don't repeatedly pay the failed-lookup cost.
	 */
	private static final AtomicBoolean DEFAULT_PROVIDER_UNAVAILABLE = new AtomicBoolean();

	/** One-shot guard so the &quot;no provider on classpath&quot; warning only logs once per JVM. */
	private static final AtomicBoolean MISSING_PROVIDER_LOGGED = new AtomicBoolean();

	private BeanValidator() {}

	/**
	 * Returns <jk>true</jk> if the supplied parameter carries any of the recognized validation opt-in
	 * markers (currently {@code jakarta.validation.Valid}, {@code javax.validation.Valid}, or Spring's
	 * {@code @Validated}).
	 *
	 * <p>
	 * Detection is purely FQN-based via {@link JsrSupport#isValidAnnotation(AnnotationInfo)} &mdash; no
	 * compile-time dependency on the Jakarta Validation or Spring annotation classes themselves is required
	 * to recognize the marker.
	 *
	 * <p>
	 * Arg resolvers should call this exactly once at construction time and cache the result; the per-request
	 * hot path then becomes a single boolean check before deciding whether to call
	 * {@link #validate(Object, BeanStore)}.
	 *
	 * @param paramInfo The Java method parameter being inspected. Must not be <jk>null</jk>.
	 * @return <jk>true</jk> if the parameter has been opted into validation.
	 */
	public static boolean isValidationRequested(ParameterInfo paramInfo) {
		return paramInfo.getAnnotations().stream().anyMatch(JsrSupport::isValidAnnotation);
	}

	/**
	 * Runs Jakarta Bean Validation against the supplied bean and throws a {@link ValidationException} if any
	 * constraints are violated.
	 *
	 * <p>
	 * Resolution order for the {@link Validator} is described on the class javadoc. When no validator can
	 * be obtained (no bean-store entry, no runtime provider), this method logs a one-shot warning and
	 * returns {@code bean} unchanged &mdash; preserving the off-by-default contract even when an arg
	 * resolver was constructed with {@code @Valid} but the deployment forgot to ship a provider.
	 *
	 * <p>
	 * A {@code null} {@code bean} returns {@code null} without invoking the validator (Jakarta's
	 * {@code validator.validate(null)} throws {@code IllegalArgumentException}, which would just produce an
	 * unhelpful 500 instead of the natural &quot;missing body&quot; 400 the upstream arg resolver already
	 * surfaces).
	 *
	 * @param <T> The bean type being validated.
	 * @param bean The resolved bean to validate. May be <jk>null</jk>.
	 * @param beanStore The op-session bean store consulted for a user-supplied {@link Validator}. May be
	 * 	<jk>null</jk> (treated as &quot;no user override&quot;).
	 * @return The input {@code bean} (unchanged).
	 * @throws ValidationException If one or more constraints are violated.
	 */
	public static <T> T validate(T bean, BeanStore beanStore) {
		if (bean == null)
			return null;
		var validator = resolveValidator(beanStore);
		if (validator == null) {
			warnMissingProviderOnce();
			return bean;
		}
		var violations = validator.validate(bean);
		if (violations.isEmpty())
			return bean;
		throw new ValidationException(toViolationList(violations));
	}

	/**
	 * Test/diagnostic hook &mdash; resets the JVM-wide default-validator cache so the next call to
	 * {@link #validate(Object, BeanStore)} re-resolves the provider. Not part of the public contract;
	 * intended for unit tests that simulate provider-missing scenarios.
	 */
	public static void resetCachedDefaultForTesting() {
		DEFAULT_VALIDATOR.set(null);
		DEFAULT_PROVIDER_UNAVAILABLE.set(false);
		MISSING_PROVIDER_LOGGED.set(false);
	}

	/**
	 * Test/diagnostic hook &mdash; forces the &quot;no provider on classpath&quot; code path. Not part of
	 * the public contract; intended for unit tests that need to assert the graceful-degradation behavior
	 * without actually removing the Hibernate Validator JAR from the test classpath.
	 *
	 * <p>
	 * After calling this, {@link #validate(Object, BeanStore)} will behave as if no Jakarta Bean
	 * Validation provider is reachable: it short-circuits to a no-op and emits the one-shot
	 * &quot;missing provider&quot; warning. Call {@link #resetCachedDefaultForTesting()} afterwards to
	 * restore normal lookup.
	 */
	public static void simulateProviderMissingForTesting() {
		DEFAULT_VALIDATOR.set(null);
		DEFAULT_PROVIDER_UNAVAILABLE.set(true);
		MISSING_PROVIDER_LOGGED.set(false);
	}

	private static Validator resolveValidator(BeanStore beanStore) {
		if (beanStore != null) {
			var fromStore = beanStore.getBean(Validator.class).orElse(null);
			if (fromStore != null)
				return fromStore;
		}
		return getDefaultValidator();
	}

	@SuppressWarnings({
		"resource" // ValidatorFactory is intentionally not closed; it is cached for the process lifetime.
	})
	private static Validator getDefaultValidator() {
		if (DEFAULT_PROVIDER_UNAVAILABLE.get())
			return null;
		var cached = DEFAULT_VALIDATOR.get();
		if (cached != null)
			return cached;
		try {
			var built = Validation.buildDefaultValidatorFactory().getValidator();
			DEFAULT_VALIDATOR.compareAndSet(null, built);
			return DEFAULT_VALIDATOR.get();
		} catch (Throwable t) {  // NOSONAR: ValidationException, NoClassDefFoundError, LinkageError all in scope here.
			DEFAULT_PROVIDER_UNAVAILABLE.set(true);
			return null;
		}
	}

	private static void warnMissingProviderOnce() {
		if (MISSING_PROVIDER_LOGGED.compareAndSet(false, true)) {
			LOG.log(Level.WARNING,
				"Jakarta Bean Validation was requested via @Valid on a REST argument, but no jakarta.validation.Validator " +
				"bean is registered and no runtime provider (e.g. org.hibernate.validator:hibernate-validator + " +
				"jakarta.el implementation) is on the classpath. Validation will be silently skipped for this and " +
				"subsequent requests. Add a provider dependency or register a Validator bean to enable validation.");
		}
	}

	private static List<ValidationViolation> toViolationList(Set<? extends ConstraintViolation<?>> violations) {
		var result = new ArrayList<ValidationViolation>(violations.size());
		for (var cv : violations) {
			result.add(new ValidationViolation(
				cv.getPropertyPath() == null ? null : cv.getPropertyPath().toString(),
				cv.getMessage(),
				cv.getConstraintDescriptor() == null || cv.getConstraintDescriptor().getAnnotation() == null
					? null
					: cv.getConstraintDescriptor().getAnnotation().annotationType().getSimpleName()
			));
		}
		// Stable ordering — Jakarta returns a Set, but tests assert on payload shape so we sort by path+message.
		result.sort((a, b) -> {
			var ap = a.getPath() == null ? "" : a.getPath();
			var bp = b.getPath() == null ? "" : b.getPath();
			var c = ap.compareTo(bp);
			if (c != 0)
				return c;
			var am = a.getMessage() == null ? "" : a.getMessage();
			var bm = b.getMessage() == null ? "" : b.getMessage();
			return am.compareTo(bm);
		});
		return result;
	}
}
