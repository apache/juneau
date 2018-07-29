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
package org.apache.juneau.rest.response;

import org.apache.juneau.http.annotation.*;

/**
 * Represents an <code>HTTP 304 Not Modified</code> response.
 *
 * <p>
 * Indicates that the resource has not been modified since the version specified by the request headers If-Modified-Since or If-None-Match.
 * In such case, there is no need to retransmit the resource since the client still has a previously-downloaded copy.
 */
@Response(code=304, example="'Not Modified'")
public class NotModified {

	/** Reusable instance. */
	public static final NotModified INSTANCE = new NotModified();

	@Override /* Object */
	public String toString() {
		return "Not Modified";
	}
}