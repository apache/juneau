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
import org.apache.juneau.marshall.serializer.*;
import org.apache.juneau.marshall.stream.*;
import org.apache.juneau.rest.server.*;

/**
 * Resolves method parameters of type {@link RecordWriter} (or any subtype) on
 * {@link RestOp}-annotated Java methods.
 *
 * <p>
 * The parameter value is resolved by:
 * <ol>
 *   <li>Negotiating the response's {@code Accept} to a matched {@link Serializer}.
 *   <li>Building a request-configured {@link SerializerSession SerializerSession}
 *       and calling {@link RecordWritable#writeRecords(Object) writeRecords(output)} on it.
 *   <li>Verifying the returned cursor is assignable to the declared parameter type.
 * </ol>
 *
 * <p>
 * Declaring this parameter flips the response into "user owns the body" mode &mdash; the
 * framework will not later serialize a POJO return value over the same stream.  Same contract
 * as declaring an {@link OutputStream OutputStream} or {@link Writer Writer}
 * parameter today.
 *
 * <p>
 * Rejection paths (all surfaced as HTTP 415 / 406):
 * <ul>
 *   <li><b>No serializer matched the {@code Accept}</b> &mdash; resolution returns {@code null} and
 *       the framework rejects the request (415/406 via the empty match).
 *   <li><b>Matched serializer does not implement {@link RecordWritable}</b> &mdash; throws
 *       {@link UnsupportedMediaType} (415).
 *   <li><b>Matched serializer produces a cursor not assignable to the declared parameter type</b>
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
public class RecordWriterArg extends SimpleRestOperationArg {

	/**
	 * Static creator.
	 *
	 * @param paramInfo The Java method parameter being resolved.
	 * @return A new {@link RecordWriterArg}, or <jk>null</jk> if the parameter type is not
	 * 	{@link RecordWriter} or a subtype (other than {@link TokenWriter} &mdash; that case is
	 * 	handled by {@link TokenWriterArg}).
	 */
	public static RecordWriterArg create(ParameterInfo paramInfo) {
		var pt = paramInfo.getParameterType();
		if (!pt.isAssignableTo(RecordWriter.class))
			return null;
		if (pt.isAssignableTo(TokenWriter.class))
			return null;
		return new RecordWriterArg(pt.inner());
	}

	private final Class<?> declaredType;

	/** Constructor. */
	protected RecordWriterArg(Class<?> declaredType) {
		super(opSession -> resolve(opSession, declaredType));
		this.declaredType = declaredType;
	}

	private static Object resolve(RestOpSession opSession, Class<?> declaredType) throws IOException {
		var req = opSession.getRequest();
		var res = opSession.getResponse();
		var match = res.getSerializerMatch();
		if (match.isEmpty())
			return null;
		var serializer = match.get().getSerializer();
		if (!(serializer instanceof RecordWritable))
			throw new UnsupportedMediaType(
				"Serializer '%s' (matched on Accept) does not support the record-writer surface.",
				serializer.getClass().getName());
		var mediaType = res.getMediaType();
		if (mediaType == null)
			mediaType = match.get().getMediaType();
		// Build a request-configured session so the cursor honors the same config as a serialized
		// response (locale / timezone / schema / charset), then open the cursor on the session.
		var session = serializer
			.createSession()
			.properties(req.getSerializerSessionPropertyMap())
			.javaMethod(req.getOpContext().getJavaMethod())
			.locale(req.getLocale())
			.timeZone(req.getTimeZone().orElse(null))
			.mediaType(mediaType)
			.apply(WriterSerializerSession.Builder.class, x -> x.streamCharset(res.getCharset()))
			.debug(req.isDebug() ? true : null)
			.build();
		Object output = session.isWriterSerializer()
			? res.getNegotiatedWriter()
			: res.getNegotiatedOutputStream();
		var cursor = ((RecordWritable) session).writeRecords(output);
		if (!declaredType.isInstance(cursor))
			throw new UnsupportedMediaType(
				"Serializer '%s' produced cursor type '%s' which is not assignable to the declared parameter type '%s'.",
				serializer.getClass().getName(),
				cursor == null ? "null" : cursor.getClass().getName(),
				declaredType.getName());
		return cursor;
	}

	/** Returns the declared parameter type. */
	public Class<?> getDeclaredType() {
		return declaredType;
	}
}
