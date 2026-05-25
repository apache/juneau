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
package org.apache.juneau.rest.annotation;

import java.lang.annotation.*;

import org.apache.juneau.commons.annotation.*;
import org.apache.juneau.rest.debug.*;

/**
 * Utility classes and methods for {@link Debug}.
 */
public final class DebugAnnotation {

	private DebugAnnotation() {}

	public static class Builder extends AnnotationObject.Builder {
		private String value = "";
		private Class<? extends DebugFormat> format = DebugFormat.Void.class;
		private String level = "";
		private String on = "";
		private Class<? extends DebugConfig> config = DebugConfig.Void.class;

		protected Builder() {
			super(Debug.class);
		}

		public Builder value(String value) { this.value = value; return this; }
		public Builder format(Class<? extends DebugFormat> value) { this.format = value; return this; }
		public Builder level(String value) { this.level = value; return this; }
		public Builder on(String value) { this.on = value; return this; }
		public Builder config(Class<? extends DebugConfig> value) { this.config = value; return this; }
		public Debug build() { return new Object(this); }
	}

	private static class Object extends AnnotationObject implements Debug {
		private final String value;
		private final Class<? extends DebugFormat> format;
		private final String level;
		private final String on;
		private final Class<? extends DebugConfig> config;

		Object(DebugAnnotation.Builder b) {
			super(b);
			value = b.value;
			format = b.format;
			level = b.level;
			on = b.on;
			config = b.config;
		}

		@Override public String value() { return value; }
		@Override public Class<? extends DebugFormat> format() { return format; }
		@Override public String level() { return level; }
		@Override public String on() { return on; }
		@Override public Class<? extends DebugConfig> config() { return config; }
	}

	public static Builder create() { return new Builder(); }
	public static final Debug DEFAULT = create().build();
}
