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

/**
 * Represents an HTTP <c>If-Match</c> header.
 *
 * <p>
 * Request precondition on entity tags.
 *
 * <p>
 * <b>Beta — API subject to change:</b> This type is part of the next-generation REST client and HTTP stack
 * ({@code org.apache.juneau.ng.*}).
 *
 * @since 9.2.1
 */
import java.util.function.*;

import org.apache.juneau.http.header.*;

/**
 * @since 9.2.1
 */
public class IfMatch extends HttpEntityTagsHeader {

	public static final String NAME = "If-Match";

	public IfMatch(String value) {
		super(NAME, value);
	}

	public IfMatch(EntityTags value) {
		super(NAME, value);
	}

	private IfMatch(Supplier<?> supplier, int lazyMode) {
		super(NAME, supplier, lazyMode);
	}

	public static IfMatch of(String value) {
		return new IfMatch(value);
	}

	public static IfMatch of(EntityTags value) {
		return new IfMatch(value);
	}

	public static IfMatch ofLazyWire(Supplier<String> supplier) {
		return new IfMatch(supplier, LAZY_WIRE_STRING);
	}

	public static IfMatch ofLazyParsed(Supplier<EntityTags> supplier) {
		return new IfMatch(supplier, LAZY_ENTITY_TAGS);
	}
}
