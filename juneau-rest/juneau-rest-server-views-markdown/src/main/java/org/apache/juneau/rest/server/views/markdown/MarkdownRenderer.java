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
package org.apache.juneau.rest.server.views.markdown;

import java.util.*;

/**
 * A pluggable markdown-to-HTML renderer: hand it a markdown string, get back an HTML fragment.
 *
 * <p>
 * This is the extension point of the optional markdown module &mdash; the toolkit's answer to "the way
 * {@code ViewDef} turns a list of beans into a themed table, hand me a markdown string and give me back HTML".
 * The interface is deliberately library-neutral: a single {@link #toHtml(String)} method, so any parser can sit
 * behind it.  The shipped default is {@link CommonmarkMarkdownRenderer} (commonmark-java with the GFM tables
 * extension enabled), but a consumer can swap in their own parser without the toolkit knowing which library won.
 *
 * <h5 class='section'>Selecting an implementation:</h5>
 * <p>
 * {@link #resolve()} returns the active renderer via the standard {@link ServiceLoader} SPI mechanism: if a
 * consumer registers their own provider (a {@code META-INF/services/}
 * {@code org.apache.juneau.rest.server.views.markdown.MarkdownRenderer} file naming their implementation class),
 * that provider wins; otherwise the built-in commonmark-java default is used.  The default is intentionally
 * <b>not</b> registered as a service, so a single consumer-supplied provider is always unambiguous.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	<jc>// Render markdown to an HTML fragment using whichever renderer is active.</jc>
 * 	MarkdownRenderer <jv>renderer</jv> = MarkdownRenderer.<jsm>resolve</jsm>();
 * 	String <jv>html</jv> = <jv>renderer</jv>.toHtml(<js>"# Runbook\n\nStep one."</js>);
 * </p>
 *
 * <p>
 * The rendered HTML is intended to be dropped inside a <c>.jc-prose</c> container so it picks up the console's
 * prose typography.
 *
 * <h5 class='section'>See Also:</h5>
 * <ul>
 * 	<li class='jc'>{@link CommonmarkMarkdownRenderer}
 * </ul>
 *
 * @since 10.0.0
 */
public interface MarkdownRenderer {

	/**
	 * Renders the specified markdown source to an HTML fragment.
	 *
	 * @param markdown The markdown source. Must not be <jk>null</jk> (an empty string renders to an empty fragment).
	 * @return The rendered HTML fragment.
	 * @throws IllegalArgumentException If <jv>markdown</jv> is <jk>null</jk>.
	 */
	String toHtml(String markdown);

	/**
	 * Resolves the active renderer: the first {@link ServiceLoader}-registered provider if one exists, otherwise the
	 * built-in commonmark-java default.
	 *
	 * @return The active renderer. Never <jk>null</jk>.
	 */
	static MarkdownRenderer resolve() {
		return resolve(ServiceLoader.load(MarkdownRenderer.class));
	}

	/**
	 * Resolution seam used by {@link #resolve()}, exposed package-private so both the provider-found and
	 * default-fallback branches can be exercised without touching the JVM-wide {@link ServiceLoader} registry.
	 *
	 * @param providers The candidate providers (a live {@link ServiceLoader} in production).
	 * @return The first provider if any, otherwise a new {@link CommonmarkMarkdownRenderer}.
	 */
	static MarkdownRenderer resolve(Iterable<MarkdownRenderer> providers) {
		var i = providers.iterator();
		return i.hasNext() ? i.next() : new CommonmarkMarkdownRenderer();
	}
}
