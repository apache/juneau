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
package org.apache.juneau.rest.view;

import org.apache.juneau.*;
import org.apache.juneau.rest.mock.classic.*;

/**
 * Shared base for typed-handler {@link View} integration test matrices.
 *
 * <p>
 * Each concrete subclass targets one engine (JSP, Thymeleaf, Mustache, FreeMarker) and covers
 * five assertions:
 *
 * <ol>
 * 	<li><b>a01</b> — A {@code @RestGet} returning an engine-specific {@code View} reaches the
 * 		corresponding {@code ViewRenderer}, not {@code SerializedPojoProcessor}.  The response
 * 		body must NOT contain {@code "templateName"} (which would appear only in a Juneau-bean
 * 		JSON dump of the view object).
 * 	<li><b>a02</b> — A {@code @RestGet} returning a plain {@code String} on the same host falls
 * 		through to {@code SerializedPojoProcessor} and is returned unchanged (regression guard).
 * 	<li><b>a03</b> — A host with two renderers dispatches each {@code View} type to the correct
 * 		renderer.  Neither engine crosses to the other.
 * 	<li><b>a04</b> — A typed handler returning a view with {@code .attr("name", "Alice")} either
 * 		(a) produces a rendered body containing {@code "Alice"} (Thymeleaf / Mustache / FreeMarker
 * 		which render from classpath without a container) or (b) does NOT bean-serialize the view
 * 		object (JSP, which requires a real servlet container).
 * 	<li><b>a05</b> — A host with <em>no</em> renderer in {@code responseProcessors} bean-serializes
 * 		a {@code View}-typed return value; the body DOES contain {@code "templateName"}.
 * </ol>
 *
 * <h5 class='section'>Architecture note:</h5>
 *
 * <p>
 * The mixin's {@code @Rest(responseProcessors=...)} only covers requests handled by the mixin's
 * own routes (e.g. {@code /thymeleaf/*}).  Host-class {@code @RestOp} methods that return
 * engine-specific {@code View} objects require the renderer to be added to the <em>host's</em>
 * {@code @Rest(responseProcessors=...)}:
 *
 * <p class='bjava'>
 * 	<ja>@Rest</ja>(<jv>responseProcessors</jv>={ThymeleafViewRenderer.<jk>class</jk>})
 * 	<jk>public class</jk> AppResource <jk>extends</jk> BasicRestServlet {
 * 		<ja>@RestGet</ja>(<js>"/hello/{name}"</js>)
 * 		<jk>public</jk> View hello(<ja>@Path</ja> String <jv>name</jv>) {
 * 			<jk>return</jk> ThymeleafView.<jsm>of</jsm>(<js>"templates/hello.html"</js>).attr(<js>"name"</js>, <jv>name</jv>);
 * 		}
 * 	}
 * </p>
 *
 * <p>
 * The {@link org.apache.juneau.rest.processor.ResponseProcessorList} partition pass (added in
 * 9.5.0) then automatically repositions the renderer before
 * {@link org.apache.juneau.rest.processor.SerializedPojoProcessor} in the chain.
 *
 * @since 9.5.0
 */
public abstract class ViewIntegrationTestBase extends TestBase {

	/**
	 * Wraps the given resource class in a {@link MockRestClient} configured in lax mode
	 * (tolerates non-2xx status codes).
	 *
	 * @param resourceClass The {@code @Rest}-annotated host class to wrap.
	 * @return A ready-to-use {@link MockRestClient}.
	 */
	protected static MockRestClient buildResource(Class<?> resourceClass) {
		return MockRestClient.buildLax(resourceClass);
	}
}
