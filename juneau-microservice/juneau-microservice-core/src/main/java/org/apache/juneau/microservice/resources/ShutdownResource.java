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
package org.apache.juneau.microservice.resources;

import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.servlet.*;

/**
 * Provides the capability to shut down this REST microservice through a REST call.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../../index.html#juneau-microservice-core">juneau-microservice-core</a>
 * </ul>
 *
 * @serial exclude
 */
@Rest(
	path="/shutdown",
	title="Shut down this resource"
)
public class ShutdownResource extends BasicRestServlet {

	private static final long serialVersionUID = 1L;

	/**
	 * [GET /] - Shutdown this resource.
	 *
	 * @return The string <js>"OK"</js>.
	 */
	@RestGet(path="/", description="Show contents of config file.")
	public String shutdown() {
		new Thread(
			() -> {
            	try {
            		Thread.sleep(1000);
            		System.exit(0);
            	} catch (InterruptedException e) {
            		e.printStackTrace();
            	}
            }
		).start();
		return "OK";
	}
}
