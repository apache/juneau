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
package org.apache.juneau.rest.server.view;

import java.util.*;

import org.apache.juneau.rest.server.*;
import org.apache.juneau.rest.server.processor.*;

/**
 * Engine-agnostic contract for a server-side-rendered template return value from an
 * {@link RestOp @RestOp}-annotated method.
 *
 * <p>
 * A {@code View} carries the information a templating engine needs to render a response: the
 * <b>template name</b> (relative to a base path supplied by the corresponding mixin), an
 * <b>attribute map</b> exposed to the template, and an optional <b>response-headers</b> seam for
 * setting {@code Content-Type}, {@code Cache-Control}, or other headers on the rendered response.
 *
 * <p>
 * The interface is deliberately minimal and engine-agnostic. Concrete implementations live in the
 * per-engine bridge modules ({@code juneau-rest-server-view-jsp}, future
 * {@code juneau-rest-server-view-thymeleaf} / {@code -mustache} / {@code -freemarker}), each
 * paired with a {@link ResponseProcessor} that detects its own {@code View} subtype and dispatches
 * to the underlying templating engine.
 *
 * <h5 class='figure'>End-state usage:</h5>
 *
 * <p class='bjava'>
 * 	<ja>@Rest</ja>(path=<js>"/app"</js>, mixins=JspMixin.<jk>class</jk>)
 * 	<jk>public class</jk> AppResource <jk>extends</jk> RestServlet {
 *
 * 		<ja>@RestGet</ja>(<js>"/hello/{name}"</js>)
 * 		<jk>public</jk> View hello(<ja>@Path</ja> String <jv>name</jv>) {
 * 			<jk>return</jk> JspView.<jsm>of</jsm>(<js>"hello.jsp"</js>)
 * 				.attr(<js>"name"</js>, <jv>name</jv>)
 * 				.attr(<js>"ts"</js>, Instant.<jsm>now</jsm>());
 * 		}
 * 	}
 * </p>
 *
 * <h5 class='section'>Contract:</h5>
 *
 * <ul class='spaced-list'>
 * 	<li>{@link #getTemplateName() getTemplateName()} &mdash; the template path the engine should
 * 		render, typically relative to a mixin-configured base path (e.g. {@code "hello.jsp"} with
 * 		base path {@code "/WEB-INF/views/"} resolves to {@code "/WEB-INF/views/hello.jsp"}).
 * 		<b>Must never be {@code null}.</b>
 * 	<li>{@link #getAttributes() getAttributes()} &mdash; key/value bindings exposed to the template
 * 		(e.g. {@code ${name}} in a JSP, {@code th:text="${name}"} in a Thymeleaf template). Returns
 * 		an immutable empty map when no attributes are bound. Must never be {@code null}.
 * 	<li>{@link #getResponseHeaders() getResponseHeaders()} &mdash; optional response-headers map
 * 		applied by the renderer before forwarding to the engine. Defaults to an empty map so most
 * 		implementations don't need to override it; engines that want to set {@code Content-Type}
 * 		or {@code Cache-Control} on the rendered response can populate this.
 * </ul>
 *
 * <h5 class='section'>Stability:</h5>
 *
 * <p>
 * This interface is the stable extension point for view modules. New methods will be added as
 * {@code default}-bodied where possible to preserve backward compatibility with downstream view
 * impls.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JspViewSupport">JSP View Support</a>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/RestServerComposition">REST Server &mdash; Composition (mixins, paths)</a>
 * </ul>
 *
 * @since 10.0.0
 */
public interface View {

	/**
	 * The name of the template to render &mdash; typically a path relative to the
	 * mixin-configured base path.
	 *
	 * <p>
	 * For example, {@code "hello.jsp"} with a JSP mixin {@code basePath("/WEB-INF/views/")}
	 * resolves to {@code "/WEB-INF/views/hello.jsp"} on the request dispatcher.
	 *
	 * @return The template name. Never {@code null}.
	 */
	String getTemplateName();

	/**
	 * The attributes to expose to the template.
	 *
	 * <p>
	 * Each entry is forwarded to the templating engine as a named binding accessible from inside
	 * the template (e.g. {@code ${name}} in JSP / EL, {@code th:text="${name}"} in Thymeleaf).
	 *
	 * @return The attributes map. Never {@code null}; returns an immutable empty map when no
	 * 	attributes are bound.
	 */
	Map<String,Object> getAttributes();

	/**
	 * Optional response headers applied by the renderer before forwarding to the templating
	 * engine.
	 *
	 * <p>
	 * Typical use cases: setting {@code Content-Type} (e.g. {@code "text/html; charset=UTF-8"}) or
	 * {@code Cache-Control} on the rendered response. The default implementation returns an empty
	 * map so most {@code View} impls don't need to override it.
	 *
	 * @return An immutable map of headers to apply. Never {@code null}; defaults to an empty map.
	 */
	default Map<String,String> getResponseHeaders() {
		return Map.of();
	}
}
