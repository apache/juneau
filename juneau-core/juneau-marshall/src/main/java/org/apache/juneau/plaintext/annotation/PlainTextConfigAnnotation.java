// ***************************************************************************************************************************
// * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file *
// * distributed with this work for additional information regarding copyright ownership.  The ASF licenses this file        *
// * to you under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance            *
// * with the License.  You may obtain a copy of the License at                                                              *
// *                                                                                                                         *
// *  http://www.apache.org/licenses/LICENSE-2.0                                                                             *
// *                                                                                                                         *
// * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an  *
// * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the License for the        *
// * specific language governing permissions and limitations under the License.                                              *
// ***************************************************************************************************************************
package org.apache.juneau.plaintext.annotation;

import org.apache.juneau.*;
import org.apache.juneau.plaintext.*;
import org.apache.juneau.reflect.*;
import org.apache.juneau.svl.*;

/**
 * Utility classes and methods for the {@link PlainTextConfig @PlainTextConfig} annotation.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * </ul>
 */
public class PlainTextConfigAnnotation {

	/**
	 * Applies {@link PlainTextConfig} annotations to a {@link org.apache.juneau.plaintext.PlainTextSerializer.Builder}.
	 */
	public static class SerializerApply extends AnnotationApplier<PlainTextConfig,PlainTextSerializer.Builder> {

		/**
		 * Constructor.
		 *
		 * @param vr The resolver for resolving values in annotations.
		 */
		public SerializerApply(VarResolverSession vr) {
			super(PlainTextConfig.class, PlainTextSerializer.Builder.class, vr);
		}

		@Override
		public void apply(AnnotationInfo<PlainTextConfig> ai, PlainTextSerializer.Builder b) {
		}
	}

	/**
	 * Applies {@link PlainTextConfig} annotations to a {@link org.apache.juneau.plaintext.PlainTextParser.Builder}.
	 */
	public static class ParserApply extends AnnotationApplier<PlainTextConfig,PlainTextParser.Builder> {

		/**
		 * Constructor.
		 *
		 * @param vr The resolver for resolving values in annotations.
		 */
		public ParserApply(VarResolverSession vr) {
			super(PlainTextConfig.class, PlainTextParser.Builder.class, vr);
		}

		@Override
		public void apply(AnnotationInfo<PlainTextConfig> ai, PlainTextParser.Builder b) {
		}
	}
}