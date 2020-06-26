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
package org.apache.juneau.rest.annotation;

import java.lang.annotation.*;

/**
 * TODO
 */
public class LoggingAnnotation implements Logging {

	private String
		disabled = "",
		level = "",
		stackTraceHashingTimeout = "",
		useStackTraceHashing = "";

	private LoggingRule[] rules = new LoggingRule[0];

	@Override /* Logging */
	public Class<? extends Annotation> annotationType() {
		return Logging.class;
	}

	@Override /* Logging */
	public String disabled() {
		return disabled;
	}

	@Override /* Logging */
	public String level() {
		return level;
	}

	@Override /* Logging */
	public LoggingRule[] rules() {
		return rules;
	}

	@Override /* Logging */
	public String stackTraceHashingTimeout() {
		return stackTraceHashingTimeout;
	}

	@Override /* Logging */
	public String useStackTraceHashing() {
		return useStackTraceHashing;
	}
}
