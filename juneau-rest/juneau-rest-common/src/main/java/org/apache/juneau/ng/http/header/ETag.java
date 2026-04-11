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

import java.util.function.*;

import org.apache.juneau.http.header.*;

/**
 * Represents an HTTP <c>ETag</c> header.
 *
 * @since 9.2.1
 */
public class ETag extends HttpEntityTagHeader {

	public static final String NAME = "ETag";

	public ETag(String value) {
		super(NAME, value);
	}

	public ETag(EntityTag value) {
		super(NAME, value);
	}

	private ETag(Supplier<?> supplier, int lazyMode) {
		super(NAME, supplier, lazyMode);
	}

	public static ETag of(String value) {
		return new ETag(value);
	}

	public static ETag of(EntityTag value) {
		return new ETag(value);
	}

	public static ETag ofLazyWire(Supplier<String> supplier) {
		return new ETag(supplier, LAZY_WIRE_STRING);
	}

	public static ETag ofLazyParsed(Supplier<EntityTag> supplier) {
		return new ETag(supplier, LAZY_ENTITY_TAG);
	}
}
