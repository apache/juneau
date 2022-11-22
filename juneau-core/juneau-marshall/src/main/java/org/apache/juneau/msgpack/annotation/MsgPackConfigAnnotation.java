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
package org.apache.juneau.msgpack.annotation;

import org.apache.juneau.*;
import org.apache.juneau.msgpack.*;
import org.apache.juneau.reflect.*;
import org.apache.juneau.svl.*;

/**
 * Utility classes and methods for the {@link MsgPackConfig @MsgPackConfig} annotation.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../../index.html#jm.MsgPackDetails">Overview &gt; juneau-marshall &gt; MessagePack Details</a>
 * </ul>
 */
public class MsgPackConfigAnnotation {

	/**
	 * Applies {@link MsgPackConfig} annotations to a {@link org.apache.juneau.msgpack.MsgPackSerializer.Builder}.
	 */
	public static class SerializerApply extends AnnotationApplier<MsgPackConfig,MsgPackSerializer.Builder> {

		/**
		 * Constructor.
		 *
		 * @param vr The resolver for resolving values in annotations.
		 */
		public SerializerApply(VarResolverSession vr) {
			super(MsgPackConfig.class, MsgPackSerializer.Builder.class, vr);
		}

		@Override
		public void apply(AnnotationInfo<MsgPackConfig> ai, MsgPackSerializer.Builder b) {
			MsgPackConfig a = ai.inner();

			bool(a.addBeanTypes()).ifPresent(x -> b.addBeanTypesMsgPack(x));
		}
	}

	/**
	 * Applies {@link MsgPackConfig} annotations to a {@link org.apache.juneau.msgpack.MsgPackParser.Builder}.
	 */
	public static class ParserApply extends AnnotationApplier<MsgPackConfig,MsgPackParser.Builder> {

		/**
		 * Constructor.
		 *
		 * @param vr The resolver for resolving values in annotations.
		 */
		public ParserApply(VarResolverSession vr) {
			super(MsgPackConfig.class, MsgPackParser.Builder.class, vr);
		}

		@Override
		public void apply(AnnotationInfo<MsgPackConfig> ai, MsgPackParser.Builder b) {
		}
	}
}