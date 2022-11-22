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
package org.apache.juneau;

/**
 * Identifies the possible types of URL resolution.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * </ul>
 */
public enum UriResolution {

	/**
	 * Resolve to an absolute URL.
	 * <p>
	 * (e.g. <js>"http://host:port/context-root/servlet-path/path-info"</js>)
	 */
	ABSOLUTE,

	/**
	 * Resolve to a root-relative URL.
	 * <p>
	 * (e.g. <js>"/context-root/servlet-path/path-info"</js>)
	 */
	ROOT_RELATIVE,

	/**
	 * Don't do any URL resolution.
	 */
	NONE;
}
