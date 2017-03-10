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
 * Used to determine whether a request should be retried based on the HTTP response code.
 */
public interface RetryOn {

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
	 * @param httpResponseCode The HTTP response code.
	 * @return <jk>true</jk> if the specified response code is retryable.
	 */
	boolean onCode(int httpResponseCode);
}
