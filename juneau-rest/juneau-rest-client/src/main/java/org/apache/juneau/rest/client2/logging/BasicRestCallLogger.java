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

import java.util.logging.*;

import org.apache.juneau.rest.client2.*;

/**
 * Specialized interceptor for logging calls to a log file.
 *
 * <p>
 * Causes a log entry to be created that shows all the request and response headers and content at the end of the
 * request.
 *
 * <p>
 * Use the {@link RestClientBuilder#logTo(Level, Logger)} and {@link RestRequest#logTo(Level, Logger)} methods to create
 * instances of this class.
 */
public class BasicRestCallLogger extends RestCallLogger {

	/**
	 * Default HTTP request logger.
	 * <p>
	 * Logs outgoing HTTP requests to the <c>org.apache.juneau.rest.client</c> logger at <jsf>WARNING</jsf> level.
	 */
	public static final BasicRestCallLogger DEFAULT = new BasicRestCallLogger(Level.WARNING, Logger.getLogger("org.apache.juneau.rest.client"));

	private Level level;
	private Logger log;

	/**
	 * Constructor.
	 *
	 * @param level The log level to log messages at.
	 * @param log The logger to log to.
	 */
	public BasicRestCallLogger(Level level, Logger log) {
		this.level = level;
		this.log = log;
	}

	@Override
	public boolean shouldLog(RestRequest req, RestResponse res) {
		return log.isLoggable(level);
	}

	@Override
	public void log(Throwable t, String msg) {
		log.log(level, msg, t);
	}
}
