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
package org.apache.juneau.rest;

/**
 * Message bundle location.
 * 
 * <p>
 * Identifies a message bundle by a base class and bundle path.
 * 
 * <h5 class='section'>See Also:</h5>
 * <ul>
 * 	<li class='link'><a class="doclink" href="../../../../overview-summary.html#juneau-rest-server.Messages">Overview &gt; Messages</a>
 * </ul>
 */
public class MessageBundleLocation {
	
	final Class<?> baseClass;
	final String bundlePath;
	
	/**
	 * Constructor.
	 * 
	 * @param baseClass The base class that the bundle path is relative to.
	 * @param bundlePath The bundle path relative to the base class.
	 */
	public MessageBundleLocation(Class<?> baseClass, String bundlePath) {
		this.baseClass = baseClass;
		this.bundlePath = bundlePath;
	}
}
