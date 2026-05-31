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
package org.apache.juneau.rest.view.thymeleaf;

import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.view.*;

/**
 * Standalone servlet companion of the {@link BasicThymeleafResource} mixin.
 *
 * <p>
 * Mounts as a <b>sibling top-level servlet</b> at {@code /thymeleaf/*} and renders raw Thymeleaf
 * templates by delegating to a shared {@link BasicThymeleafResource} instance &mdash; the same
 * {@link RawTemplateDispatcher} implementation the mixin uses, so the two forms cannot drift.
 *
 * <p>
 * Whereas the {@link BasicThymeleafResource} mixin pins its op at {@code /thymeleaf/*} for
 * composition into a host at {@code /}, this companion declares the mount at the class level
 * ({@link Rest#paths() @Rest(paths="/thymeleaf/*")}) and inherits the {@code /*} op from
 * {@link BasicViewServlet}.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='jc'>{@link BasicThymeleafResource}
 * 	<li class='jc'>{@link BasicViewServlet}
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/ThymeleafViewSupport">Thymeleaf View Support</a>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/RestServerComposition">REST Server &mdash; Composition (mixins, paths)</a>
 * </ul>
 *
 * @serial exclude
 * @since 9.5.0
 */
@Rest(
	paths="/thymeleaf/*",
	responseProcessors={ThymeleafViewRenderer.class}
)
public class BasicThymeleafServlet extends BasicViewServlet {

	private static final long serialVersionUID = 1L;

	private final transient BasicThymeleafResource delegate;

	/** No-arg constructor &mdash; uses a default {@link BasicThymeleafResource} delegate. */
	public BasicThymeleafServlet() {
		this(BasicThymeleafResource.create().build());
	}

	/**
	 * Delegate constructor.
	 *
	 * @param delegate The shared Thymeleaf renderer/mixin this servlet delegates raw dispatch to.
	 * 	Must not be {@code null}.
	 */
	protected BasicThymeleafServlet(BasicThymeleafResource delegate) {
		this.delegate = delegate;
	}

	@Override /* BasicViewServlet */
	protected RawTemplateDispatcher dispatcher() {
		return delegate;
	}
}
