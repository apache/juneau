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
package org.apache.juneau.rest.server.view.freemarker;

import org.apache.juneau.rest.server.*;
import org.apache.juneau.rest.server.view.*;

/**
 * Standalone servlet companion of the {@link FreemarkerMixin} mixin.
 *
 * <p>
 * Mounts as a <b>sibling top-level servlet</b> at {@code /freemarker/*} and renders raw FreeMarker
 * templates by delegating to a shared {@link FreemarkerDispatcher} worker &mdash; the same
 * flavor-neutral {@link RawTemplateDispatcher} implementation the mixin and child flavors hold, so
 * the forms cannot drift.
 *
 * <p>
 * Whereas the {@link FreemarkerMixin} mixin pins its op at {@code /freemarker/*} for
 * composition into a host at {@code /}, this companion declares the mount at the class level
 * ({@link Rest#paths() @Rest(paths="/freemarker/*")}) and inherits the {@code /*} op from
 * {@link ViewServlet}.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='jc'>{@link FreemarkerMixin}
 * 	<li class='jc'>{@link ViewServlet}
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/FreemarkerViewSupport">FreeMarker View Support</a>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/RestServerComposition">REST Server &mdash; Mixins and Multi-Mount Paths</a>
 * </ul>
 *
 * @serial exclude
 * @since 10.0.0
 */
@Rest(
	paths="/freemarker/*",
	responseProcessors={FreemarkerViewRenderer.class}
)
public class FreemarkerServlet extends ViewServlet {

	private static final long serialVersionUID = 1L;

	private final transient FreemarkerDispatcher worker;

	/** No-arg constructor &mdash; uses a default {@link FreemarkerDispatcher} worker. */
	public FreemarkerServlet() {
		this(FreemarkerDispatcher.create().build());
	}

	/**
	 * Worker constructor.
	 *
	 * @param worker The shared flavor-neutral FreeMarker dispatcher this servlet delegates raw
	 * 	dispatch to. Must not be {@code null}.
	 */
	protected FreemarkerServlet(FreemarkerDispatcher worker) {
		this.worker = worker;
	}

	@Override /* ViewServlet */
	protected RawTemplateDispatcher dispatcher() {
		return worker;
	}
}
