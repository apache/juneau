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

import static org.apache.juneau.commons.utils.Shorts.*;

import org.apache.juneau.rest.server.*;
import org.apache.juneau.rest.server.servlet.*;

/**
 * Provides the capability to shut down this REST microservice through a REST call.
 *
 * <p>
 * {@code POST /shutdown} stops the process: the {@code "OK"} response is sent first, and the exit follows about a
 * second later on a background thread.  Shutting down is a write, so it is a {@code POST}, never a {@code GET}: a safe
 * method passes every CSRF check, so a {@code GET} would let any page that embeds {@code <img src=".../shutdown">}
 * stop the process.  {@code GET /shutdown} is answered with {@code 405}.
 *
 * <p>
 * From the command line, send a non-form content type so a CSRF boundary in front of this resource (see
 * {@link AdminBoundary}) admits the request without a token:
 * <p class='bcode'>
 * 	curl -X POST -H 'Content-Type: application/json' http://localhost:10000/shutdown
 * </p>
 *
 * <p>
 * This resource has no authentication of its own.  Do not expose it on an untrusted network.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='jc'>{@link AdminBoundary}
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauMicroservice">juneau-microservice</a>
 * </ul>
 *
 * @serial exclude
 */
@Rest(path = "/shutdown", title = "Shut down this resource")
public class ShutdownResource extends BasicRestServlet {

	/**
	 * How the process is stopped.
	 *
	 * <p>
	 * The default calls {@link System#exit(int)}.  Pass another to {@link ShutdownResource#ShutdownResource(Exit)}, or
	 * register one as a bean in its parent resource's bean store, to observe a shutdown without stopping the JVM, for
	 * example in a test.
	 */
	@FunctionalInterface
	public interface Exit {

		/**
		 * Stops the process.
		 *
		 * @param status The exit status.
		 */
		void exit(int status);
	}

	private static final long serialVersionUID = 1L;

	// Not serialized: a servlet is never restored from a stream, and an exit is behaviour, not state.
	private final transient Exit exit;

	/**
	 * Constructor.
	 *
	 * <p>
	 * A shutdown calls {@link System#exit(int)}.
	 */
	public ShutdownResource() {
		this(System::exit);
	}

	/**
	 * Constructor.
	 *
	 * <p>
	 * When the resource is mounted as a child by class, this constructor is used if the parent resource's bean store
	 * holds an {@link Exit}, for example from a <c>@Bean</c> method on the parent.  A microservice's own beans are not
	 * in that store.
	 *
	 * @param exit How a shutdown stops the process.  Must not be <jk>null</jk>.
	 * @throws IllegalArgumentException If {@code exit} is <jk>null</jk>.
	 */
	public ShutdownResource(Exit exit) {
		this.exit = reqnn("exit", exit);
	}

	/**
	 * [POST /] - Shuts down this microservice.
	 *
	 * @return The string <js>"OK"</js>.
	 */
	@Mutating("Stops the process")
	@RestPost(path = "/", summary = "Shut down this microservice", description = "Stops the process about a second after the response is sent.")
	@SuppressWarnings({
		"java:S2654" // Background thread is required so the delayed exit runs off the request thread, letting this method's "OK" response flush to the client before the JVM dies; no managed executor is available in this embedded microservice container.
	})
	public String shutdown() {
		var e = exit;
		new Thread(() -> {
			try {
				Thread.sleep(1000);
				e.exit(0);
			} catch (@SuppressWarnings("unused") InterruptedException e2) {
				Thread.currentThread().interrupt();
			}
		}).start();
		return "OK";
	}
}
