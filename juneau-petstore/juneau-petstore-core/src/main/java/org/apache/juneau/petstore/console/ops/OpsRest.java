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
package org.apache.juneau.petstore.console.ops;

import org.apache.juneau.http.response.*;
import org.apache.juneau.petstore.console.*;
import org.apache.juneau.rest.server.*;

/**
 * Operations: parent of the Jobs and Audit pages.  {@code GET /console/ops} redirects to Jobs.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	<ja>@Rest</ja>(path=<js>"/ops"</js>, children={JobsRest.<jk>class</jk>})
 * 	<jk>public class</jk> OpsRest <jk>extends</jk> PetstoreConsolePage {
 *
 * 		<ja>@RestGet</ja>(path=<js>"/"</js>)
 * 		<jk>public</jk> SeeOther index(RestRequest <jv>req</jv>) {
 * 			<jk>return new</jk> SeeOther().setLocation(<jv>req</jv>.getUriResolver().resolve(<js>"servlet:/jobs"</js>));
 * 		}
 * 	}
 * </p>
 */
@Rest(path="/ops", title="Operations", children={JobsRest.class, AuditRest.class})
@SuppressWarnings({
	"java:S110" // Inheritance depth comes from the BasicRestServlet hierarchy, not this page.
})
public class OpsRest extends PetstoreConsolePage {

	private static final long serialVersionUID = 1L;

	/**
	 * {@code GET /console/ops} redirects to the Jobs page.
	 *
	 * @param req The request.
	 * @return A 303 to {@code /console/ops/jobs}.
	 */
	@RestGet(path="/")
	public SeeOther index(RestRequest req) {
		return new SeeOther().setLocation(req.getUriResolver().resolve("servlet:/jobs"));
	}
}
