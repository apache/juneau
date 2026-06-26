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
package org.apache.juneau.http.remote;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.*;

import java.lang.annotation.*;

/**
 * REST proxy method-level <c>multipart/form-data</c> marker annotation.
 *
 * <p>
 * Marks a {@link Remote}-proxy method whose request body is a {@code multipart/form-data} message (RFC 7578)
 * assembled from the method's {@link org.apache.juneau.http.Part @Part}-annotated parameters &mdash; the
 * Retrofit <c>@Multipart</c> pattern.  Each {@code @Part} parameter contributes one part (a text field, a
 * file/stream/byte-array upload, or a serialized bean); the engine adapts those parameters into the streaming
 * {@link org.apache.juneau.http.entity.MultipartBody} and the {@code multipart/form-data} {@code Content-Type}
 * (with its generated boundary) is applied automatically.
 *
 * <h5 class='figure'>Example:</h5>
 * <p class='bjava'>
 * 	<ja>@Remote</ja>(path=<js>"/api"</js>)
 * 	<jk>public interface</jk> UploadService {
 * 		<ja>@RemotePost</ja>(<js>"/upload"</js>)
 * 		<ja>@Multipart</ja>
 * 		String upload(
 * 			<ja>@Part</ja>(<js>"title"</js>) String <jv>title</jv>,
 * 			<ja>@Part</ja>(name=<js>"attachment"</js>, contentType=<js>"application/pdf"</js>) File <jv>report</jv>);
 * 	}
 * </p>
 *
 * <h5 class='section'>Body-mode exclusivity:</h5>
 * <p>
 * A method is either <i>multipart</i> ({@code @Multipart} + one or more {@link org.apache.juneau.http.Part @Part}
 * parameters) or <i>single-body</i> ({@link org.apache.juneau.http.Content @Content}), never both.  Declaring both
 * on the same method &mdash; or declaring {@code @Multipart} with no {@code @Part} parameters, or a {@code @Part}
 * parameter without {@code @Multipart} &mdash; is rejected at proxy-build time with an
 * {@link IllegalArgumentException}.
 *
 * <h5 class='section'>Notes:</h5><ul>
 * 	<li class='note'>
 * 		<b>Escape hatch (Option B):</b> for advanced cases you can still hand-assemble a
 * 		{@link org.apache.juneau.http.entity.MultipartBody} and pass it as a single
 * 		{@link org.apache.juneau.http.Content @Content} body without {@code @Multipart}; that path is unchanged.
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
 * 	<li class='extlink'><a class="doclink" href="https://www.rfc-editor.org/rfc/rfc7578">RFC 7578 — multipart/form-data</a>
 * </ul>
 *
 * @since 9.2.1
 */
@Documented
@Target(METHOD)
@Retention(RUNTIME)
@Inherited
public @interface Multipart {
}
