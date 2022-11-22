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
package org.apache.juneau.rest.guard;

import java.text.*;
import java.util.*;
import java.util.stream.*;

import org.apache.juneau.rest.*;

/**
 * {@link RestGuard} that uses role expressions to determine whether an authenticated user has access to a class or method.
 *
 * <p>
 * The role expression supports the following constructs:
 * <ul>
 * 	<li><js>"foo"</js> - Single arguments.
 * 	<li><js>"foo,bar,baz"</js> - Multiple OR'ed arguments.
 * 	<li><js>"foo | bar | bqz"</js> - Multiple OR'ed arguments, pipe syntax.
 * 	<li><js>"foo || bar || bqz"</js> - Multiple OR'ed arguments, Java-OR syntax.
 * 	<li><js>"fo*"</js> - Patterns including <js>'*'</js> and <js>'?'</js>.
 * 	<li><js>"fo* &amp; *oo"</js> - Multiple AND'ed arguments, ampersand syntax.
 * 	<li><js>"fo* &amp;&amp; *oo"</js> - Multiple AND'ed arguments, Java-AND syntax.
 * 	<li><js>"fo* || (*oo || bar)"</js> - Parenthesis.
 * </ul>
 *
 * <h5 class='section'>Notes:</h5><ul>
 * 	<li class='note'>AND operations take precedence over OR operations (as expected).
 * 	<li class='note'>Whitespace is ignored.
 * 	<li class='note'><jk>null</jk> or empty expressions always match as <jk>false</jk>.
 * </ul>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../../index.html#jrs.Guards">Guards</a>
 * </ul>
 */
public class RoleBasedRestGuard extends RestGuard {

	private final Set<String> roles;
	private final RoleMatcher roleMatcher;

	/**
	 * Constructor.
	 *
	 * @param declaredRoles
	 * 	List of possible declared roles.
	 * 	<br>If <jk>null</jk>, we find the roles in the expression itself.
	 * 	<br>This is only needed if you're using pattern matching in the expression.
	 * @param roleExpression
	 * 	The role expression.
	 * 	<br>If <jk>null</jk> or empty/blanks, the this guard will never pass.
	 * @throws ParseException Invalid role expression syntax.
	 */
	public RoleBasedRestGuard(Set<String> declaredRoles, String roleExpression) throws ParseException {
		roleMatcher = new RoleMatcher(roleExpression);
		roles = new TreeSet<>(declaredRoles == null ? roleMatcher.getRolesInExpression() : declaredRoles);
	}

	@Override
	public boolean isRequestAllowed(RestRequest req) {
		Set<String> userRoles = roles.stream().filter(x -> req.isUserInRole(x)).collect(Collectors.toSet());
		return roleMatcher.matches(userRoles);
	}
}
