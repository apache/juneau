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
 * Child-resource flavor of the {@link FreemarkerMixin} mixin.
 *
 * <p>
 * Mounts as a <b>routed child</b> via {@link Rest#children() @Rest(children=FreemarkerResource.class)}
 * under a parent at the subtree {@code /freemarker} and renders raw FreeMarker templates by delegating to
 * a shared {@link FreemarkerDispatcher} worker &mdash; the same flavor-neutral {@link RawTemplateDispatcher}
 * implementation the {@link FreemarkerMixin mixin} and {@link FreemarkerServlet servlet} flavors hold, so the
 * three forms cannot drift.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='jc'>{@link FreemarkerMixin}
 * 	<li class='jc'>{@link FreemarkerServlet}
 * 	<li class='jc'>{@link ViewResource}
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/FreemarkerViewSupport">FreeMarker View Support</a>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/RestServerComposition">REST Server &mdash; Composition (mixins, paths)</a>
 * </ul>
 *
 * @since 10.0.0
 */
@Rest(
	path="/freemarker",
	responseProcessors={FreemarkerViewRenderer.class}
)
public class FreemarkerResource extends ViewResource {

	private final FreemarkerDispatcher worker;

	/** No-arg constructor &mdash; uses a default {@link FreemarkerDispatcher} worker. */
	public FreemarkerResource() {
		this(FreemarkerDispatcher.create().build());
	}

	/**
	 * Worker constructor.
	 *
	 * @param worker The shared flavor-neutral FreeMarker dispatcher this child delegates raw dispatch
	 * 	to. Must not be {@code null}.
	 */
	protected FreemarkerResource(FreemarkerDispatcher worker) {
		this.worker = worker;
	}

	@Override /* ViewResource */
	protected RawTemplateDispatcher dispatcher() {
		return worker;
	}
}
