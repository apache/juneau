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
package org.apache.juneau.bean.rfc7807;

import java.util.*;

/**
 * SPI seam for locale-aware translation of {@link Problem#getTitle()} / {@link Problem#getDetail()} (and any other
 * human-readable fields) before serialization.
 *
 * <p>
 * The default in-tree implementation is {@link #IDENTITY} &mdash; a pass-through that returns the input unchanged.
 * Server-side wiring consults the resource's bean store for a registered {@code ProblemLocalizationStrategy} bean,
 * and falls back to {@link #IDENTITY} when none is registered, so the seam is invisible at zero cost on the hot
 * path until a deliberate strategy is contributed.
 *
 * <h5 class='section'>Status</h5>
 * <p>
 * This interface is shipped in {@code juneau-bean-rfc7807} as the future-work hook for an eventual
 * {@code Messages}-driven (resource-bundle) translation pass. The reference {@code Messages} integration is
 * intentionally out of scope for the v1 RFC 7807 server wiring; the no-op {@link #IDENTITY} default keeps the
 * call site stable so a future implementation can be dropped in without changing the processor contract.
 *
 * <h5 class='section'>Example</h5>
 * <p class='bjava'>
 * 	<ja>@Bean</ja>
 * 	<jk>public</jk> ProblemLocalizationStrategy localize() {
 * 		<jk>return</jk> (<jv>p</jv>, <jv>locale</jv>) -&gt; {
 * 			<jc>// Future work: look up titles/details from a ResourceBundle keyed by locale.</jc>
 * 			<jk>return</jk> <jv>p</jv>;
 * 		};
 * 	}
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='jc'>{@link Problem}
 * 	<li class='jc'>{@link ProblemMapper}
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/RestServerProblemDetails">REST Server &mdash; RFC 7807 problem-details</a>
 * </ul>
 */
@FunctionalInterface
public interface ProblemLocalizationStrategy {

	/**
	 * Default no-op strategy. Returns the input {@link Problem} unchanged.
	 *
	 * <p>
	 * The server-side processor uses this constant as the fallback whenever no
	 * {@code ProblemLocalizationStrategy} bean is registered with the resource. Implementations that need to opt
	 * out of localization for a specific {@link Problem} can return this constant's behavior by simply returning
	 * the input.
	 */
	ProblemLocalizationStrategy IDENTITY = (problem, locale) -> problem;

	/**
	 * Translates the supplied {@link Problem} for the given {@link Locale}.
	 *
	 * <p>
	 * Implementations may mutate and return the input {@link Problem}, or return a fresh bean &mdash; the
	 * processor treats the return value as the bean to serialize. A {@code null} return is treated as "no opinion"
	 * and the original (pre-localization) {@link Problem} is used instead.
	 *
	 * @param problem The {@link Problem} to localize. Never <jk>null</jk>.
	 * @param locale The negotiated locale for the current request, or <jk>null</jk> when no locale is available.
	 * @return The localized {@link Problem} to emit, or the input {@code problem} for pass-through.
	 */
	Problem localize(Problem problem, Locale locale);
}
