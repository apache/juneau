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
package org.apache.juneau.microservice.examples;

import org.apache.juneau.rest.server.*;
import org.apache.juneau.rest.server.servlet.*;

/**
 * Sample root REST resource.
 *
 * <p>
 * It mounts {@link ConfigResource} with no CSRF boundary in front of it.  {@code ConfigResource} has no HTML form, and
 * its writes are {@code PUT}s, which a browser cannot send cross-site without a CORS preflight.  An application that
 * mounts the admin resources where a browser can reach them should still register {@link AdminBoundary}.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauMicroservice">juneau-microservice</a>
 * </ul>
 *
 * @serial exclude
 */
@Rest(
	path="/",
	title="Sample Root Resource",
	description="This is a sample router page",
	children={ConfigResource.class}
)
@SuppressWarnings({
	"java:S110" // Inheritance depth acceptable for microservice hierarchy
})
public class SampleRootResource extends BasicRestServletGroup {
	private static final long serialVersionUID = 1L;
}