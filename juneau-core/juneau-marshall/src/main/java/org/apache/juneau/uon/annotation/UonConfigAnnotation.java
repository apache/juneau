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
package org.apache.juneau.uon.annotation;

import org.apache.juneau.*;
import org.apache.juneau.reflect.*;
import org.apache.juneau.svl.*;
import org.apache.juneau.uon.*;

/**
 * Utility classes and methods for the {@link UonConfig @UonConfig} annotation.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../../index.html#jm.UonDetails">UON Details</a>
 * </ul>
 */
public class UonConfigAnnotation {

	/**
	 * Applies {@link UonConfig} annotations to a {@link org.apache.juneau.uon.UonSerializer.Builder}.
	 */
	public static class SerializerApply extends AnnotationApplier<UonConfig,UonSerializer.Builder> {

		/**
		 * Constructor.
		 *
		 * @param vr The resolver for resolving values in annotations.
		 */
		public SerializerApply(VarResolverSession vr) {
			super(UonConfig.class, UonSerializer.Builder.class, vr);
		}

		@Override
		public void apply(AnnotationInfo<UonConfig> ai, UonSerializer.Builder b) {
			UonConfig a = ai.inner();

			bool(a.addBeanTypes()).ifPresent(x -> b.addBeanTypesUon(x));
			bool(a.encoding()).ifPresent(x -> b.encoding(x));
			string(a.paramFormat()).map(ParamFormat::valueOf).ifPresent(x -> b.paramFormat(x));
		}
	}

	/**
	 * Applies {@link UonConfig} annotations to a {@link org.apache.juneau.uon.UonParser.Builder}.
	 */
	public static class ParserApply extends AnnotationApplier<UonConfig,UonParser.Builder> {

		/**
		 * Constructor.
		 *
		 * @param vr The resolver for resolving values in annotations.
		 */
		public ParserApply(VarResolverSession vr) {
			super(UonConfig.class, UonParser.Builder.class, vr);
		}

		@Override
		public void apply(AnnotationInfo<UonConfig> ai, UonParser.Builder b) {
			UonConfig a = ai.inner();

			bool(a.decoding()).ifPresent(x -> b.decoding(x));
			bool(a.validateEnd()).ifPresent(x -> b.validateEnd(x));
		}
	}
}