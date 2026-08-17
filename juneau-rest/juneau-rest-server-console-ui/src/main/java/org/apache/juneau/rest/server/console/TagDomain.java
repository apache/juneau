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
package org.apache.juneau.rest.server.console;

import java.lang.annotation.*;

/**
 * Marks an <jk>enum</jk> type with the pill/badge "domain" {@link TagHtmlRender} resolves it under (the second CSS
 * class in the rendered {@code .tag.<domain>.<value>} markup, e.g. {@code .tag.status.released}).
 *
 * <p>
 * <b>Naming note (deviation from the TODO-361 plan text, recorded here rather than silently "fixed"):</b> the plan
 * and design documents write this annotation as {@code @Tag(domain=...)} &mdash; i.e. as if it shared its simple
 * name with the unrelated {@link Tag} <i>class</i> (the {@code Tag.of(domain, value)} static factory). That is not
 * something Java allows: an annotation type ({@code @interface}) cannot declare a static method, so a single type
 * named {@code Tag} cannot simultaneously be the {@code @Tag(domain=...)} annotation <b>and</b> expose
 * {@code Tag.of(...)}. This is a genuine plan-vs-language-semantics contradiction (verified by compiling a
 * `@interface` with a static method &mdash; {@code javac} rejects it outright), analogous in kind (though far
 * smaller in scope) to the Phase 5 FreeMarker-seam contradiction the plan's own r3 revision already fixed. Rather
 * than halt the build over an unambiguous, zero-risk naming split, this annotation is named {@code TagDomain}
 * (keeping the plan's heavily-referenced {@code Tag.of(...)} factory call form exactly as documented across
 * Phases 4-7); this deviation is called out in the phase manifest.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	<ja>@TagDomain</ja>(domain=<js>"priority"</js>)
 * 	<jk>public enum</jk> Priority { LOW, MEDIUM, HIGH }
 * </p>
 *
 * @since 10.0.0
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Inherited
public @interface TagDomain {

	/**
	 * The pill/badge domain (e.g. {@code "status"}, {@code "priority"}).
	 *
	 * <p>
	 * Defaults to the empty string, meaning "unset &mdash; fall back to {@link TagHtmlRender}'s default"
	 * (currently {@code "status"}).
	 *
	 * @return The domain name.
	 */
	String domain() default "";
}
