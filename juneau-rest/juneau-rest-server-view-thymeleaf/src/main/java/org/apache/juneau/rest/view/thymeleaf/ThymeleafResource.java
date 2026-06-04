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
 * Child-resource flavor of the {@link ThymeleafMixin} mixin.
 *
 * <p>
 * Mounts as a <b>routed child</b> via {@link Rest#children() @Rest(children=ThymeleafResource.class)}
 * under a parent at the subtree {@code /thymeleaf} and renders raw Thymeleaf templates by delegating to a
 * shared {@link ThymeleafDispatcher} worker &mdash; the same flavor-neutral {@link RawTemplateDispatcher}
 * implementation the {@link ThymeleafMixin mixin} and {@link ThymeleafServlet servlet} flavors hold, so the
 * three forms cannot drift.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='jc'>{@link ThymeleafMixin}
 * 	<li class='jc'>{@link ThymeleafServlet}
 * 	<li class='jc'>{@link ViewResource}
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/ThymeleafViewSupport">Thymeleaf View Support</a>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/RestServerComposition">REST Server &mdash; Composition (mixins, paths)</a>
 * </ul>
 *
 * @since 9.5.0
 */
@Rest(
	path="/thymeleaf",
	responseProcessors={ThymeleafViewRenderer.class}
)
public class ThymeleafResource extends ViewResource {

	private final ThymeleafDispatcher worker;

	/** No-arg constructor &mdash; uses a default {@link ThymeleafDispatcher} worker. */
	public ThymeleafResource() {
		this(ThymeleafDispatcher.create().build());
	}

	/**
	 * Worker constructor.
	 *
	 * @param worker The shared flavor-neutral Thymeleaf dispatcher this child delegates raw dispatch
	 * 	to. Must not be {@code null}.
	 */
	protected ThymeleafResource(ThymeleafDispatcher worker) {
		this.worker = worker;
	}

	@Override /* ViewResource */
	protected RawTemplateDispatcher dispatcher() {
		return worker;
	}
}
