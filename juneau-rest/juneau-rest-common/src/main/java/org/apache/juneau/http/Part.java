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
package org.apache.juneau.http;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.*;

import java.lang.annotation.*;

/**
 * REST request <c>multipart/form-data</c> part annotation.
 *
 * <p>
 * Identifies a method parameter on a {@link org.apache.juneau.http.remote.Remote}-proxy method (which must also be
 * marked {@link org.apache.juneau.http.remote.Multipart @Multipart}) as a single part of a {@code multipart/form-data}
 * request body &mdash; the Retrofit <c>@Part</c> pattern.  The engine adapts each {@code @Part} parameter into the
 * streaming {@link org.apache.juneau.http.entity.MultipartBody}.
 *
 * <h5 class='topic'>Accepted part-source types</h5>
 * <p>
 * The bound argument type determines how the part body is produced:
 * <ul class='spaced-list'>
 * 	<li>{@link CharSequence}/{@link String} (and other scalars such as numbers/booleans) &mdash; sent as a text field.
 * 	<li><c><jk>byte</jk>[]</c> &mdash; sent verbatim (repeatable).
 * 	<li>{@link java.io.File} &mdash; <b>streamed</b> from disk; the {@link #fileName()} defaults to the file's name.
 * 	<li>{@link java.io.InputStream} &mdash; <b>streamed</b> (one-shot; makes the request body non-repeatable).
 * 	<li>{@link java.io.Reader} &mdash; <b>streamed</b> as UTF-8 (one-shot; not drained during request building).
 * 	<li>An {@link HttpBody} &mdash; used directly.
 * 	<li>Any other object (a bean) &mdash; serialized with the client's default serializer (e.g. JSON).
 * </ul>
 *
 * <p>
 * A <jk>null</jk> argument contributes no part.
 *
 * <h5 class='figure'>Example:</h5>
 * <p class='bjava'>
 * 	<ja>@RemotePost</ja>(<js>"/upload"</js>)
 * 	<ja>@Multipart</ja>
 * 	String upload(
 * 		<ja>@Part</ja>(<js>"title"</js>) String <jv>title</jv>,
 * 		<ja>@Part</ja>(name=<js>"attachment"</js>, fileName=<js>"report.pdf"</js>, contentType=<js>"application/pdf"</js>) <jk>byte</jk>[] <jv>data</jv>);
 * </p>
 *
 * <h5 class='section'>Notes:</h5><ul>
 * 	<li class='note'>
 * 		Valid only on a method also annotated {@link org.apache.juneau.http.remote.Multipart @Multipart}; a
 * 		{@code @Part} parameter on a non-multipart method is rejected at proxy-build time.
 * 	<li class='note'>
 * 		Honored by the next-generation engine (<c>RestClient.remote(...)</c>) only; the classic engine currently
 * 		ignores this annotation.
 * </ul>
 *
 * <p>
 * <b>Beta — API subject to change:</b> This type is part of the next-generation REST client and HTTP stack
 * ({@code org.apache.juneau.marshall.ng.*}).
 * It is not API-frozen: binary- and source-incompatible changes may appear in the <b>next major</b> Juneau release
 * (and possibly earlier).
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/NextGenRestClient">juneau-ng REST client</a>
 * </ul>
 *
 * @since 9.2.1
 */
@Documented
@Target(PARAMETER)
@Retention(RUNTIME)
@Inherited
public @interface Part {

	/**
	 * The part (form-field) name.
	 *
	 * <p>
	 * When blank, falls back to {@link #value()}, then to the reflected parameter name (requires the
	 * <c>-parameters</c> compiler flag).
	 *
	 * @return The annotation value.
	 */
	String name() default "";

	/**
	 * A synonym for {@link #name()}.
	 *
	 * <p>
	 * Allows shortened notation when only the part name is specified: <c><ja>@Part</ja>(<js>"title"</js>)</c>.
	 *
	 * @return The annotation value.
	 */
	String value() default "";

	/**
	 * The filename advertised in the part's {@code Content-Disposition} header.
	 *
	 * <p>
	 * When blank, no {@code filename} is emitted &mdash; except for a {@link java.io.File} argument, whose filename
	 * defaults to {@link java.io.File#getName()}.
	 *
	 * @return The annotation value.
	 */
	String fileName() default "";

	/**
	 * The per-part {@code Content-Type} (e.g. {@code "application/pdf"}, {@code "application/json"}).
	 *
	 * <p>
	 * When blank, a sensible default is used based on the argument type (e.g. {@code text/plain; charset=UTF-8} for
	 * text fields, the serializer's media type for beans, {@code application/octet-stream} for binary sources).
	 *
	 * @return The annotation value.
	 */
	String contentType() default "";
}
