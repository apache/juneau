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
package org.apache.juneau.examples.rest.jetty;

import org.apache.juneau.examples.rest.RootResources;
import org.apache.juneau.microservice.jetty.*;

/**
 * An example of an extended REST microservice.
 *
 * <p>
 * Subclasses can extend from {@link JettyMicroservice} to implement their own custom behavior.
 * However, this is optional and the {@link JettyMicroservice} class can be invoked directly.
 *
 * <p>
 * The {@link JettyMicroservice} class will locate the <c>examples.cfg</c> file in the home directory and initialize
 * the resources and commands defined in that file.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * </ul>
 */
public class App {

	/**
	 * Entry point method.
	 *
	 * @param args Command line arguments.
	 * @throws Exception General exception occurred.
	 */
	public static void main(String[] args) throws Exception {
		JettyMicroservice
			.create()
			.args(args)
			.servlet(RootResources.class)
			.build()
			.start()
			.startConsole()
			.join();
	}
}
