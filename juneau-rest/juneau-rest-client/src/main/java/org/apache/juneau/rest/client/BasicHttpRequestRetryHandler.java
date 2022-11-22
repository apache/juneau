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

import java.io.*;

import org.apache.http.impl.client.*;
import org.apache.http.protocol.*;

/**
 * An extension of {@link StandardHttpRequestRetryHandler} that adds support for a retry interval.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../../index.html#juneau-rest-client">juneau-rest-client</a>
 * </ul>
 */
public class BasicHttpRequestRetryHandler extends StandardHttpRequestRetryHandler {

	private final int retryInterval;

	/**
	 * Create the request retry handler.
	 *
	 * Uses the following list of non-retryable IOException classes:
	 * <ul>
	 * <li>InterruptedIOException</li>
	 * <li>UnknownHostException</li>
	 * <li>ConnectException</li>
	 * <li>SSLException</li>
	 * </ul>
	 *
	 * @param retryCount How many times to retry. <code>0</code> means no retries.
	 * @param retryInterval The time in milliseconds to sleep before trying again, or <code>&lt;=0</code> for no interval.
	 * @param requestSentRetryEnabled Specify <jk>true</jk> if it's OK to retry non-idempotent requests that have been sent.
	 */
	public BasicHttpRequestRetryHandler(int retryCount, int retryInterval, boolean requestSentRetryEnabled) {
		super(retryCount, requestSentRetryEnabled);
		this.retryInterval = retryInterval;
	}

	@Override
	public boolean retryRequest(IOException exception, int executionCount, HttpContext context) {
		if (retryInterval > 0) {
			try {
				Thread.sleep(retryInterval);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		return super.retryRequest(exception, executionCount, context);
	}
}
