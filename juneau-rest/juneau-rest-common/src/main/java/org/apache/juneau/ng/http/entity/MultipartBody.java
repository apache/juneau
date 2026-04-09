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
package org.apache.juneau.ng.http.entity;

import static org.apache.juneau.commons.utils.AssertionUtils.assertArgNotNull;

import java.io.*;
import java.nio.charset.*;
import java.util.*;

import org.apache.juneau.ng.http.*;

/**
 * An {@link HttpBody} that serializes a {@code multipart/form-data} message (RFC 7578).
 *
 * <p>
 * Each part is a {@link MultipartPart} that carries a field name, an optional filename, an optional
 * per-part {@code Content-Type}, and a body writer.  When {@link #writeTo(OutputStream)} is called the
 * parts are streamed directly — no full-body buffering occurs.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	<jv>body</jv> = MultipartBody.<jsm>builder</jsm>()
 * 		.field(<js>"title"</js>, <js>"My Report"</js>)
 * 		.file(<js>"attachment"</js>, <jv>reportFile</jv>, <js>"application/pdf"</js>)
 * 		.build();
 * </p>
 *
 * <p>
 * {@link #isRepeatable()} returns {@code false} when any part wraps a one-shot stream, and {@code true}
 * when all parts are repeatable (e.g. strings, files, byte arrays).
 *
 * <p>
 * <b>Beta — API subject to change:</b> This type is part of the next-generation REST client and HTTP stack
 * ({@code org.apache.juneau.ng.*}).
 * It is not API-frozen: binary- and source-incompatible changes may appear in the <b>next major</b> Juneau release
 * (and possibly earlier).
 * For production use cases that require long-term binary stability, continue using the existing
 * {@code juneau-rest-client} and {@code juneau-rest-common} APIs until the {@code ng} stack is declared stable.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/juneau-ng-rest-client">juneau-ng REST client</a>
 * 	<li class='extlink'><a class="doclink" href="https://www.rfc-editor.org/rfc/rfc7578">RFC 7578 — Returning Values from Forms: multipart/form-data</a>
 * </ul>
 *
 * @since 9.2.1
 */
public final class MultipartBody implements HttpBody {

	private static final byte[] CRLF = "\r\n".getBytes(StandardCharsets.US_ASCII);
	private static final byte[] DASHDASH = "--".getBytes(StandardCharsets.US_ASCII);

	private final String boundary;
	private final List<MultipartPart> parts;

	private MultipartBody(String boundary, List<MultipartPart> parts) {
		this.boundary = boundary;
		this.parts = List.copyOf(parts);
	}

	/** Returns a new {@link Builder}. */
	public static Builder builder() {
		return new Builder();
	}

	@Override /* HttpBody */
	public String getContentType() {
		return "multipart/form-data; boundary=" + boundary;
	}

	/**
	 * Returns {@code -1} — multipart bodies are streamed without pre-computing the total length.
	 */
	@Override /* HttpBody */
	public long getContentLength() {
		return -1;
	}

	@Override /* HttpBody */
	public void writeTo(OutputStream out) throws IOException {
		var bnd = boundary.getBytes(StandardCharsets.US_ASCII);
		for (var part : parts) {
			out.write(DASHDASH);
			out.write(bnd);
			out.write(CRLF);
			// Content-Disposition header
			var cd = new StringBuilder("Content-Disposition: form-data; name=\"").append(part.name()).append('"');
			if (part.filename() != null)
				cd.append("; filename=\"").append(part.filename()).append('"');
			out.write(cd.toString().getBytes(StandardCharsets.UTF_8));
			out.write(CRLF);
			// Content-Type header (if present)
			if (part.contentType() != null) {
				out.write(("Content-Type: " + part.contentType()).getBytes(StandardCharsets.US_ASCII));
				out.write(CRLF);
			}
			out.write(CRLF);
			part.body().writeTo(out);
			out.write(CRLF);
		}
		// Final boundary
		out.write(DASHDASH);
		out.write(bnd);
		out.write(DASHDASH);
		out.write(CRLF);
	}

	@Override /* HttpBody */
	public boolean isRepeatable() {
		for (var part : parts)
			if (!part.body().isRepeatable())
				return false;
		return true;
	}

	/**
	 * Returns the multipart boundary string.
	 *
	 * @return The boundary. Never <jk>null</jk>.
	 */
	public String getBoundary() {
		return boundary;
	}

	/**
	 * Returns an unmodifiable view of the parts in this multipart body.
	 *
	 * @return The parts. Never <jk>null</jk>.
	 */
	public List<MultipartPart> getParts() {
		return parts;
	}

	// -----------------------------------------------------------------------------------------------------------------
	// Inner types
	// -----------------------------------------------------------------------------------------------------------------

	/**
	 * A single part within a {@link MultipartBody}.
	 *
	 * @since 9.2.1
	 */
	public record MultipartPart(String name, String filename, String contentType, HttpBody body) {

		/**
		 * Creates a text field part with no filename and {@code text/plain; charset=UTF-8} content type.
		 *
		 * @param name The field name. Must not be <jk>null</jk>.
		 * @param value The string value. Must not be <jk>null</jk>.
		 * @return A new instance. Never <jk>null</jk>.
		 */
		public static MultipartPart field(String name, String value) {
			return new MultipartPart(name, null, "text/plain; charset=UTF-8", StringBody.of(value, "text/plain; charset=UTF-8"));
		}

		/**
		 * Creates a file upload part.
		 *
		 * @param name The field name. Must not be <jk>null</jk>.
		 * @param file The file to upload. Must not be <jk>null</jk>.
		 * @param contentType The MIME content type for the file (e.g. {@code "application/pdf"}). May be <jk>null</jk>.
		 * @return A new instance. Never <jk>null</jk>.
		 */
		public static MultipartPart file(String name, File file, String contentType) {
			return new MultipartPart(name, file.getName(), contentType, FileBody.of(file, contentType));
		}

		/**
		 * Creates a part from an arbitrary {@link HttpBody}.
		 *
		 * @param name The field name. Must not be <jk>null</jk>.
		 * @param filename The filename to advertise in {@code Content-Disposition}. May be <jk>null</jk>.
		 * @param contentType The MIME content type. May be <jk>null</jk>.
		 * @param body The body content. Must not be <jk>null</jk>.
		 * @return A new instance. Never <jk>null</jk>.
		 */
		public static MultipartPart of(String name, String filename, String contentType, HttpBody body) {
			return new MultipartPart(name, filename, contentType, body);
		}
	}

	/**
	 * Fluent builder for {@link MultipartBody}.
	 *
	 * <p>
	 * <b>Beta — API subject to change.</b>
	 *
	 * @since 9.2.1
	 */
	public static final class Builder {

		private String boundary = UUID.randomUUID().toString().replace("-", "");
		private final List<MultipartPart> parts = new ArrayList<>();

		private Builder() {}

		/**
		 * Sets a custom multipart boundary string (defaults to a random UUID-derived value).
		 *
		 * @param value The boundary. Must not be <jk>null</jk> or blank.
		 * @return This object.
		 */
		public Builder boundary(String value) {
			boundary = assertArgNotNull("value", value);
			return this;
		}

		/**
		 * Adds a plain-text form field.
		 *
		 * @param name The field name. Must not be <jk>null</jk>.
		 * @param value The field value. Must not be <jk>null</jk>.
		 * @return This object.
		 */
		public Builder field(String name, String value) {
			parts.add(MultipartPart.field(name, value));
			return this;
		}

		/**
		 * Adds a file upload part.
		 *
		 * @param name The field name. Must not be <jk>null</jk>.
		 * @param file The file to upload. Must not be <jk>null</jk>.
		 * @param contentType The MIME content type for the file. May be <jk>null</jk>.
		 * @return This object.
		 */
		public Builder file(String name, File file, String contentType) {
			parts.add(MultipartPart.file(name, file, contentType));
			return this;
		}

		/**
		 * Adds a custom part.
		 *
		 * @param part The part to add. Must not be <jk>null</jk>.
		 * @return This object.
		 */
		public Builder part(MultipartPart part) {
			parts.add(assertArgNotNull("part", part));
			return this;
		}

		/**
		 * Builds and returns the {@link MultipartBody}.
		 *
		 * @return A new instance. Never <jk>null</jk>.
		 */
		public MultipartBody build() {
			return new MultipartBody(boundary, parts);
		}
	}
}
