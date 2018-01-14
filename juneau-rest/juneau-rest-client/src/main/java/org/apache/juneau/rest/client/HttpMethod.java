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
package org.apache.juneau.rest.client;

/**
 * Enumeration of HTTP methods.
 */
public enum HttpMethod {

	/** HTTP GET */
	GET(false),

	/** HTTP PUT */
	PUT(true),

	/** HTTP POST */
	POST(true),

	/** HTTP DELETE */
	DELETE(false),

	/** HTTP OPTIONS */
	OPTIONS(false),

	/** HTTP HEAD */
	HEAD(false),

	/** HTTP TRACE */
	TRACE(false),

	/** HTTP CONNECT */
	CONNECT(false),

	/** HTTP MOVE */
	MOVE(false);

	private boolean hasContent;

	HttpMethod(boolean hasContent) {
		this.hasContent = hasContent;
	}

	/**
	 * Returns whether this HTTP method normally has content.
	 * 
	 * @return <jk>true</jk> if this HTTP method normally has content.
	 */
	public boolean hasContent() {
		return hasContent;
	}
}
