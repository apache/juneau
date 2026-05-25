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
 * Resolves a configuration value into a constructor parameter, setter, or field.
 *
 * <p>
 * The {@code value()} string is passed through Juneau's {@code VarResolver} (so
 * {@code ${...}} / {@code $P{...}} placeholders are expanded against
 * {@link org.apache.juneau.commons.settings.Settings#get() Settings.get()}), then coerced to the
 * target Java type via {@link org.apache.juneau.commons.settings.Settings#toType(String, Class)}.
 *
 * <h5 class='section'>Examples:</h5>
 * <p class='bjava'>
 * 	<jc>// Field injection with default.</jc>
 * 	<ja>@Value</ja>(<js>"${db.timeout.ms:5000}"</js>)
 * 	<jk>private int</jk> <jv>timeoutMs</jv>;
 *
 * 	<jc>// Constructor-parameter injection.</jc>
 * 	<ja>@Inject</ja>
 * 	<jk>public</jk> OrdersResource(
 * 		<ja>@Value</ja>(<js>"${app.name:orders}"</js>) String <jv>appName</jv>,
 * 		<ja>@Value</ja>(<js>"${app.start}"</js>) Instant <jv>start</jv>
 * 	) { ... }
 * </p>
 *
 * <h5 class='section'>Resolution order:</h5>
 * <ol>
 * 	<li>Thread-local override ({@link org.apache.juneau.commons.settings.Settings#setLocal Settings.setLocal})
 * 	<li>Global override ({@link org.apache.juneau.commons.settings.Settings#setGlobal Settings.setGlobal})
 * 	<li>Property sources in reverse insertion order — this is where Juneau {@code Config},
 * 		Spring {@code Environment}, and any other registered {@code PropertySource} participate.
 * 	<li>System properties
 * 	<li>System environment variables
 * </ol>
 *
 * <p>
 * Spring's {@code org.springframework.beans.factory.annotation.Value} annotation is honored identically
 * via FQN-based detection in {@link JsrSupport}; no compile-time Spring dependency is introduced in
 * {@code juneau-commons}.
 *
 * <h5 class='section'>Notes:</h5><ul>
 * 	<li class='note'>{@code @Value} and {@code @Inject} on the same site is invalid and throws
 * 		a {@code BeanCreationException}. {@code @Value} resolves strings/primitives; {@code @Inject}
 * 		resolves beans.
 * 	<li class='note'>If the expression resolves to {@code null} and the target type is a primitive,
 * 		a {@code BeanCreationException} is thrown.  For reference types, {@code null} is injected.
 * </ul>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/ValueAnnotationBasics">@Value Annotation Basics</a>
 * </ul>
 */
@Documented
@Target({CONSTRUCTOR, METHOD, FIELD, PARAMETER})
@Retention(RUNTIME)
public @interface Value {

	/**
	 * The configuration expression.
	 *
	 * <p>
	 * Typically uses the {@code ${key}} or {@code ${key:default}} placeholder syntax that is routed
	 * through {@code VarResolver} and {@link org.apache.juneau.commons.settings.Settings Settings}.
	 * Plain literals (no {@code $}) are passed through unchanged and still type-coerced.
	 *
	 * @return The configuration expression.
	 */
	String value();
}
