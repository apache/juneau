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
package org.apache.juneau.rest.view.jsp;

import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.view.*;

/**
 * Standalone servlet companion of the {@link BasicJspResource} mixin.
 *
 * <p>
 * Mounts as a <b>sibling top-level servlet</b> at {@code /jsp/*} (a sibling of the host
 * application's other servlets, e.g. {@code /rest/*}) and serves raw {@code .jsp} resources by
 * delegating to a shared {@link BasicJspResource} instance &mdash; the same
 * {@link RawTemplateDispatcher} implementation the mixin uses, so the two forms cannot drift.
 *
 * <p>
 * Whereas the {@link BasicJspResource} mixin pins its op at {@code /jsp/*} for composition into a
 * host at {@code /}, this companion declares the mount at the class level
 * ({@link Rest#paths() @Rest(paths="/jsp/*")}) and inherits the {@code /*} op from
 * {@link BasicViewServlet} &mdash; eliminating the path-doubling that a mixin-shaped op would
 * produce when the servlet itself is mounted at {@code /jsp/*}.
 *
 * <h5 class='figure'>Microservice registration:</h5>
 *
 * <p class='bjava'>
 * 	<ja>@Bean</ja> Servlet jsp() {
 * 		<jk>return new</jk> BasicJspServlet();  <jc>// auto-mounted at /jsp/* by JettyServerComponent</jc>
 * 	}
 * </p>
 *
 * <p>
 * The default mount {@code /jsp/*} can be relocated by overriding {@link #getPaths()} or via the
 * programmatic {@code RestContext.Builder.paths(...)} rung; the underlying base path for resolving
 * {@code .jsp} files is configured on the {@link BasicJspResource} delegate.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='jc'>{@link BasicJspResource}
 * 	<li class='jc'>{@link BasicViewServlet}
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JspViewSupport">JSP View Support</a>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/RestServerComposition">REST Server &mdash; Composition (mixins, paths)</a>
 * </ul>
 *
 * @serial exclude
 * @since 9.5.0
 */
@Rest(
	paths="/jsp/*",
	responseProcessors={JspViewRenderer.class}
)
public class BasicJspServlet extends BasicViewServlet {

	private static final long serialVersionUID = 1L;

	private final transient BasicJspResource delegate;

	/** No-arg constructor &mdash; uses a default {@link BasicJspResource} delegate. */
	public BasicJspServlet() {
		this(BasicJspResource.create().build());
	}

	/**
	 * Delegate constructor.
	 *
	 * @param delegate The shared JSP renderer/mixin this servlet delegates raw dispatch to. Must
	 * 	not be {@code null}.
	 */
	protected BasicJspServlet(BasicJspResource delegate) {
		this.delegate = delegate;
	}

	@Override /* BasicViewServlet */
	protected RawTemplateDispatcher dispatcher() {
		return delegate;
	}
}
