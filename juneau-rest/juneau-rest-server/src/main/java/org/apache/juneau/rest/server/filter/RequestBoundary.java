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
package org.apache.juneau.rest.server.filter;

import jakarta.servlet.http.*;

/**
 * Decides whether a request came from a page this application served, and supplies the CSRF token such a page embeds.
 *
 * <p>
 * {@link RequestBoundaryFilter} applies an implementation to every request and answers refusals; other callers that
 * judge a raw request directly (a message-bus session endpoint, say) call {@link #check(HttpServletRequest)} the same
 * way.
 *
 * <p>
 * Implementations judge a request on its <b>effective</b> method, via
 * {@link org.apache.juneau.rest.server.MethodSafety#effectiveMethod(String, String, java.util.function.UnaryOperator)},
 * so a safe request line that claims a write through a method override takes the write checks.
 *
 * <p>
 * Rejections are built only inside this package ({@link BoundaryResult}'s rejecting constructor is package-private),
 * so an implementation outside it can only {@linkplain BoundaryResult#ALLOWED allow}.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='jc'>{@link LoopbackBoundary}
 * 	<li class='jc'>{@link RequestBoundaryFilter}
 * </ul>
 *
 * @since 10.0.0
 */
public interface RequestBoundary {

	/**
	 * Applies the boundary to a request.
	 *
	 * @param req The request to check.  Must not be <jk>null</jk>.
	 * @return {@link BoundaryResult#ALLOWED} when every applicable check passed, else the first rejection encountered.
	 */
	BoundaryResult check(HttpServletRequest req);

	/**
	 * The name of the header a state-changing request must carry the CSRF token in.
	 *
	 * @return The CSRF header name.
	 */
	String csrfHeader();

	/**
	 * The CSRF token value a page served in answer to {@code req} must embed.
	 *
	 * @param req The request being answered.  Must not be <jk>null</jk>.
	 * @return The value to embed.
	 */
	String tokenFor(HttpServletRequest req);
}
