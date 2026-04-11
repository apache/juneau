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


import static org.apache.juneau.commons.utils.Utils.*;

import java.util.*;
import java.util.function.*;

import org.apache.juneau.http.header.*;

/**
 * Base for multiple entity-tags headers.
 *
 * @since 9.2.1
 */
@SuppressWarnings({
	"java:S2160",
	"unchecked"
})
public class HttpEntityTagsHeader extends HttpHeaderBean {

	public static final int LAZY_WIRE_STRING = 0;
	public static final int LAZY_ENTITY_TAGS = 1;

	private final EntityTags value;
	private final Supplier<?> lazySupplier;
	private final int lazyMode;

	protected HttpEntityTagsHeader(String name, String wireValue) {
		super(name, wireValue);
		this.value = wireValue == null ? null : EntityTags.of(wireValue);
		this.lazySupplier = null;
		this.lazyMode = -1;
	}

	protected HttpEntityTagsHeader(String name, EntityTags typedValue) {
		super(name, s(typedValue));
		this.value = typedValue;
		this.lazySupplier = null;
		this.lazyMode = -1;
	}

	protected HttpEntityTagsHeader(String name, Supplier<?> supplier, int lazyMode) {
		super(name, lazyMode == LAZY_WIRE_STRING
			? () -> ((Supplier<String>) supplier).get()
			: () -> s(((Supplier<EntityTags>) supplier).get()));
		this.value = null;
		this.lazySupplier = supplier;
		this.lazyMode = lazyMode;
	}

	public Optional<EntityTags> asEntityTags() {
		return opt(toEntityTags());
	}

	@Override
	public String getValue() {
		return s(toEntityTags());
	}

	public EntityTags orElse(EntityTags other) {
		var x = toEntityTags();
		return nn(x) ? x : other;
	}

	public EntityTags toEntityTags() {
		if (lazyMode == LAZY_ENTITY_TAGS)
			return ((Supplier<EntityTags>) lazySupplier).get();
		if (value != null)
			return value;
		var v = super.getValue();
		return v == null ? null : EntityTags.of(v);
	}
}
