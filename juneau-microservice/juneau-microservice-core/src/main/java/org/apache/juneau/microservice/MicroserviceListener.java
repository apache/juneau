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
package org.apache.juneau.microservice;

import org.apache.juneau.config.event.*;

/**
 * Listener class for microservice lifecycle events.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../index.html#juneau-microservice-core">juneau-microservice-core</a>
 * </ul>
 */
public interface MicroserviceListener {

	/**
	 * Called at the beginning of the {@link Microservice#start()} call.
	 *
	 * @param microservice Reference to microservice.
	 */
	void onStart(Microservice microservice);

	/**
	 * Called at the end of the {@link Microservice#stop()} call.
	 *
	 * @param microservice Reference to microservice.
	 */
	void onStop(Microservice microservice);

	/**
	 * Called if one or more changes occur in the config file.
	 *
	 * @param microservice Reference to microservice.
	 * @param events The list of changes in the config file.
	 */
	void onConfigChange(Microservice microservice, ConfigEvents events);
}
