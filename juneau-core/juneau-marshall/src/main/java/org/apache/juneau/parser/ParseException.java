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

import static org.apache.juneau.common.internal.StringUtils.*;

import java.lang.reflect.*;
import java.text.*;
import org.apache.juneau.*;
import org.apache.juneau.collections.*;
import org.apache.juneau.serializer.*;

/**
 * Exception that indicates invalid syntax encountered during parsing.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../index.html#jm.SerializersAndParsers">Serializers and Parsers</a>
 * </ul>
 *
 * @serial exclude
 */
public class ParseException extends BasicRuntimeException {

	private static final long serialVersionUID = 1L;

	/**
	 * Creator method.
	 *
	 * <p>
	 * If the throwable is already a {@link ParseException}, we simply return that exception as-is.
	 * If the throwable is an {@link InvocationTargetException}, we unwrap the thrown exception.
	 * Otherwise we create a new {@link ParseException}.
	 *
	 * @param e The exception being wrapped or unwrapped.
	 * @return A new {@link SerializeException}.
	 */
	public static ParseException create(Throwable e) {
		if (e instanceof InvocationTargetException)
			e = ((InvocationTargetException)e).getCause();
		if (e instanceof ParseException)
			return (ParseException)e;
		return new ParseException(e);
	}

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
			StringBuilder sb = new StringBuilder(msg);

			sb.append("\n\tAt: ").append(p);

			JsonMap lastLocation = session.getLastLocation();
			if (lastLocation != null) {
				sb.append("\n\tWhile parsing into: ");
				lastLocation.forEach((k,v) -> sb.append("\n\t\t").append(k).append(": ").append(v));
			}

			String lines = session.getInputAsString();
			if (lines == null)
				sb.append("\n\tUse BEAN_debug setting to display content.");
			else {
				int numLines = session.getDebugOutputLines();
				int start = p.line - numLines, end = p.line + numLines;
				sb.append("\n---start--\n").append(getNumberedLines(lines, start, end)).append("---end---");
			}

			msg = sb.toString();
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
