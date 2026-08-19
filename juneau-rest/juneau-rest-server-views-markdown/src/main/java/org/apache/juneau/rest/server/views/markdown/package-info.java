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

/**
 * An optional markdown-to-HTML rendering story for the views/console toolkit.
 *
 * <p>
 * The framework itself renders data tables ({@code juneau-rest-server-views}) and chrome
 * ({@code juneau-rest-server-console-ui}); neither has any concept of a rendered <i>document</i>. This optional
 * module fills that gap with a single, library-neutral extension point &mdash;
 * {@link org.apache.juneau.rest.server.views.markdown.MarkdownRenderer} &mdash; plus a shipped default
 * ({@link org.apache.juneau.rest.server.views.markdown.CommonmarkMarkdownRenderer}) that wraps commonmark-java with
 * the GFM tables extension enabled.  Because it is a separate module, consumers that do not need markdown pay
 * nothing for it.
 *
 * <p>
 * The rendered HTML is meant to be dropped inside a <c>.jc-prose</c> container so it inherits the console's prose
 * typography.  Wiring the rendered output into a {@code Tab}/{@code Subtab} panel's content property is a deferred
 * follow-on integration point, not part of this module.
 *
 * <h5 class='section'>See Also:</h5>
 * <ul>
 * 	<li class='jc'>{@link org.apache.juneau.rest.server.views.markdown.MarkdownRenderer}
 * 	<li class='jc'>{@link org.apache.juneau.rest.server.views.markdown.CommonmarkMarkdownRenderer}
 * 	<li class='link'><a class="doclink" href="https://github.com/commonmark/commonmark-java">commonmark-java</a>
 * </ul>
 *
 * @since 10.0.0
 */
package org.apache.juneau.rest.server.views.markdown;
