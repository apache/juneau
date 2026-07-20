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
package org.apache.juneau.marshall.json5l;

import org.apache.juneau.commons.reflect.*;
import org.apache.juneau.commons.svl.*;
import org.apache.juneau.marshall.*;
import org.apache.juneau.marshall.json.*;

/**
 * Utility classes and methods for the {@link Json5lConfig @Json5lConfig} annotation.
 */
public class Json5lConfigAnnotation {

	private Json5lConfigAnnotation() {}

	/**
	 * Applies {@link Json5lConfig} annotations to a {@link org.apache.juneau.marshall.json.JsonParser.Builder}.
	 */
	@SuppressWarnings({
		"rawtypes" // Raw types required for reflective annotation application.
	})
	public static class ParserApply extends AnnotationApplier<Json5lConfig, JsonParser.Builder> {

		/**
		 * Constructor.
		 *
		 * @param vr The resolver for resolving values in annotations.
		 * 	<br>Must not be <jk>null</jk>.
		 */
		public ParserApply(VarResolverSession vr) {
			super(Json5lConfig.class, JsonParser.Builder.class, vr);
		}

		@Override
		public void apply(AnnotationInfo<Json5lConfig> ai, JsonParser.Builder b) {
			// No-op: JSON5L reuses JsonParser.Builder; no format-specific settings initially
		}
	}

	/**
	 * Applies {@link Json5lConfig} annotations to a {@link org.apache.juneau.marshall.json.JsonSerializer.Builder}.
	 */
	@SuppressWarnings({
		"rawtypes" // Raw types required for reflective annotation application.
	})
	public static class SerializerApply extends AnnotationApplier<Json5lConfig, JsonSerializer.Builder> {

		/**
		 * Constructor.
		 *
		 * @param vr The resolver for resolving values in annotations.
		 * 	<br>Must not be <jk>null</jk>.
		 */
		public SerializerApply(VarResolverSession vr) {
			super(Json5lConfig.class, JsonSerializer.Builder.class, vr);
		}

		@Override
		public void apply(AnnotationInfo<Json5lConfig> ai, JsonSerializer.Builder b) {
			// No-op: JSON5L reuses JsonSerializer.Builder; no format-specific settings initially
		}
	}
}
