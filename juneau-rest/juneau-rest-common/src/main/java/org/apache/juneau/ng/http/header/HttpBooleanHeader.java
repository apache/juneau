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

/**
 * Base for boolean headers.
 *
 * @since 9.2.1
 */
@SuppressWarnings({
	"java:S2160",
	"unchecked"
})
public class HttpBooleanHeader extends HttpHeaderBean {

	public static final int LAZY_WIRE_STRING = 0;
	public static final int LAZY_BOOLEAN = 1;

	private final Boolean value;
	private final Supplier<?> lazySupplier;
	private final int lazyMode;

	protected HttpBooleanHeader(String name, String wireValue) {
		super(name, wireValue);
		this.value = e(wireValue) ? null : Boolean.valueOf(bool(wireValue));
		this.lazySupplier = null;
		this.lazyMode = -1;
	}

	protected HttpBooleanHeader(String name, Boolean typedValue) {
		super(name, s(typedValue));
		this.value = typedValue;
		this.lazySupplier = null;
		this.lazyMode = -1;
	}

	protected HttpBooleanHeader(String name, Supplier<?> supplier, int lazyMode) {
		super(name, lazyMode == LAZY_WIRE_STRING
			? () -> ((Supplier<String>) supplier).get()
			: () -> s(((Supplier<Boolean>) supplier).get()));
		this.value = null;
		this.lazySupplier = supplier;
		this.lazyMode = lazyMode;
	}

	public Optional<Boolean> asBoolean() {
		return opt(toBoolean());
	}

	@Override
	public String getValue() {
		return s(toBoolean());
	}

	public boolean isTrue() {
		return Boolean.TRUE.equals(toBoolean());
	}

	public Boolean orElse(Boolean other) {
		var x = toBoolean();
		return nn(x) ? x : other;
	}

	public Boolean toBoolean() {
		if (lazyMode == LAZY_BOOLEAN)
			return ((Supplier<Boolean>) lazySupplier).get();
		if (value != null)
			return value;
		var v = super.getValue();
		return e(v) ? null : Boolean.valueOf(bool(v));
	}
}
