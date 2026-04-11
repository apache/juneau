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
package org.apache.juneau.bson.annotation;

import org.apache.juneau.*;
import org.apache.juneau.commons.reflect.*;
import org.apache.juneau.bson.*;
import org.apache.juneau.svl.*;

/**
 * Utility classes and methods for the {@link BsonConfig @BsonConfig} annotation.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/BsonBasics">BSON Basics</a>
 * </ul>
 */
public class BsonConfigAnnotation {

	/**
	 * Prevents instantiation.
	 */
	private BsonConfigAnnotation() {}

	/**
	 * Applies {@link BsonConfig} annotations to a {@link org.apache.juneau.bson.BsonParser.Builder}.
	 */
	public static class ParserApply extends AnnotationApplier<BsonConfig,BsonParser.Builder> {

		/**
		 * Constructor.
		 *
		 * @param vr The resolver for resolving values in annotations.
		 */
		public ParserApply(VarResolverSession vr) {
			super(BsonConfig.class, BsonParser.Builder.class, vr);
		}

		@Override
		public void apply(AnnotationInfo<BsonConfig> ai, BsonParser.Builder b) {
			// No-op for parser
		}
	}

	/**
	 * Applies {@link BsonConfig} annotations to a {@link org.apache.juneau.bson.BsonSerializer.Builder}.
	 */
	public static class SerializerApply extends AnnotationApplier<BsonConfig,BsonSerializer.Builder> {

		/**
		 * Constructor.
		 *
		 * @param vr The resolver for resolving values in annotations.
		 */
		public SerializerApply(VarResolverSession vr) {
			super(BsonConfig.class, BsonSerializer.Builder.class, vr);
		}

		@Override
		public void apply(AnnotationInfo<BsonConfig> ai, BsonSerializer.Builder b) {
			BsonConfig a = ai.inner();

			bool(a.addBeanTypes()).ifPresent(b::addBeanTypesBson);
			bool(a.writeDatesAsDatetime()).ifPresent(b::writeDatesAsDatetime);
		}
	}
}
