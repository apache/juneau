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
package org.apache.juneau.microservice.tomcat;

import java.io.*;

import org.apache.catalina.startup.*;

/**
 * Interface for creating embedded Tomcat servers.
 *
 * <p>
 * Mirrors {@code org.apache.juneau.marshall.microservice.jetty.JettyServerFactory} but uses the programmatic
 * {@link Tomcat} façade instead of a {@code jetty.xml} document.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauMicroserviceTomcat">juneau-microservice-tomcat Basics</a>
 * </ul>
 */
public interface TomcatServerFactory {

	/**
	 * Create a new initialized embedded Tomcat server.
	 *
	 * <p>
	 * The returned server is configured with the specified Catalina base directory and a root context (mounted
	 * at <c>""</c>) but is not yet started and has no connector port assigned — the {@link TomcatServerComponent}
	 * assigns the port and materializes the connector before calling {@link Tomcat#start()}.
	 *
	 * @param baseDir The writable Catalina base / work directory to use for this server.
	 * @return A newly-created but not-yet-started Tomcat server.
	 * @throws Exception Any exception.
	 */
	@SuppressWarnings({
		"java:S112" // throws Exception intentional - callback/lifecycle method
	})
	Tomcat create(File baseDir) throws Exception;
}
