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
package org.apache.juneau.xml;

import static org.apache.juneau.internal.StringUtils.*;

import java.text.*;

import javax.xml.stream.*;

import org.apache.juneau.parser.ParseException;

/**
 * Exception that indicates invalid syntax encountered during XML parsing.
 */
@SuppressWarnings("serial")
public class XmlParseException extends ParseException {

	/**
	 * Constructor.
	 *
	 * @param location The location of the exception.
	 * @param message The exception message containing {@link MessageFormat}-style arguments.
	 * @param args Optional {@link MessageFormat}-style arguments.
	 */
	public XmlParseException(Location location, String message, Object...args) {
		super(getMessage(location, message, args));
	}

	private static String getMessage(Location location, String msg, Object... args) {
		if (args.length != 0)
			msg = format(msg, args);
		if (location != null)
			msg = "Parse exception occurred at " + location + ".  " + msg;
		return msg;
	}
}
