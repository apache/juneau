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

import org.apache.http.*;

/**
 * Used to determine whether a request should be retried based on the HTTP response code.
 *
 * <p>
 * Subclasses should override either the {@link #onCode(int)} method (if you only care about the HTTP status code)
 * or {@link #onResponse(HttpResponse)} (if you want full access to the HTTP response object.
 */
public abstract class RetryOn {

	/**
	 * Default RetryOn that returns <jk>true</jk> of any HTTP response &gt;= 400 is received.
	 */
	public static final RetryOn DEFAULT = new RetryOn() {
		@Override /* RetryOn */
		public boolean onCode(int httpResponseCode) {
			return httpResponseCode <= 0 || httpResponseCode >= 400;
		}
	};

	/**
	 * Default RetryOn that returns <jk>true</jk> if the HTTP connection could not be made.
	 */
	public static final RetryOn CONNECTION_LOST = new RetryOn() {
		@Override /* RetryOn */
		public boolean onCode(int httpResponseCode) {
			return httpResponseCode <= 0;
		}
	};

	/**
	 * Subclasses should override this method to determine whether the HTTP response is retryable.
	 *
	 * @param response The HTTP response object.  May be <jk>null</jk> if a connection could not be made.
	 * @return <jk>true</jk> if the specified response code is retryable.
	 */
	protected boolean onResponse(HttpResponse response) {
		return onCode(response == null || response.getStatusLine() == null ? -1 : response.getStatusLine().getStatusCode());
	}

	/**
	 * Subclasses should override this method to determine whether the HTTP response is retryable.
	 *
	 * @param httpResponseCode The HTTP response code.  <code>-1</code> if a connection could not be made.
	 * @return <jk>true</jk> if the specified response code is retryable.
	 */
	protected boolean onCode(int httpResponseCode) {
		return false;
	}
}
