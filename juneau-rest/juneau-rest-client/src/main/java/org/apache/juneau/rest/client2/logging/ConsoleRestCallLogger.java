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
package org.apache.juneau.rest.client2.logging;

import java.io.*;

import org.apache.http.*;
import org.apache.http.protocol.*;
import org.apache.juneau.rest.client2.*;

/**
 * Specialized interceptor for logging calls to the console.
 *
 * <p>
 * Causes a log entry to be created in the console that shows all the request and response headers and content at the end of the
 * request.
 */
public class ConsoleRestCallLogger extends RestCallLogger {

	/**
	 * Default HTTP request logger.
	 * <p>
	 * Logs outgoing HTTP requests to the <c>org.apache.juneau.rest.client</c> logger at <jsf>WARNING</jsf> level.
	 */
	public static final ConsoleRestCallLogger DEFAULT = new ConsoleRestCallLogger();

	/**
	 * Constructor.
	 */
	public ConsoleRestCallLogger() {}

	@Override
	public boolean shouldLog(RestRequest req, RestResponse res) {
		return true;
	}

	@Override
	public void log(Throwable t, String msg) {
		System.err.println(msg);
		if (t != null)
			t.printStackTrace(System.err);
	}
}
