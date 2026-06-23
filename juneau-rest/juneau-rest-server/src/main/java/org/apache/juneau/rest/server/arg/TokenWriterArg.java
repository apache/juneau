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

import org.apache.juneau.commons.reflect.*;
import org.apache.juneau.http.response.*;
import org.apache.juneau.marshall.serializer.*;
import org.apache.juneau.marshall.stream.*;
import org.apache.juneau.rest.server.*;

/**
 * Resolves method parameters of type {@link TokenWriter} (or any subtype) on
 * {@link RestOp}-annotated Java methods.
 *
 * <p>
 * Implements two-layer resolution: static parameter type filters eligible serializers; response
 * {@code Accept} negotiates among them.  The cursor is opened on a request-configured
 * {@link SerializerSession SerializerSession} so it honors the
 * same locale / timezone / schema / charset as a normal serialized response.
 *
 * <p>
 * Declaring this parameter flips the response into "user owns the body" mode.
 *
 * <p>
 * Rejection paths (all surfaced as HTTP 415 / 406):
 * <ul>
 *   <li><b>No serializer matched the {@code Accept}</b> &mdash; resolution returns {@code null} and
 *       the framework rejects the request (415/406 via the empty match).
 *   <li><b>Matched serializer does not implement {@link TokenWritable}</b> &mdash; throws
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
public class TokenWriterArg extends SimpleRestOperationArg {

	/**
	 * Static creator.
	 *
	 * @param paramInfo The Java method parameter being resolved.
	 * @return A new {@link TokenWriterArg}, or <jk>null</jk> if the parameter type is not
	 * 	{@link TokenWriter} or a subtype.
	 */
	public static TokenWriterArg create(ParameterInfo paramInfo) {
		var pt = paramInfo.getParameterType();
		if (!pt.isAssignableTo(TokenWriter.class))
			return null;
		return new TokenWriterArg(pt.inner());
	}

	private final Class<?> declaredType;

	/** Constructor. */
	protected TokenWriterArg(Class<?> declaredType) {
		super(opSession -> resolve(opSession, declaredType));
		this.declaredType = declaredType;
	}

	private static Object resolve(RestOpSession opSession, Class<?> declaredType) throws Exception {
		// HTT: all branches require a live RestOpSession with response serializer infrastructure;
		//      covered by integration tests in juneau-integration-tests.
		var req = opSession.getRequest();
		var res = opSession.getResponse();
		var match = res.getSerializerMatch();
		if (match.isEmpty())
			return null;
		var serializer = match.get().getSerializer();
		if (!(serializer instanceof TokenWritable))
			throw new UnsupportedMediaType(
				"Serializer ''{0}'' (matched on Accept) does not support the token-writer surface.",
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
		var cursor = ((TokenWritable) session).serializeTokens(output);
		if (!declaredType.isInstance(cursor))
			throw new UnsupportedMediaType(
				"Serializer ''{0}'' produced cursor type ''{1}'' which is not assignable to the declared parameter type ''{2}''.",
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
