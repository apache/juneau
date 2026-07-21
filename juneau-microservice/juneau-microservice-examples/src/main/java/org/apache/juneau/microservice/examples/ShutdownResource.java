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
 * Provides the capability to shut down this REST microservice through a REST call.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauMicroservice">juneau-microservice Basics</a>
 * </ul>
 *
 * @serial exclude
 */
@Rest(path = "/shutdown", title = "Shut down this resource")
public class ShutdownResource extends BasicRestServlet {

	private static final long serialVersionUID = 1L;

	/**
	 * [GET /] - Shutdown this resource.
	 *
	 * @return The string <js>"OK"</js>.
	 */
	@RestGet(path = "/", description = "Show contents of config file.")
	@SuppressWarnings("java:S2654") // Background thread is required so the delayed System.exit() runs off the request thread, letting this method's "OK" response flush to the client before the JVM dies; no managed executor is available in this embedded microservice container.
	public String shutdown() {
		new Thread(() -> {
			try {
				Thread.sleep(1000);
				System.exit(0);
			} catch (@SuppressWarnings("unused") InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		}).start();
		return "OK";
	}
}