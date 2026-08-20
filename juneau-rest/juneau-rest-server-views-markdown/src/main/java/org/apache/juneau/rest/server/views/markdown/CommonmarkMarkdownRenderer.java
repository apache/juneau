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

import static org.apache.juneau.commons.utils.Shorts.*;

import java.util.*;

import org.commonmark.Extension;
import org.commonmark.ext.gfm.tables.TablesExtension;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.AttributeProvider;
import org.commonmark.renderer.html.HtmlRenderer;

/**
 * The default {@link MarkdownRenderer} implementation: a thin wrapper around
 * <a class="doclink" href="https://github.com/commonmark/commonmark-java">commonmark-java</a> with the GFM tables
 * extension enabled, HTML-escaped raw markup, and URL allowlisting on {@code <a href>} / {@code <img src>}.
 *
 * <p>
 * The GFM tables extension is on by default because real-world documents (runbooks, onboarding guides, skills)
 * routinely use pipe tables, and core CommonMark renders them as paragraphs of literal pipes.
 *
 * <p>
 * Raw HTML in the markdown source is escaped ({@code escapeHtml(true)}), so a {@code <script>} or
 * {@code <img onerror>} in a {@code SKILL.md} becomes visible text, not an element.  Link and image URLs are
 * restricted to {@code http}/{@code https}/{@code mailto}, fragments, and scheme-less relative paths;
 * {@code javascript:}, {@code data:}, and {@code vbscript:} are stripped.
 *
 * <p>
 * commonmark-java's {@link Parser} and {@link HtmlRenderer} are immutable and thread-safe once built, so a single
 * instance of this class is safe to share across threads and reuse for every render.
 *
 * <h5 class='section'>See Also:</h5>
 * <ul>
 * 	<li class='jc'>{@link MarkdownRenderer}
 * 	<li class='link'><a class="doclink" href="https://github.github.com/gfm/#tables-extension-">GFM tables extension</a>
 * </ul>
 *
 * @since 10.0.0
 */
public class CommonmarkMarkdownRenderer implements MarkdownRenderer {

	private final Parser parser;
	private final HtmlRenderer renderer;

	/**
	 * Constructor.
	 *
	 * <p>
	 * Builds a parser/renderer pair with the GFM tables extension enabled, raw HTML escaped, and unsafe URLs
	 * stripped.
	 */
	public CommonmarkMarkdownRenderer() {
		List<Extension> extensions = List.of(TablesExtension.create());
		parser = Parser.builder().extensions(extensions).build();
		renderer = HtmlRenderer.builder()
			.extensions(extensions)
			.escapeHtml(true)
			.attributeProviderFactory(ctx -> new SafeUrlAttributeProvider())
			.build();
	}

	@Override /* MarkdownRenderer */
	public String toHtml(String markdown) {
		if (markdown == null)
			throw iaex("Markdown source must not be null.");
		return renderer.render(parser.parse(markdown));
	}

	/**
	 * Whether {@code url} is safe to emit as an {@code href} or {@code src}: {@code http}/{@code https}/
	 * {@code mailto}, a fragment, a same-origin path, or a scheme-less relative URL.
	 *
	 * @param url The candidate URL.  May be <jk>null</jk>.
	 * @return <jk>true</jk> if the URL may be copied onto an element.
	 */
	public static boolean isSafeUrl(String url) {
		if (url == null || url.isBlank())
			return false;
		var t = url.trim();
		var lower = t.toLowerCase(Locale.ROOT);
		if (lower.startsWith("javascript:") || lower.startsWith("data:") || lower.startsWith("vbscript:"))
			return false;
		if (lower.startsWith("http://") || lower.startsWith("https://") || lower.startsWith("mailto:"))
			return true;
		if (t.charAt(0) == '#' || t.charAt(0) == '/')
			return true;
		return lower.indexOf(':') < 0;
	}

	private static final class SafeUrlAttributeProvider implements AttributeProvider {
		@Override
		public void setAttributes(Node node, String tagName, Map<String,String> attributes) {
			if ("a".equals(tagName) && attributes.containsKey("href") && ! isSafeUrl(attributes.get("href")))
				attributes.remove("href");
			if ("img".equals(tagName) && attributes.containsKey("src") && ! isSafeUrl(attributes.get("src")))
				attributes.remove("src");
		}
	}
}
