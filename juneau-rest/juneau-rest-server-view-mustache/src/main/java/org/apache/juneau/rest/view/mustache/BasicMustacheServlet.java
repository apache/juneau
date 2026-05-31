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
package org.apache.juneau.rest.view.mustache;

import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.view.*;

/**
 * Standalone servlet companion of the {@link BasicMustacheResource} mixin.
 *
 * <p>
 * Mounts as a <b>sibling top-level servlet</b> at {@code /mustache/*} and renders raw Mustache
 * templates by delegating to a shared {@link BasicMustacheResource} instance &mdash; the same
 * {@link RawTemplateDispatcher} implementation the mixin uses, so the two forms cannot drift.
 *
 * <p>
 * Whereas the {@link BasicMustacheResource} mixin pins its op at {@code /mustache/*} for composition
 * into a host at {@code /}, this companion declares the mount at the class level
 * ({@link Rest#paths() @Rest(paths="/mustache/*")}) and inherits the {@code /*} op from
 * {@link BasicViewServlet}.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='jc'>{@link BasicMustacheResource}
 * 	<li class='jc'>{@link BasicViewServlet}
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/MustacheViewSupport">Mustache View Support</a>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/RestServerComposition">REST Server &mdash; Composition (mixins, paths)</a>
 * </ul>
 *
 * @serial exclude
 * @since 9.5.0
 */
@Rest(
	paths="/mustache/*",
	responseProcessors={MustacheViewRenderer.class}
)
public class BasicMustacheServlet extends BasicViewServlet {

	private static final long serialVersionUID = 1L;

	private final transient BasicMustacheResource delegate;

	/** No-arg constructor &mdash; uses a default {@link BasicMustacheResource} delegate. */
	public BasicMustacheServlet() {
		this(BasicMustacheResource.create().build());
	}

	/**
	 * Delegate constructor.
	 *
	 * @param delegate The shared Mustache renderer/mixin this servlet delegates raw dispatch to.
	 * 	Must not be {@code null}.
	 */
	protected BasicMustacheServlet(BasicMustacheResource delegate) {
		this.delegate = delegate;
	}

	@Override /* BasicViewServlet */
	protected RawTemplateDispatcher dispatcher() {
		return delegate;
	}
}
