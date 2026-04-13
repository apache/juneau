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
package org.apache.juneau.ng.http.header;


import org.apache.juneau.commons.http.MediaType;
import java.util.function.*;


/**
 * Represents an HTTP <c>Content-Type</c> header.
 *
 * @since 9.2.1
 */
public class ContentType extends HttpMediaTypeHeader {

	public static final String NAME = "Content-Type";

	public ContentType(String value) {
		super(NAME, value);
	}

	public ContentType(MediaType value) {
		super(NAME, value);
	}

	private ContentType(Supplier<?> supplier, int lazyMode) {
		super(NAME, supplier, lazyMode);
	}

	public static ContentType of(String value) {
		return new ContentType(value);
	}

	public static ContentType of(MediaType value) {
		return new ContentType(value);
	}

	public static ContentType ofLazyWire(Supplier<String> supplier) {
		return new ContentType(supplier, LAZY_WIRE_STRING);
	}

	public static ContentType ofLazyParsed(Supplier<MediaType> supplier) {
		return new ContentType(supplier, LAZY_MEDIA_TYPE);
	}
}
