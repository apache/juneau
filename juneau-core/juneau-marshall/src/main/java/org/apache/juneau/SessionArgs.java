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
 * Runtime arguments common to all bean, serializer, and parser sessions.
 */
public class SessionArgs {

	/**
	 * Default empty session arguments.
	 */
	public static final SessionArgs DEFAULT = new SessionArgs(ObjectMap.EMPTY_MAP);
	
	final ObjectMap properties;

	/**
	 * Constructor.
	 *
	 * @param properties
	 * 	Session-level properties.
	 * 	These override context-level properties.
	 * 	Can be <jk>null</jk>.
	 */
	public SessionArgs(ObjectMap properties) {
		this.properties = properties != null ? properties : ObjectMap.EMPTY_MAP;
	}
}
