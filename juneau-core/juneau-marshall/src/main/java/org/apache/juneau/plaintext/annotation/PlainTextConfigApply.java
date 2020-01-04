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

import static org.apache.juneau.BeanContext.*;

import org.apache.juneau.*;
import org.apache.juneau.reflect.*;
import org.apache.juneau.svl.*;

/**
 * Applies {@link PlainTextConfig} annotations to a {@link PropertyStoreBuilder}.
 */
public class PlainTextConfigApply extends ConfigApply<PlainTextConfig> {

	/**
	 * Constructor.
	 *
	 * @param c The annotation class.
	 * @param r The resolver for resolving values in annotations.
	 */
	public PlainTextConfigApply(Class<PlainTextConfig> c, VarResolverSession r) {
		super(c, r);
	}

	@Override
	public void apply(AnnotationInfo<PlainTextConfig> ai, PropertyStoreBuilder psb) {
		PlainTextConfig a = ai.getAnnotation();

		if (a.annotatePlainText().length > 0)
			psb.addTo(BEAN_annotations, a.annotatePlainText());
	}
}
