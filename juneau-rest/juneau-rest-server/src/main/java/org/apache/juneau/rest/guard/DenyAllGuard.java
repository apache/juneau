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
package org.apache.juneau.rest.guard;

import org.apache.juneau.rest.*;

/**
 * Deny-all {@link RestGuard} &mdash; rejects every request with {@code 403 Forbidden}.
 *
 * <p>
 * Used as the secure-by-default placeholder on operations or resources that require an
 * authentication / authorization chain the importer must explicitly opt into. Without the
 * importer's override, every request is denied; once a user-supplied
 * {@link org.apache.juneau.commons.inject.Bean @Bean} {@link RestGuardList} is registered on the
 * resource, the framework's bean-store override seam <b>replaces</b> the entire
 * annotation-derived guard list (including this deny-all) with the user-supplied chain &mdash;
 * see the {@code RestOpContext.guards} memoizer for the full lookup contract.
 *
 * <h5 class='figure'>Composition example:</h5>
 *
 * <p class='bjava'>
 * 	<jc>// Default-deny on a resource until the importer wires up auth.</jc>
 * 	<ja>@Rest</ja>(path=<js>"/admin"</js>, guards=DenyAllGuard.<jk>class</jk>)
 * 	<jk>public class</jk> AdminResource <jk>extends</jk> RestServlet {
 *
 * 		<jc>// Importer opt-in: register a guard chain that allows authorized callers through.</jc>
 * 		<ja>@Bean</ja>(name=<js>"guards"</js>)
 * 		<jk>public</jk> RestGuardList guards(BeanStore <jv>bs</jv>) {
 * 			<jk>return</jk> RestGuardList.<jsm>create</jsm>(<jv>bs</jv>)
 * 				.append(<jk>new</jk> MyAuthGuard())
 * 				.build();
 * 		}
 * 	}
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='jc'>{@link RestGuard}
 * 	<li class='jc'>{@link RestGuardList}
 * 	<li class='ja'>{@link Rest#guards()}
 * 	<li class='ja'>{@link RestOp#guards()}
 * </ul>
 *
 * @since 10.0.0
 */
public class DenyAllGuard extends RestGuard {

	@Override /* Overridden from RestGuard */
	public boolean isRequestAllowed(RestRequest req) {
		return false;
	}
}
