/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the
 * License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.juneau.http.header;

import static org.apache.juneau.commons.utils.StringUtils.*;
import static org.apache.juneau.commons.utils.ThrowableUtils.*;

import java.util.function.*;

import org.apache.juneau.commons.http.*;


/**
 * Represents an HTTP <c>Content-Disposition</c> header.
 *
 * <p>
 * Content disposition (inline / attachment / filename).
 *
 * <p>
 * <b>Beta — API subject to change:</b> This type is part of the next-generation REST client and HTTP stack
 * ({@code org.apache.juneau.ng.*}).
 *
 * @since 9.2.1
 */
public class ContentDisposition extends HttpStringRangesHeader {

	public static final String NAME = "Content-Disposition";

	public ContentDisposition(String value) {
		super(NAME, value);
	}

	public ContentDisposition(StringRanges value) {
		super(NAME, value);
	}

	private ContentDisposition(Supplier<?> supplier, int lazyMode) {
		super(NAME, supplier, lazyMode);
	}

	public static ContentDisposition of(String value) {
		return new ContentDisposition(value);
	}

	public static ContentDisposition of(StringRanges value) {
		return new ContentDisposition(value);
	}

	public static ContentDisposition ofLazyWire(Supplier<String> supplier) {
		return new ContentDisposition(supplier, LAZY_WIRE_STRING);
	}

	public static ContentDisposition ofLazyParsed(Supplier<StringRanges> supplier) {
		return new ContentDisposition(supplier, LAZY_STRING_RANGES);
	}

	/**
	 * Returns a {@code Content-Disposition} header value of the form {@code attachment; filename="<filename>"} with
	 * the file name properly escaped (backslashes and double-quotes are backslash-escaped).
	 *
	 * @param filename The attachment filename. Must not be {@code null}, blank, or contain CR/LF characters.
	 * @return A new {@code ContentDisposition} header.
	 * @throws IllegalArgumentException If {@code filename} is invalid.
	 */
	public static ContentDisposition attachment(String filename) {
		if (isBlank(filename))
			throw illegalArg("Attachment filename must not be null or blank.");
		for (var i = 0; i < filename.length(); i++) {
			var c = filename.charAt(i);
			if (c == '\r' || c == '\n')
				throw illegalArg("Attachment filename must not contain CR or LF characters.");
		}
		var sb = new StringBuilder(filename.length() + 8);
		for (var i = 0; i < filename.length(); i++) {
			var c = filename.charAt(i);
			if (c == '\\' || c == '"')
				sb.append('\\');
			sb.append(c);
		}
		return ContentDisposition.of("attachment; filename=\"" + sb + "\"");
	}
}
