/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.juneau.rest.server.arg;

import java.io.*;

import org.apache.juneau.commons.reflect.*;
import org.apache.juneau.http.response.*;
import org.apache.juneau.marshall.parser.*;
import org.apache.juneau.marshall.stream.*;
import org.apache.juneau.rest.server.*;

/**
 * Resolves method parameters of type {@link RecordReader} (or any subtype) on
 * {@link RestOp}-annotated Java methods.
 *
 * <p>
 * The parameter value is resolved by:
 * <ol>
 *   <li>Negotiating the request's {@code Content-Type} to a matched {@link Parser}.
 *   <li>Building a request-configured {@link ParserSession ParserSession}
 *       and calling {@link RecordReadable#readRecords(Object) readRecords(input)} on it where
 *       {@code input} is the request reader (text formats) or input stream (binary formats).
 *   <li>Verifying the returned cursor's runtime type is assignable to the declared parameter
 *       type &mdash; this enforces the two-layer resolution for concrete cursor subtypes
 *       (e.g. {@link org.apache.juneau.marshall.json.JsonTokenReader JsonTokenReader}).
 * </ol>
 *
 * <p>
 * Rejection paths (all surfaced as HTTP 415 / 406):
 * <ul>
 *   <li><b>No parser matched the {@code Content-Type}</b> &mdash; resolution returns {@code null} and
 *       the framework rejects the request (415/406 via the empty match).
 *   <li><b>Matched parser does not implement {@link RecordReadable}</b> &mdash; throws
 *       {@link UnsupportedMediaType} (415).
 *   <li><b>Matched parser produces a cursor not assignable to the declared parameter type</b>
 *       &mdash; throws {@link UnsupportedMediaType} (415).
 * </ul>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JavaMethodParameters">Java Method Parameters</a>
 * </ul>
 */
@SuppressWarnings({
	"resource" // Cursor methods return Closeables owned by the caller; Eclipse JDT @Owning warning is by design.
})
public class RecordReaderArg extends SimpleRestOperationArg {

	/**
	 * Static creator.
	 *
	 * @param paramInfo The Java method parameter being resolved. Must not be <jk>null</jk>.
	 * @return A new {@link RecordReaderArg}, or <jk>null</jk> if the parameter type is not
	 * 	{@link RecordReader} or a subtype (other than {@link TokenReader} &mdash; that case is
	 * 	handled by {@link TokenReaderArg}).
	 */
	public static RecordReaderArg create(ParameterInfo paramInfo) {
		var pt = paramInfo.getParameterType();
		// Only handle RecordReader and direct RecordReader-but-not-TokenReader subtypes.
		// TokenReader-and-subtypes go through TokenReaderArg.
		if (!pt.isAssignableTo(RecordReader.class))
			return null;
		if (pt.isAssignableTo(TokenReader.class))
			return null;
		return new RecordReaderArg(pt.inner());
	}

	private final Class<?> declaredType;

	/**
	 * Constructor.
	 *
	 * @param declaredType The declared parameter type (used to verify the matched parser's
	 * 	cursor type is assignable to the parameter).
	 */
	protected RecordReaderArg(Class<?> declaredType) {
		super(opSession -> resolve(opSession, declaredType));
		this.declaredType = declaredType;
	}

	private static Object resolve(RestOpSession opSession, Class<?> declaredType) throws IOException, ParseException {
		var req = opSession.getRequest();
		var content = req.getContent();
		var match = content.getParserMatch();
		if (match.isEmpty())
			return null;
		var parser = match.get().getParser();
		if (!(parser instanceof RecordReadable))
			throw new UnsupportedMediaType(
				"Parser '%s' (matched on Content-Type) does not support the record-reader surface.",
				parser.getClass().getName());
		// Build a request-configured session so the cursor honors the same config as a parsed body
		// (locale / timezone / schema / charset), then open the cursor on the session.
		var session = parser
			.createSession()
			.properties(req.getParserSessionPropertyMap())
			.javaMethod(req.getOpContext().getJavaMethod())
			.locale(req.getLocale())
			.timeZone(req.getTimeZone().orElse(null))
			.mediaType(match.get().getMediaType())
			.apply(ReaderParser.Builder.class, x -> x.streamCharset(req.getCharset()))
			.debug(req.isDebug() ? true : null)
			.build();
		var input = session.isReaderParser() ? content.getReader() : content.getInputStream();
		var cursor = ((RecordReadable) session).readRecords(input);
		if (!declaredType.isInstance(cursor))
			throw new UnsupportedMediaType(
				"Parser '%s' produced cursor type '%s' which is not assignable to the declared parameter type '%s'.",
				parser.getClass().getName(),
				cursor == null ? "null" : cursor.getClass().getName(),
				declaredType.getName());
		return cursor;
	}

	/** Returns the declared parameter type for diagnostic / introspection use. */
	public Class<?> getDeclaredType() {
		return declaredType;
	}
}
