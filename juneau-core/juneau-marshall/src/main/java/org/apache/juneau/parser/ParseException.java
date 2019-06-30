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
package org.apache.juneau.parser;

import static org.apache.juneau.internal.StringUtils.*;

import java.text.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.internal.*;

/**
 * Exception that indicates invalid syntax encountered during parsing.
 */
public class ParseException extends FormattedException {

	private static final long serialVersionUID = 1L;

	/**
	 * Constructor.
	 *
	 * @param message The {@link MessageFormat}-style message.
	 * @param args Optional {@link MessageFormat}-style arguments.
	 */
	public ParseException(String message, Object...args) {
		super(message, args);
	}

	/**
	 * Constructor.
	 *
	 * @param causedBy The cause of this exception.
	 * @param message The {@link MessageFormat}-style message.
	 * @param args Optional {@link MessageFormat}-style arguments.
	 */
	public ParseException(Throwable causedBy, String message, Object...args) {
		super(causedBy, message, args);
	}

	/**
	 * Constructor.
	 *
	 * @param causedBy The cause of this exception.
	 */
	public ParseException(Throwable causedBy) {
		super(causedBy);
	}

	/**
	 * Constructor.
	 *
	 * @param session The parser session.
	 * @param message The exception message containing {@link MessageFormat}-style arguments.
	 * @param args Optional {@link MessageFormat}-style arguments.
	 */
	public ParseException(ParserSession session, String message, Object...args) {
		super(getMessage(session, message, args));
	}

	/**
	 * Constructor.
	 *
	 * @param session The parser session.
	 * @param causedBy The cause of this exception.
	 * @param message The exception message containing {@link MessageFormat}-style arguments.
	 * @param args Optional {@link MessageFormat}-style arguments.
	 */
	public ParseException(ParserSession session, Throwable causedBy, String message, Object...args) {
		super(causedBy, getMessage(session, message, args));
	}

	/**
	 * Constructor.
	 *
	 * @param session The parser session.
	 * @param causedBy The inner exception.
	 */
	public ParseException(ParserSession session, Exception causedBy) {
		super(causedBy, getMessage(session, causedBy.getMessage()));
	}


	private static String getMessage(ParserSession session, String msg, Object... args) {
		if (args.length != 0)
			msg = format(msg, args);

		if (session != null) {
			Position p = session.getPosition();

			msg += "\n\tAt: " + p;

			ObjectMap lastLocation = session.getLastLocation();
			if (lastLocation != null) {
				msg += "\n\tWhile parsing into: ";
				for (Map.Entry<String,Object> e : lastLocation.entrySet())
					msg += "\n\t\t" + e.getKey() + ": " + e.getValue();
			}

			String lines = session.getInputAsString();
			if (lines == null)
				msg += "\n\tUse BEAN_debug setting to display content.";
			else {
				int numLines = session.getDebugOutputLines();
				int start = p.line - numLines, end = p.line + numLines;
				msg += "\n---start--\n" + StringUtils.getNumberedLines(lines, start, end) + "---end---";
			}
		}
		return msg;
	}

	/**
	 * Returns the highest-level <c>ParseException</c> in the stack trace.
	 *
	 * <p>
	 * Useful for JUnit testing of error conditions.
	 *
	 * @return The root parse exception, or this exception if there isn't one.
	 */
	public ParseException getRootCause() {
		ParseException t = this;
		while (! (t.getCause() == null || ! (t.getCause() instanceof ParseException)))
			t = (ParseException)t.getCause();
		return t;
	}
}
