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

import static org.apache.juneau.commons.utils.Utils.*;

import java.util.*;

import org.apache.juneau.commons.reflect.*;

/**
 * FQN-based support for JSR-style annotation detection.
 *
 * <p>
 * Centralizes all framework annotation-name matching for:
 * <ul>
 * 	<li>JSR-330 style injection annotations (<c>jakarta.inject</c> / <c>javax.inject</c>).
 * 	<li>JSR-250 lifecycle annotations (<c>jakarta.annotation</c> / <c>javax.annotation</c>).
 * 	<li>Spring's <c>Autowired</c> annotation.
 * </ul>
 *
 * <p>
 * Juneau intentionally matches by annotation class name so modules can interoperate with these
 * annotations without compile-time dependencies on their APIs.
 */
public final class JsrSupport {

	// JSR-330 / Spring.
	public static final String JUNEAU_INJECT = "org.apache.juneau.commons.inject.Inject";
	public static final String JAKARTA_INJECT = "jakarta.inject.Inject";
	public static final String JAVAX_INJECT = "javax.inject.Inject";
	public static final String SPRING_AUTOWIRED = "org.springframework.beans.factory.annotation.Autowired";
	public static final String JUNEAU_NAMED = "org.apache.juneau.commons.inject.Named";
	public static final String JAKARTA_NAMED = "jakarta.inject.Named";
	public static final String JAVAX_NAMED = "javax.inject.Named";
	public static final String JUNEAU_QUALIFIER = "org.apache.juneau.commons.inject.Qualifier";
	public static final String JAKARTA_QUALIFIER = "jakarta.inject.Qualifier";
	public static final String JAVAX_QUALIFIER = "javax.inject.Qualifier";
	public static final String JUNEAU_SINGLETON = "org.apache.juneau.commons.inject.Singleton";
	public static final String JAKARTA_SINGLETON = "jakarta.inject.Singleton";
	public static final String JAVAX_SINGLETON = "javax.inject.Singleton";
	public static final String JUNEAU_PROVIDER = "org.apache.juneau.commons.inject.Provider";
	public static final String JAKARTA_PROVIDER = "jakarta.inject.Provider";
	public static final String JAVAX_PROVIDER = "javax.inject.Provider";

	// JSR-250 lifecycle.
	public static final String JUNEAU_POSTCONSTRUCT = "org.apache.juneau.commons.inject.PostConstruct";
	public static final String JAKARTA_POSTCONSTRUCT = "jakarta.annotation.PostConstruct";
	public static final String JAVAX_POSTCONSTRUCT = "javax.annotation.PostConstruct";
	public static final String JUNEAU_PREDESTROY = "org.apache.juneau.commons.inject.PreDestroy";
	public static final String JAKARTA_PREDESTROY = "jakarta.annotation.PreDestroy";
	public static final String JAVAX_PREDESTROY = "javax.annotation.PreDestroy";

	// Config-value injection (Juneau + Spring).  Detected by FQN so juneau-commons stays free of a
	// compile-time Spring dependency, mirroring the SPRING_AUTOWIRED trick above.
	public static final String JUNEAU_VALUE = "org.apache.juneau.commons.inject.Value";
	public static final String SPRING_VALUE = "org.springframework.beans.factory.annotation.Value";

	// Jakarta Bean Validation 3.x (and the older javax. namespace).  Detected by FQN so juneau-commons
	// stays free of a compile-time jakarta.validation dependency; the opt-in marker is recognized whether
	// the consumer pulls in jakarta.validation-api 3.x, javax.validation:validation-api 2.x, or Spring's
	// own @Validated.
	public static final String JAKARTA_VALID = "jakarta.validation.Valid";
	public static final String JAVAX_VALID = "javax.validation.Valid";
	public static final String SPRING_VALIDATED = "org.springframework.validation.annotation.Validated";

	private JsrSupport() {}

	/**
	 * Returns <jk>true</jk> if the annotation is an inject marker.
	 */
	public static boolean isInjectAnnotation(AnnotationInfo<?> annotation) {
		var name = annotation.getName();
		return eqAny(name, JUNEAU_INJECT, JAKARTA_INJECT, JAVAX_INJECT, SPRING_AUTOWIRED);
	}

	/**
	 * Returns <jk>true</jk> if the annotation is a named qualifier.
	 */
	public static boolean isNamedAnnotation(AnnotationInfo<?> annotation) {
		var name = annotation.getName();
		return eqAny(name, JUNEAU_NAMED, JAKARTA_NAMED, JAVAX_NAMED);
	}

	/**
	 * Returns <jk>true</jk> if this annotation type itself is annotated with a qualifier marker.
	 */
	public static boolean isQualifierMeta(AnnotationInfo<?> annotation) {
		return Arrays.stream(annotation.annotationType().getAnnotations())
			.map(a -> a.annotationType().getName())
			.anyMatch(n -> eqAny(n, JUNEAU_QUALIFIER, JAKARTA_QUALIFIER, JAVAX_QUALIFIER));
	}

	/**
	 * Returns <jk>true</jk> if the annotation is a singleton marker.
	 */
	public static boolean isSingletonAnnotation(AnnotationInfo<?> annotation) {
		var name = annotation.getName();
		return eqAny(name, JUNEAU_SINGLETON, JAKARTA_SINGLETON, JAVAX_SINGLETON);
	}

	/**
	 * Returns <jk>true</jk> if the class is a supported provider interface.
	 */
	public static boolean isProviderType(Class<?> type) {
		return type != null && eqAny(type.getName(), JUNEAU_PROVIDER, JAKARTA_PROVIDER, JAVAX_PROVIDER);
	}

	/**
	 * Returns <jk>true</jk> if the annotation is a post-construct lifecycle marker.
	 */
	public static boolean isPostConstructAnnotation(AnnotationInfo<?> annotation) {
		var name = annotation.getName();
		return eqAny(name, JUNEAU_POSTCONSTRUCT, JAKARTA_POSTCONSTRUCT, JAVAX_POSTCONSTRUCT);
	}

	/**
	 * Returns <jk>true</jk> if the annotation is a pre-destroy lifecycle marker.
	 */
	public static boolean isPreDestroyAnnotation(AnnotationInfo<?> annotation) {
		var name = annotation.getName();
		return eqAny(name, JUNEAU_PREDESTROY, JAKARTA_PREDESTROY, JAVAX_PREDESTROY);
	}

	/**
	 * Reads qualifier value from an annotation when supported.
	 *
	 * <p>
	 * For <c>@Named</c>, returns the annotation value directly.  For qualifier-meta annotations,
	 * this returns the annotation's own <c>value()</c> attribute if present and string-typed.
	 */
	public static String qualifierValue(AnnotationInfo<?> annotation) {
		if (isNamedAnnotation(annotation))
			return annotation.getValue().orElse(null);
		if (isQualifierMeta(annotation))
			return annotation.getValue().orElse(null);
		return null;
	}

	/**
	 * Returns <jk>true</jk> if the annotation is a config-value marker.
	 *
	 * <p>
	 * Recognizes both Juneau's {@code @Value} and Spring's
	 * {@code org.springframework.beans.factory.annotation.Value} by FQN — no compile-time Spring
	 * dependency in {@code juneau-commons}.
	 *
	 * @param annotation The annotation to inspect.
	 * @return <jk>true</jk> if {@code annotation} is one of the recognized {@code @Value} variants.
	 */
	public static boolean isValueAnnotation(AnnotationInfo<?> annotation) {
		var name = annotation.getName();
		return eqAny(name, JUNEAU_VALUE, SPRING_VALUE);
	}

	/**
	 * Returns the configuration expression carried by a {@code @Value} annotation, or {@code null}
	 * if the annotation is not a recognized {@code @Value}.
	 *
	 * <p>
	 * Both Juneau's {@code @Value} and Spring's {@code @Value} expose a single {@code String value()}
	 * attribute, so a uniform {@code getValue()} lookup against the annotation works for either.
	 *
	 * @param annotation The annotation to inspect.
	 * @return The {@code value()} string, or {@code null} if {@code annotation} is not a {@code @Value}.
	 */
	public static String valueExpression(AnnotationInfo<?> annotation) {
		if (! isValueAnnotation(annotation))
			return null;
		return annotation.getValue().orElse(null);
	}

	/**
	 * Returns <jk>true</jk> if the annotation is a Jakarta Bean Validation opt-in marker.
	 *
	 * <p>
	 * Recognizes Jakarta 3.x ({@code jakarta.validation.Valid}), the older Javax 2.x equivalent
	 * ({@code javax.validation.Valid}), and Spring's {@code @Validated} group-selector by FQN &mdash; no
	 * compile-time Jakarta Validation or Spring dependency in {@code juneau-commons}.
	 *
	 * @param annotation The annotation to inspect.
	 * @return <jk>true</jk> if {@code annotation} is one of the recognized validation opt-in markers.
	 */
	public static boolean isValidAnnotation(AnnotationInfo<?> annotation) {
		var name = annotation.getName();
		return eqAny(name, JAKARTA_VALID, JAVAX_VALID, SPRING_VALIDATED);
	}
}
