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

import static org.apache.juneau.commons.utils.Shorts.*;

/**
 * A {@link RequestBoundaryFilter} applying a {@link LoopbackBoundary}.
 *
 * <p>
 * Behaves exactly as {@link RequestBoundaryFilter} does; it exists so a loopback application's wiring names the
 * boundary it uses, and so {@link #boundary()} returns the {@link LoopbackBoundary} without a cast.  The constants
 * here are the same values as {@link RequestBoundaryFilter}'s.
 *
 * <h5 class='section'>Example (Spring Boot):</h5>
 * <p class='bjava'>
 * 	<ja>@Bean</ja>
 * 	<jk>public</jk> FilterRegistrationBean&lt;LoopbackBoundaryFilter&gt; boundary(LoopbackBoundary <jv>b</jv>) {
 * 		<jk>var</jk> <jv>reg</jv> = <jk>new</jk> FilterRegistrationBean&lt;&gt;(<jk>new</jk> LoopbackBoundaryFilter(<jv>b</jv>));
 * 		<jv>reg</jv>.addUrlPatterns(<js>"/*"</js>);
 * 		<jv>reg</jv>.setOrder(Ordered.<jsf>HIGHEST_PRECEDENCE</jsf>);
 * 		<jk>return</jk> <jv>reg</jv>;
 * 	}
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='jc'>{@link LoopbackBoundary}
 * 	<li class='jc'>{@link RequestBoundaryFilter}
 * 	<li class='jc'>{@link SynchronizerToken}
 * </ul>
 *
 * @since 10.0.0
 */
public class LoopbackBoundaryFilter extends RequestBoundaryFilter {

	/** Same value as {@link RequestBoundaryFilter#TOKEN_ATTRIBUTE}. */
	public static final String TOKEN_ATTRIBUTE = RequestBoundaryFilter.TOKEN_ATTRIBUTE;

	/** Same value as {@link RequestBoundaryFilter#HEADER_ATTRIBUTE}. */
	public static final String HEADER_ATTRIBUTE = RequestBoundaryFilter.HEADER_ATTRIBUTE;

	/** Same value as {@link RequestBoundaryFilter#REJECTION_HEADER}. */
	public static final String REJECTION_HEADER = RequestBoundaryFilter.REJECTION_HEADER;

	private final LoopbackBoundary loopback;

	/**
	 * Constructor.
	 *
	 * @param boundary The boundary to apply to every request.  Must not be <jk>null</jk>.
	 */
	public LoopbackBoundaryFilter(LoopbackBoundary boundary) {
		super(reqnn("boundary", boundary));
		this.loopback = boundary;
	}

	@Override /* RequestBoundaryFilter */
	public LoopbackBoundary boundary() { return loopback; }
}
