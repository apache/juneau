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
 * A one-time-use non-thread-safe object that's meant to be used once and then thrown away.
 * <p>
 * Serializers and parsers use context objects to retrieve config properties and to use it
 * 	as a scratchpad during serialize and parse actions.
 *
 * @see ContextFactory
 */
public abstract class Session {

	/**
	 * Default constructor.
	 *
	 * @param ctx The context creating this session object.
	 * 	The context contains all the configuration settings for the session.
	 */
	protected Session(Context ctx) {}
}
