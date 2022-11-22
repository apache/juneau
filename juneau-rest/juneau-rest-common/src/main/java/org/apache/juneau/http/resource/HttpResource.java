// ***************************************************************************************************************************
// * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file *
// * distributed with this work for additional information regarding copyright ownership.  The ASF licenses this file        *
// * to you under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance            *
// * with the License.  You may obtain a copy of the License at                                                              *
// *                                                                                                                         *
// *  http://www.apache.org/licenses/LICENSE-2.0                                                                             *
// *                                                                                                                         *
// * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an  *
// * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the License for the        *
// * specific language governing permissions and limitations under the License.                                              *
// ***************************************************************************************************************************
package org.apache.juneau.http.resource;

import org.apache.http.*;
import org.apache.juneau.http.header.*;

/**
 * An extension of an {@link HttpEntity} that also includes arbitrary headers.
 *
 * <p>
 * While {@link HttpEntity} beans support <c>Content-Type</c>, <c>Content-Encoding</c>, and <c>Content-Length</c>
 * headers, this interface allows you to add any number of arbitrary headers to an entity.
 *
 * <p>
 * For example, you might want to be able to create an entity with a <c>Cache-Control</c> header.
 *
 * <p class='bjava'>
 * 	<jk>import static</jk> org.apache.juneau.http.HttpResources.*;
 *
 *	<jc>// Create a resource with dynamic content and a no-cache header.</jc>
 * 	HttpResource <jv>myResource</jv> = <jsm>stringResource</jsm>(()-&gt;<jsm>getMyContent</jsm>(), ContentType.<jsf>TEXT_PLAIN</jsf>)
 * 		.header(<js>"Cache-Control"</js>, <js>"no-cache"</js>)
 * 		.build();
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../../index.html#juneau-rest-common">juneau-rest-common</a>
 * </ul>
 */
public interface HttpResource extends HttpEntity {

	/**
	 * Returns the list of headers associated with this resource.
	 *
	 * <p>
	 * Note that this typically does NOT include headers associated with {@link HttpEntity}
	 * (e.g. <c>Content-Type</c>, <c>Content-Encoding</c>, and <c>Content-Length</c>).
	 *
	 * @return The list of headers associated with this resource.
	 */
	HeaderList getHeaders();
}
