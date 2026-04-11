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
package org.apache.juneau.jsonl.annotation;

import org.apache.juneau.*;
import org.apache.juneau.commons.reflect.*;
import org.apache.juneau.json.*;
import org.apache.juneau.svl.*;

/**
 * Utility classes and methods for the {@link JsonlConfig @JsonlConfig} annotation.
 */
public class JsonlConfigAnnotation {

	private JsonlConfigAnnotation() {}

	/**
	 * Applies {@link JsonlConfig} annotations to a {@link org.apache.juneau.json.JsonParser.Builder}.
	 */
	public static class ParserApply extends AnnotationApplier<JsonlConfig, JsonParser.Builder> {

		/**
		 * Constructor.
		 *
		 * @param vr The resolver for resolving values in annotations.
		 */
		public ParserApply(VarResolverSession vr) {
			super(JsonlConfig.class, JsonParser.Builder.class, vr);
		}

		@Override
		public void apply(AnnotationInfo<JsonlConfig> ai, JsonParser.Builder b) {
			// No-op: JSONL reuses JsonParser.Builder; no format-specific settings initially
		}
	}

	/**
	 * Applies {@link JsonlConfig} annotations to a {@link org.apache.juneau.json.JsonSerializer.Builder}.
	 */
	public static class SerializerApply extends AnnotationApplier<JsonlConfig, JsonSerializer.Builder> {

		/**
		 * Constructor.
		 *
		 * @param vr The resolver for resolving values in annotations.
		 */
		public SerializerApply(VarResolverSession vr) {
			super(JsonlConfig.class, JsonSerializer.Builder.class, vr);
		}

		@Override
		public void apply(AnnotationInfo<JsonlConfig> ai, JsonSerializer.Builder b) {
			// No-op: JSONL reuses JsonSerializer.Builder; no format-specific settings initially
		}
	}
}
