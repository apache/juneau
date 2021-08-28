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

import javax.servlet.http.*;

import org.apache.juneau.rest.annotation.*;

/**
 * Interface used for selectively turning on debug per request.
 */
public interface DebugEnablement {

	/**
	 * Represents no DebugEnablement.
	 */
	public abstract class Null implements DebugEnablement {};

	/**
	 * Creator.
	 *
	 * @return A new builder for this object.
	 */
	public static DebugEnablementBuilder create() {
		return new DebugEnablementBuilder();
	}

	/**
	 * Returns <jk>true</jk> if debug is enabled on the specified class and request.
	 * 
	 * <p>
	 * This enables debug mode on requests once the matched class is found and before the
	 * Java method is found.
	 *
	 * @param context The context of the {@link Rest}-annotated class.
	 * @param req The HTTP request.
	 * @return <jk>true</jk> if debug is enabled on the specified method and request.
	 */
	public boolean isDebug(RestContext context, HttpServletRequest req);

	/**
	 * Returns <jk>true</jk> if debug is enabled on the specified method and request.
	 *
	 * <p>
	 * This enables debug mode after the Java method is found and allows you to enable
	 * debug on individual Java methods instead of the entire class.
	 * 
	 * @param context The context of the {@link RestOp}-annotated method.
	 * @param req The HTTP request.
	 * @return <jk>true</jk> if debug is enabled on the specified method and request.
	 */
	public boolean isDebug(RestOpContext context, HttpServletRequest req);
}
