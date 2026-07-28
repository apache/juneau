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

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.*;

import java.lang.annotation.*;

/**
 * Declares a class as a prefix-scoped, whole-object config binding target — Juneau's analog of
 * Spring's {@code @ConfigurationProperties}.
 *
 * <p>
 * When a {@code @ConfigProperties}-annotated class is materialized through {@link BeanInstantiator},
 * its fields are populated from the {@link org.apache.juneau.commons.settings.Settings Settings} chain
 * under {@link #prefix()} via {@link ConfigPropertiesBinder}, and the bound instance is auto-registered
 * into the current {@link BeanStore}.
 *
 * <h5 class='section'>Binding order and lifecycle</h5>
 * <p>
 * The whole-object config bind runs <b>after</b> {@code @Value}/{@code @Inject} field-and-method injection and
 * <b>before</b> any {@link org.apache.juneau.commons.inject.PostConstruct @PostConstruct} method — so a
 * {@code @PostConstruct} method observes the fully-bound config fields. Fields that are themselves annotated with
 * {@code @Value} or {@code @Inject} are left to the injection engine and are never overwritten by config binding,
 * even when a matching {@code prefix.fieldName} key exists.
 *
 * <h5 class='section'>Caller-scoped sources</h5>
 * <p>
 * The bind consults any {@link org.apache.juneau.commons.settings.PropertySource PropertySource}-typed beans in the
 * active {@link BeanStore} <b>ahead of</b> the global {@link org.apache.juneau.commons.settings.Settings Settings}
 * chain, so a caller can layer overriding or supplementary sources without mutating the shared
 * {@code Settings.get()} singleton.
 *
 * <h5 class='section'>Not inherited</h5>
 * <p>
 * This annotation is <b>not</b> {@link java.lang.annotation.Inherited @Inherited}: the {@link BeanInstantiator}
 * trigger fires only when the <i>exact declared type</i> being instantiated carries the annotation — a subclass or
 * interface implementor of an annotated type does not itself trigger binding or auto-registration.
 *
 * <p>
 * This is distinct from the {@link ConfigPropertiesBinder} <b>nested-field</b> detection, which <i>is</i>
 * inheritance-aware: a field whose <i>declared type</i> is a subclass of an {@code @ConfigProperties}-annotated
 * type still triggers nested binding under the enclosing type's prefix, since the annotation lookup there walks the
 * declared field type's class hierarchy rather than requiring an exact-type match.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	<ja>@ConfigProperties</ja>(prefix = <js>"MyService"</js>)
 * 	<jk>public class</jk> MyServiceProperties {
 * 		<jk>public</jk> String <jv>host</jv> = <js>"localhost"</js>;
 * 		<jk>public int</jk> <jv>port</jv> = 8080;
 * 	}
 * </p>
 *
 * <p>
 * {@code @ConfigProperties} and {@link org.apache.juneau.commons.inject.Value @Value} coexist: {@code @Value}
 * resolves a single value; {@code @ConfigProperties} resolves a whole object. A class can use
 * {@code @Value} fields internally and separately be the target of another class's nested
 * {@code @ConfigProperties} bind.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/ConfigProperties">@ConfigProperties Annotation Basics</a>
 * </ul>
 *
 * @since 10.0.0
 */
@Documented
@Target(TYPE)
@Retention(RUNTIME)
public @interface ConfigProperties {

	/**
	 * The property-key prefix (no trailing dot).
	 *
	 * <p>
	 * Ignored when the annotated type appears as a nested field of another {@code @ConfigProperties} type — the
	 * enclosing type's prefix (extended with the field name) wins for the nested bind.
	 *
	 * @return The prefix.
	 */
	String prefix();

	/**
	 * Whether relaxed (camelCase / kebab-case / dotted / {@code SCREAMING_SNAKE_CASE}) key matching is enabled.
	 *
	 * <p>
	 * Ignored when the annotated type appears as a nested field of another {@code @ConfigProperties} type — the
	 * enclosing type's {@code relaxed} flag wins for the nested bind.
	 *
	 * @return <jk>true</jk> if relaxed matching is enabled. Defaults to <jk>true</jk>.
	 */
	boolean relaxed() default true;
}
