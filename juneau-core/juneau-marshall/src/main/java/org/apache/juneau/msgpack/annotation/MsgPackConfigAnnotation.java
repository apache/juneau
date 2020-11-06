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

import static org.apache.juneau.BeanContext.*;
import static org.apache.juneau.msgpack.MsgPackSerializer.*;

import org.apache.juneau.*;
import org.apache.juneau.reflect.*;
import org.apache.juneau.svl.*;

/**
 * Utility classes and methods for the {@link MsgPackConfig @MsgPackConfig} annotation.
 */
public class MsgPackConfigAnnotation {

	/**
	 * Applies {@link MsgPackConfig} annotations to a {@link PropertyStoreBuilder}.
	 */
	public static class Apply extends ConfigApply<MsgPackConfig> {

		/**
		 * Constructor.
		 *
		 * @param c The annotation class.
		 * @param r The resolver for resolving values in annotations.
		 */
		public Apply(Class<MsgPackConfig> c, VarResolverSession r) {
			super(c, r);
		}

		@Override
		public void apply(AnnotationInfo<MsgPackConfig> ai, PropertyStoreBuilder psb) {
			MsgPackConfig a = ai.getAnnotation();

			if (! a.addBeanTypes().isEmpty())
				psb.set(MSGPACK_addBeanTypes, bool(a.addBeanTypes()));

			if (a.applyMsgPack().length > 0)
				psb.prependTo(BEAN_annotations, a.applyMsgPack());
		}
	}
}