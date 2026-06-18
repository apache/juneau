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

import org.apache.juneau.marshall.encoders.*;
import org.apache.juneau.marshall.httppart.*;
import org.apache.juneau.marshall.serializer.*;
import org.apache.juneau.rest.server.arg.*;
import org.apache.juneau.rest.server.converter.*;
import org.apache.juneau.rest.server.guard.*;
import org.apache.juneau.rest.server.logger.*;
import org.apache.juneau.rest.server.processor.*;

/**
 * Rich mixin definition for the {@link Rest#mixinDefs() @Rest(mixinDefs=...)} attribute.
 *
 * <p>
 * Declares a mixin class <b>and</b> lets the <i>host</i> override selected {@code @Rest}-level settings for
 * that mixin's mixed-in endpoints &mdash; in one place, without subclassing or editing the mixin class.  This
 * is the host-side complement to {@link Rest#mixins() @Rest(mixins=...)} (which takes bare classes and offers
 * no override hook).
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	<ja>@Rest</ja>(
 * 		mixinDefs=<ja>@Mixin</ja>(type=FooMixin.<jk>class</jk>, guards=AdminGuard.<jk>class</jk>)
 * 	)
 * 	<jk>public class</jk> MyResource { ... }
 * </p>
 *
 * <p>
 * Here {@code AdminGuard} is applied to {@code FooMixin}'s endpoints as declared by the host, without
 * {@code FooMixin} having to declare it.
 *
 * <h5 class='section'>Override semantics</h5>
 * <p>
 * The override slots mirror the same {@code @Rest} attributes and resolve through the existing host&rarr;mixin
 * inheritance chain, exactly as if the value had been declared on the mixin class itself, but at the
 * most-derived position so a host override wins over the mixin class's own same-property declaration:
 * <ul>
 * 	<li><b>List-shaped</b> ({@code guards}, {@code converters}, {@code encoders}, {@code serializers},
 * 		{@code parsers}, {@code responseProcessors}, {@code restOpArgs}, default headers/attributes,
 * 		{@code produces}/{@code consumes}) &mdash; appended after the host chain that the mixin inherits.
 * 	<li><b>Replace-shaped</b> ({@code callLogger}, {@code partSerializer}, {@code partParser}, {@code debug},
 * 		{@code defaultAccept}/{@code defaultContentType}/{@code defaultCharset}, {@code maxInput},
 * 		{@code roleGuard}/{@code rolesDeclared}, {@code messages}) &mdash; the host override wins when set.
 * </ul>
 *
 * <p>
 * {@link #noInherit()} is the {@code @Mixin}'s own inheritance-cutoff list, applied to this mixin's
 * sub-context: a token here cuts the host&rarr;mixin inheritance walk for that property (same token set as
 * {@link Rest#noInherit()}).  The host override is authoritative <i>but</i> overridable by these
 * {@code noInherit} rules &mdash; e.g. {@code @Mixin(type=X.class, noInherit="guards")} removes the host's
 * guard chain from {@code X}'s endpoints entirely.
 *
 * <p>
 * {@link #path()}/{@link #paths()} re-mount the mixin's endpoints under host-chosen prefix(es).
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='ja'>{@link Rest#mixinDefs()}
 * 	<li class='ja'>{@link Rest#mixins()}
 * </ul>
 *
 * @since 10.0.0
 */
@Target({})
@Retention(RUNTIME)
public @interface Mixin {

	/**
	 * The mixin class to compose into the host resource.
	 *
	 * <p>
	 * Required.  Equivalent to a bare entry in {@link Rest#mixins()}, but with the override slots below.
	 *
	 * @return The mixin class.
	 */
	Class<?> type();

	//-----------------------------------------------------------------------------------------------------------------
	// Override slots — mirror the corresponding @Rest attribute signatures.
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Host-declared {@link Rest#guards() guards} override for this mixin's endpoints (appended after the host chain).
	 *
	 * @return The annotation value.
	 */
	Class<? extends RestGuard>[] guards() default {};

	/**
	 * Host-declared {@link Rest#roleGuard() roleGuard} override for this mixin's endpoints.
	 *
	 * @return The annotation value.
	 */
	String roleGuard() default "";

	/**
	 * Host-declared {@link Rest#rolesDeclared() rolesDeclared} override for this mixin's endpoints.
	 *
	 * @return The annotation value.
	 */
	String rolesDeclared() default "";

	/**
	 * Host-declared {@link Rest#converters() converters} override for this mixin's endpoints.
	 *
	 * @return The annotation value.
	 */
	Class<? extends RestConverter>[] converters() default {};

	/**
	 * Host-declared {@link Rest#encoders() encoders} override for this mixin's endpoints.
	 *
	 * @return The annotation value.
	 */
	Class<? extends Encoder>[] encoders() default {};

	/**
	 * Host-declared {@link Rest#serializers() serializers} override for this mixin's endpoints.
	 *
	 * @return The annotation value.
	 */
	Class<? extends Serializer>[] serializers() default {};

	/**
	 * Host-declared {@link Rest#parsers() parsers} override for this mixin's endpoints.
	 *
	 * @return The annotation value.
	 */
	Class<?>[] parsers() default {};

	/**
	 * Host-declared {@link Rest#responseProcessors() responseProcessors} override for this mixin's endpoints.
	 *
	 * @return The annotation value.
	 */
	Class<? extends ResponseProcessor>[] responseProcessors() default {};

	/**
	 * Host-declared {@link Rest#restOpArgs() restOpArgs} override for this mixin's endpoints.
	 *
	 * @return The annotation value.
	 */
	Class<? extends RestOpArg>[] restOpArgs() default {};

	/**
	 * Host-declared {@link Rest#callLogger() callLogger} override for this mixin's endpoints.
	 *
	 * @return The annotation value.
	 */
	Class<? extends CallLogger> callLogger() default CallLogger.Void.class;

	/**
	 * Host-declared {@link Rest#partSerializer() partSerializer} override for this mixin's endpoints.
	 *
	 * @return The annotation value.
	 */
	Class<? extends HttpPartSerializer> partSerializer() default HttpPartSerializer.Void.class;

	/**
	 * Host-declared {@link Rest#partParser() partParser} override for this mixin's endpoints.
	 *
	 * @return The annotation value.
	 */
	Class<? extends HttpPartParser> partParser() default HttpPartParser.Void.class;

	/**
	 * Host-declared {@link Rest#debug() debug} override for this mixin's endpoints.
	 *
	 * @return The annotation value.
	 */
	Debug debug() default @Debug;

	/**
	 * Host-declared {@link Rest#messages() messages} override for this mixin's endpoints.
	 *
	 * @return The annotation value.
	 */
	String messages() default "";

	/**
	 * Host-declared {@link Rest#defaultRequestHeaders() defaultRequestHeaders} override for this mixin's endpoints.
	 *
	 * @return The annotation value.
	 */
	String[] defaultRequestHeaders() default {};

	/**
	 * Host-declared {@link Rest#defaultResponseHeaders() defaultResponseHeaders} override for this mixin's endpoints.
	 *
	 * @return The annotation value.
	 */
	String[] defaultResponseHeaders() default {};

	/**
	 * Host-declared {@link Rest#defaultRequestAttributes() defaultRequestAttributes} override for this mixin's endpoints.
	 *
	 * @return The annotation value.
	 */
	String[] defaultRequestAttributes() default {};

	/**
	 * Host-declared {@link Rest#produces() produces} override for this mixin's endpoints.
	 *
	 * @return The annotation value.
	 */
	String[] produces() default {};

	/**
	 * Host-declared {@link Rest#consumes() consumes} override for this mixin's endpoints.
	 *
	 * @return The annotation value.
	 */
	String[] consumes() default {};

	/**
	 * Host-declared {@link Rest#defaultAccept() defaultAccept} override for this mixin's endpoints.
	 *
	 * @return The annotation value.
	 */
	String defaultAccept() default "";

	/**
	 * Host-declared {@link Rest#defaultContentType() defaultContentType} override for this mixin's endpoints.
	 *
	 * @return The annotation value.
	 */
	String defaultContentType() default "";

	/**
	 * Host-declared {@link Rest#defaultCharset() defaultCharset} override for this mixin's endpoints.
	 *
	 * @return The annotation value.
	 */
	String defaultCharset() default "";

	/**
	 * Host-declared {@link Rest#maxInput() maxInput} override for this mixin's endpoints.
	 *
	 * @return The annotation value.
	 */
	String maxInput() default "";

	/**
	 * Host-chosen single mount prefix for this mixin's endpoints (re-mount).
	 *
	 * @return The annotation value.
	 */
	String path() default "";

	/**
	 * Host-chosen multi-mount prefixes for this mixin's endpoints (re-mount).
	 *
	 * @return The annotation value.
	 */
	String[] paths() default {};

	/**
	 * Inheritance-cutoff list for this mixin's sub-context.
	 *
	 * <p>
	 * Each token names a property whose host&rarr;mixin inheritance walk is cut for this mixin (same token set
	 * as {@link Rest#noInherit()}).  Authoritative over the host override slots above.
	 *
	 * @return The annotation value.
	 */
	String[] noInherit() default {};
}
