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
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;

/**
 * The default {@link MarkdownRenderer} implementation: a thin wrapper around
 * <a class="doclink" href="https://github.com/commonmark/commonmark-java">commonmark-java</a> with the GFM tables
 * extension enabled.
 *
 * <p>
 * The GFM tables extension is on by default because real-world documents (runbooks, onboarding guides) routinely
 * use pipe tables, and core CommonMark renders them as paragraphs of literal pipes &mdash; shipping core-only would
 * reproduce inside the framework exactly the discovery cost this module exists to remove.
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
	 * Builds a parser/renderer pair with the GFM tables extension enabled on both.
	 */
	public CommonmarkMarkdownRenderer() {
		List<Extension> extensions = List.of(TablesExtension.create());
		parser = Parser.builder().extensions(extensions).build();
		renderer = HtmlRenderer.builder().extensions(extensions).build();
	}

	@Override /* MarkdownRenderer */
	public String toHtml(String markdown) {
		if (markdown == null)
			throw iaex("Markdown source must not be null.");
		return renderer.render(parser.parse(markdown));
	}
}
