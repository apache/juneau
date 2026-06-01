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
 * Child-resource flavor of the {@link MustacheMixin} mixin.
 *
 * <p>
 * Mounts as a <b>routed child</b> via {@link Rest#children() @Rest(children=MustacheResource.class)}
 * under a parent at the subtree {@code /mustache} and renders raw Mustache templates by delegating to a
 * shared {@link MustacheDispatcher} worker &mdash; the same flavor-neutral {@link RawTemplateDispatcher}
 * implementation the {@link MustacheMixin mixin} and {@link MustacheServlet servlet} flavors hold, so the
 * three forms cannot drift.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='jc'>{@link MustacheMixin}
 * 	<li class='jc'>{@link MustacheServlet}
 * 	<li class='jc'>{@link ViewResource}
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/MustacheViewSupport">Mustache View Support</a>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/RestServerComposition">REST Server &mdash; Composition (mixins, paths)</a>
 * </ul>
 *
 * @since 9.5.0
 */
@Rest(
	path="/mustache",
	responseProcessors={MustacheViewRenderer.class}
)
public class MustacheResource extends ViewResource {

	private final transient MustacheDispatcher worker;

	/** No-arg constructor &mdash; uses a default {@link MustacheDispatcher} worker. */
	public MustacheResource() {
		this(MustacheDispatcher.create().build());
	}

	/**
	 * Worker constructor.
	 *
	 * @param worker The shared flavor-neutral Mustache dispatcher this child delegates raw dispatch
	 * 	to. Must not be {@code null}.
	 */
	protected MustacheResource(MustacheDispatcher worker) {
		this.worker = worker;
	}

	@Override /* ViewResource */
	protected RawTemplateDispatcher dispatcher() {
		return worker;
	}
}
