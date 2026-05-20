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

import java.util.*;

/**
 * An {@link HttpBody} that carries additional headers (such as {@code Content-Type},
 * {@code Content-Disposition}, or {@code Content-Language}) alongside the body.
 *
 * <p>
 * Mirrors the semantics of {@code HttpResource} from {@code juneau-rest-common-classic} without the
 * Apache HttpCore dependency. The default immutable implementation is
 * {@link org.apache.juneau.http.resource.HttpResourceBean}.
 *
 * <p>
 * <b>Beta — API subject to change:</b> This type is part of the next-generation REST client and HTTP stack
 * ({@code org.apache.juneau.ng.*}). It is not API-frozen: binary- and source-incompatible changes may appear in
 * the <b>next major</b> Juneau release (and possibly earlier).
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/juneau-ng-rest-client">juneau-ng REST client</a>
 * </ul>
 *
 * @since 9.5.0
 */
public interface HttpResource extends HttpBody {

	/**
	 * Returns the headers associated with this resource.
	 *
	 * <p>
	 * The returned list may include body-specific headers ({@code Content-Type}, {@code Content-Length})
	 * or arbitrary application headers (e.g. {@code Cache-Control}, {@code Content-Disposition}).
	 *
	 * @return The headers. Never <jk>null</jk>.
	 */
	List<HttpHeader> getHeaders();
}
