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
package org.apache.juneau.rest.server.view.jsp;

import org.apache.juneau.rest.server.*;
import org.apache.juneau.rest.server.view.*;

/**
 * Child-resource flavor of the {@link JspMixin} mixin.
 *
 * <p>
 * Mounts as a <b>routed child</b> via {@link Rest#children() @Rest(children=JspResource.class)} under a
 * parent at the subtree {@code /jsp} and serves raw {@code .jsp} resources by delegating to a shared
 * {@link JspDispatcher} worker &mdash; the same flavor-neutral {@link RawTemplateDispatcher}
 * implementation the {@link JspMixin mixin} and {@link JspServlet servlet} flavors hold, so the three
 * forms cannot drift.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='jc'>{@link JspMixin}
 * 	<li class='jc'>{@link JspServlet}
 * 	<li class='jc'>{@link ViewResource}
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JspViewSupport">JSP View Support</a>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/RestServerComposition">REST Server &mdash; Composition (mixins, paths)</a>
 * </ul>
 *
 * @since 10.0.0
 */
@Rest(
	path="/jsp",
	responseProcessors={JspViewRenderer.class}
)
public class JspResource extends ViewResource {

	private final JspDispatcher worker;

	/** No-arg constructor &mdash; uses a default {@link JspDispatcher} worker. */
	public JspResource() {
		this(JspDispatcher.create().build());
	}

	/**
	 * Worker constructor.
	 *
	 * @param worker The shared flavor-neutral JSP dispatcher this child delegates raw dispatch to.
	 * 	Must not be {@code null}.
	 */
	protected JspResource(JspDispatcher worker) {
		this.worker = worker;
	}

	@Override /* ViewResource */
	protected RawTemplateDispatcher dispatcher() {
		return worker;
	}
}
