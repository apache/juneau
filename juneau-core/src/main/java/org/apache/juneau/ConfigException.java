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
package org.apache.juneau;

import java.text.*;

/**
 * An exception that typically occurs when trying to perform an invalid operation on a configuration property.
 */
public class ConfigException extends FormattedRuntimeException {
	private static final long serialVersionUID = 1L;

	/**
	 * Constructor
	 *
	 * @param message The error message.
	 * @param args Optional {@link MessageFormat}-style arguments.
	 */
	public ConfigException(String message, Object...args) {
		super(message, args);
	}

	@Override
	public synchronized ConfigException initCause(Throwable t) {
		super.initCause(t);
		return this;
	}
}