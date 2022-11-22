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
package org.apache.juneau.rest.logger;

import static java.util.logging.Level.*;
import static org.apache.juneau.rest.logger.CallLoggingDetail.*;

import org.apache.juneau.cp.*;
import org.apache.juneau.rest.*;

/**
 * Basic implementation of a call logger.
 *
 * <h5 class='section'>Configured Settings:</h5>
 * <ul>
 * 	<li>Logs to the {@link RestContext#getLogger() context logger}.
 * 	<li>Only calls with status code &gt;=400 will be logged.
 * 	<li>Logs full request and response entity.
 * </ul>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../../index.html#jrs.LoggingAndDebugging">Logging / Debugging</a>
 * </ul>
 */
public class BasicCallLogger extends CallLogger {

	/**
	 * Constructor using specific settings.
	 *
	 * @param beanStore The bean store containing injectable beans for this logger.
	 */
	public BasicCallLogger(BeanStore beanStore) {
		super(beanStore);
	}

	/**
	 * Constructor using default settings.
	 * <p>
	 * Uses the same settings as {@link CallLogger}.
	 */
	public BasicCallLogger() {
		super(BeanStore.INSTANCE);
	}

	@Override
	protected Builder init(BeanStore beanStore) {
		return super.init(beanStore)
			.normalRules(  // Rules when debugging is not enabled.
				CallLoggerRule.create(beanStore)  // Log 500+ errors with status-line and header information.
					.statusFilter(x -> x >= 500)
					.level(SEVERE)
					.requestDetail(HEADER)
					.responseDetail(HEADER)
					.build(),
				CallLoggerRule.create(beanStore)  // Log 400-500 errors with just status-line information.
					.statusFilter(x -> x >= 400)
					.level(WARNING)
					.requestDetail(STATUS_LINE)
					.responseDetail(STATUS_LINE)
					.build()
			)
			.debugRules(  // Rules when debugging is enabled.
				CallLoggerRule.create(beanStore)  // Log everything with full details.
					.level(SEVERE)
					.requestDetail(ENTITY)
					.responseDetail(ENTITY)
					.build()
			)
		;
	}
}
