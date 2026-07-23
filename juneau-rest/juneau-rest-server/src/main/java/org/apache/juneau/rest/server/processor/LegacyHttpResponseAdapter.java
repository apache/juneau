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
package org.apache.juneau.rest.server.processor;

import static org.apache.juneau.commons.utils.Shorts.*;

import java.io.*;
import java.lang.reflect.*;
import java.util.*;

import org.apache.juneau.http.*;
import org.apache.juneau.http.header.*;
import org.apache.juneau.http.response.*;

/**
 * Reflection-based adapter that bridges legacy {@code org.apache.http.HttpResponse} (Apache HttpClient 4.5)
 * objects to the modern {@link HttpResponseMessage} contract.
 *
 * <p>
 * This adapter exists so that user code returning legacy {@code org.apache.http.*} response types from
 * {@code @RestGet} methods continues to render correctly through the response-processor chain even though
 * {@code juneau-rest-server} no longer compiles against Apache HttpClient. The adapter uses pure
 * reflection — no compile-time dependency on the Apache HttpClient types — so this module remains
 * dependency-free against {@code org.apache.httpcomponents}.
 *
 * <p>
 * Detection: an object is considered a "legacy HTTP response" if it implements
 * {@code org.apache.http.HttpResponse}. Detection caches the loaded class once per VM.
 */
@SuppressWarnings({
	"java:S1192" // Duplicate string literals are HTTP header names and MIME type strings; intentional
})
final class LegacyHttpResponseAdapter {

	private static final Class<?> LEGACY_HTTP_RESPONSE = tryLoad("org.apache.http.HttpResponse");
	private static final Class<?> LEGACY_HTTP_ENTITY = tryLoad("org.apache.http.HttpEntity");
	private static final Class<?> LEGACY_HTTP_RESOURCE = tryLoad("org.apache.juneau.http.classic.resource.HttpResource");

	private LegacyHttpResponseAdapter() {}

	/**
	 * Returns <jk>true</jk> if the given object is an instance of the legacy
	 * {@code org.apache.http.HttpResponse} interface, indicating it should be adapted.
	 *
	 * @param obj The candidate object. May be <jk>null</jk>.
	 * @return <jk>true</jk> if adaptation is needed, otherwise <jk>false</jk>.
	 */
	static boolean isLegacyResponse(Object obj) {
		return obj != null && LEGACY_HTTP_RESPONSE != null && LEGACY_HTTP_RESPONSE.isInstance(obj);
	}

	/**
	 * Returns <jk>true</jk> if the given object is an instance of the legacy
	 * {@code org.apache.http.HttpEntity} interface.
	 *
	 * @param obj The candidate object. May be <jk>null</jk>.
	 * @return <jk>true</jk> if the object is a legacy entity, otherwise <jk>false</jk>.
	 */
	static boolean isLegacyEntity(Object obj) {
		return obj != null && LEGACY_HTTP_ENTITY != null && LEGACY_HTTP_ENTITY.isInstance(obj);
	}

	/**
	 * Adapts a legacy {@code org.apache.http.HttpResponse} into a {@link HttpResponseMessage}.
	 *
	 * @param legacy The legacy response. Must be an instance of {@code org.apache.http.HttpResponse}.
	 * @return A {@link HttpResponseMessage} view over the legacy object.
	 */
	static HttpResponseMessage adapt(Object legacy) {
		return new LegacyAdapter(legacy);
	}

	/**
	 * Adapts a legacy {@code org.apache.http.HttpEntity} into an {@link HttpBody}.
	 *
	 * @param legacyEntity The legacy entity. Must be an instance of {@code org.apache.http.HttpEntity}.
	 * @return An {@link HttpBody} view over the legacy entity.
	 */
	static HttpBody adaptEntity(Object legacyEntity) {
		return new LegacyEntityBody(legacyEntity);
	}

	/**
	 * If the given object is a legacy {@code org.apache.juneau.http.classic.resource.HttpResource}, copies
	 * each header onto the supplied consumer.
	 *
	 * @param legacyEntity The legacy entity. May be <jk>null</jk>.
	 * @param sink Receives each adapted {@link HttpHeader}. Must not be <jk>null</jk>.
	 */
	static void copyResourceHeaders(Object legacyEntity, java.util.function.Consumer<HttpHeader> sink) {
		if (legacyEntity == null || LEGACY_HTTP_RESOURCE == null || ! LEGACY_HTTP_RESOURCE.isInstance(legacyEntity))
			return;
		var hl = invoke(legacyEntity, "getHeaders", new Class<?>[0]);
		if (hl == null)
			return;
		Iterable<?> iter;
		if (hl instanceof Iterable<?> hl2) {
			iter = hl2;
		} else {
			var arr = (Object[]) invoke(hl, "getAll", new Class<?>[0]);
			if (arr == null)
				return;
			iter = Arrays.asList(arr);
		}
		for (var h : iter) {
			if (h == null)
				continue;
			var name = (String) invoke(h, "getName", new Class<?>[0]);
			var value = (String) invoke(h, "getValue", new Class<?>[0]);
			sink.accept(HttpStringHeader.of(name, value));
		}
	}

	private static Class<?> tryLoad(String name) {
		try {
			return Class.forName(name);
		} catch (ClassNotFoundException e) {
			return null;
		}
	}

	private static Object invoke(Object target, String method, Class<?>[] argTypes, Object...args) {
		try {
			var m = target.getClass().getMethod(method, argTypes);
			return m.invoke(target, args);
		} catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
			throw new IllegalStateException("Failed to invoke " + method + " on " + cn(target), e);
		}
	}

	// -----------------------------------------------------------------------------------------------
	// Inner adapter types.
	// -----------------------------------------------------------------------------------------------

	private static final class LegacyAdapter implements HttpResponseMessage {

		private final Object legacy;

		LegacyAdapter(Object legacy) {
			this.legacy = legacy;
		}

		@Override
		public HttpStatusLine getStatusLine() {
			var sl = invoke(legacy, "getStatusLine", new Class<?>[0]);
			if (sl == null)
				return HttpStatusLineBean.of(200, "OK");
			var statusCode = (int) invoke(sl, "getStatusCode", new Class<?>[0]);
			var reasonPhrase = (String) invoke(sl, "getReasonPhrase", new Class<?>[0]);
			return HttpStatusLineBean.of(statusCode, reasonPhrase);
		}

		@Override
		public List<HttpHeader> getHeaders() {
			var arr = (Object[]) invoke(legacy, "getAllHeaders", new Class<?>[0]);
			if (arr == null || arr.length == 0)
				return List.of();
			var out = new ArrayList<HttpHeader>(arr.length);
			for (var h : arr) {
				var name = (String) invoke(h, "getName", new Class<?>[0]);
				var value = (String) invoke(h, "getValue", new Class<?>[0]);
				out.add(isUriHeader(name) ? HttpUriHeader.of(name, value) : HttpStringHeader.of(name, value));
			}
			return out;
		}

		private static boolean isUriHeader(String name) {
			return name != null && (
				name.equalsIgnoreCase("Location")
				|| name.equalsIgnoreCase("Content-Location")
				|| name.equalsIgnoreCase("Referer"));
		}

		@Override
		public HttpBody getBody() {
			var entity = invoke(legacy, "getEntity", new Class<?>[0]);
			if (entity == null)
				return null;
			return new LegacyEntityBody(entity);
		}
	}

	private static final class LegacyEntityBody implements HttpBody {

		private final Object entity;

		LegacyEntityBody(Object entity) {
			this.entity = entity;
		}

		@Override
		public String getContentType() {
			var h = invoke(entity, "getContentType", new Class<?>[0]);
			return h == null ? null : (String) invoke(h, "getValue", new Class<?>[0]);
		}

		@Override
		public long getContentLength() {
			var v = invoke(entity, "getContentLength", new Class<?>[0]);
			return v == null ? -1 : ((Number) v).longValue();
		}

		@Override
		public void writeTo(OutputStream os) throws IOException {
			try {
				var m = entity.getClass().getMethod("writeTo", OutputStream.class);
				m.invoke(entity, os);
			} catch (NoSuchMethodException | IllegalAccessException e) {
				throw new IOException("Cannot invoke writeTo on legacy entity", e);
			} catch (InvocationTargetException e) {
				var t = e.getCause();
				if (t instanceof IOException t2) throw t2;
				if (t instanceof RuntimeException t2) throw t2;
				throw new IOException(t);
			}
		}
	}
}
