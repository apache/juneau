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
package org.apache.juneau.microservice.jetty;

import org.apache.juneau.microservice.*;

/**
 * Basic extensible microservice listener with default no-op method implementations.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauMicroserviceJettyBasics">juneau-microservice-jetty Basics</a>
 * </ul>
 */
public class BasicJettyMicroserviceListener extends BasicMicroserviceListener implements JettyMicroserviceListener {

	@Override /* Overridden from JettyMicroserviceListener */
	public void onCreateServer(JettyMicroservice microservice) {}

	@Override /* Overridden from JettyMicroserviceListener */
	public void onPostStartServer(JettyMicroservice microservice) {}

	@Override /* Overridden from JettyMicroserviceListener */
	public void onPostStopServer(JettyMicroservice microservice) {}

	@Override /* Overridden from JettyMicroserviceListener */
	public void onStartServer(JettyMicroservice microservice) {}

	@Override /* Overridden from JettyMicroserviceListener */
	public void onStopServer(JettyMicroservice microservice) {}
}