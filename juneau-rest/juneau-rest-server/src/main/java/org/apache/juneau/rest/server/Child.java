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
package org.apache.juneau.rest.server;

import static java.lang.annotation.RetentionPolicy.*;

import java.lang.annotation.*;

import org.apache.juneau.marshall.httppart.*;
import org.apache.juneau.rest.server.converter.*;
import org.apache.juneau.rest.server.guard.*;

/**
 * Host-side setting seed for the {@link Rest#childrenDefs() @Rest(childrenDefs=...)} attribute.
 *
 * <p>
 * Declares a routed child class <b>and</b> lets the <i>host</i> seed a curated set of {@code @Rest}-level
 * settings onto that child's otherwise-isolated {@link RestContext} &mdash; the child-resource analog of
 * {@link Rest#mixinDefs() @Rest(mixinDefs=...)}. This is the host-side complement to
 * {@link Rest#children() @Rest(children=...)} (which takes bare classes and offers no seed hook).
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	<ja>@Rest</ja>(
 * 		childrenDefs=<ja>@Child</ja>(type=FooChild.<jk>class</jk>, maxInput=<js>"10M"</js>)
 * 	)
 * 	<jk>public class</jk> MyResource { ... }
 * </p>
 *
 * <h5 class='section'>Seed semantics</h5>
 * <p>
 * Unlike {@link Mixin @Mixin} overrides (which win over an <i>inherited</i> chain), children are
 * <b>isolated</b> from the host's resolution chain by design, so a {@code @Child} seed doesn't override
 * anything &mdash; it seeds settings onto an otherwise-isolated child context:
 * <ul>
 * 	<li><b>Additive-security</b> ({@code guards}, {@code converters}, {@code roleGuard}, {@code rolesDeclared})
 * 		&mdash; the host contributes, the child can't remove or weaken it (list-shaped members prepend; the two
 * 		role-based members AND-stack alongside the child's own value).
 * 	<li><b>Child-wins scalars</b> ({@code partSerializer}, {@code partParser},
 * 		{@code defaultCharset}, {@code maxInput}) &mdash; the seed is a default/fallback; the child's own explicit
 * 		{@code @Rest} declaration wins when present.
 * </ul>
 *
 * <p>
 * A child's own {@code @Rest(noInherit="&lt;property&gt;")} cuts the corresponding {@code @Child} seed too,
 * for both buckets above &mdash; the child always stays in full control of its own configuration.
 *
 * <p>
 * There is no {@code noInherit()} member on {@code @Child} itself (nothing to cut in an isolated context), and
 * no {@code path()}/{@code paths()} re-mount member (the child's own {@code @Rest(path)} stays authoritative).
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='ja'>{@link Rest#childrenDefs()}
 * 	<li class='ja'>{@link Rest#children()}
 * </ul>
 *
 * @since 10.0.0
 */
@Target({})
@Retention(RUNTIME)
public @interface Child {

	/**
	 * The child class to route.
	 *
	 * <p>
	 * Required. Equivalent to a bare entry in {@link Rest#children()}, but with the seed slots below.
	 *
	 * @return The child class.
	 */
	Class<?> type();

	//-----------------------------------------------------------------------------------------------------------------
	// Additive-security seed slots — host contributes, child can't remove or weaken.
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Host-seeded {@link Rest#guards() guards} for this child's endpoints (prepended before the child's own).
	 *
	 * @return The annotation value.
	 */
	Class<? extends RestGuard>[] guards() default {};

	/**
	 * Host-seeded {@link Rest#converters() converters} for this child's endpoints (prepended before the child's own).
	 *
	 * @return The annotation value.
	 */
	Class<? extends RestConverter>[] converters() default {};

	/**
	 * Host-seeded {@link Rest#roleGuard() roleGuard} for this child's endpoints (AND-stacks with the child's own).
	 *
	 * @return The annotation value.
	 */
	String roleGuard() default "";

	/**
	 * Host-seeded {@link Rest#rolesDeclared() rolesDeclared} for this child's endpoints (AND-stacks with the child's own).
	 *
	 * @return The annotation value.
	 */
	String rolesDeclared() default "";

	//-----------------------------------------------------------------------------------------------------------------
	// Child-wins scalar seed slots — seed is a default/fallback; the child's own explicit value wins.
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Host-seeded {@link Rest#partSerializer() partSerializer} default for this child's endpoints.
	 *
	 * @return The annotation value.
	 */
	Class<? extends HttpPartSerializer> partSerializer() default HttpPartSerializer.Void.class;

	/**
	 * Host-seeded {@link Rest#partParser() partParser} default for this child's endpoints.
	 *
	 * @return The annotation value.
	 */
	Class<? extends HttpPartParser> partParser() default HttpPartParser.Void.class;

	/**
	 * Host-seeded {@link Rest#defaultCharset() defaultCharset} default for this child's endpoints.
	 *
	 * @return The annotation value.
	 */
	String defaultCharset() default "";

	/**
	 * Host-seeded {@link Rest#maxInput() maxInput} default for this child's endpoints.
	 *
	 * @return The annotation value.
	 */
	String maxInput() default "";
}
