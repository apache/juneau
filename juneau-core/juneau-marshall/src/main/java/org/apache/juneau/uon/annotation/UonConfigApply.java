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

import static org.apache.juneau.uon.UonSerializer.*;
import static org.apache.juneau.uon.UonParser.*;
import org.apache.juneau.*;
import org.apache.juneau.utils.*;

/**
 * Applies {@link UonConfig} annotations to a {@link PropertyStoreBuilder}.
 */
public class UonConfigApply extends ConfigApply<UonConfig> {

	/**
	 * Constructor.
	 *
	 * @param c The annotation class.
	 * @param r The resolver for resolving values in annotations.
	 */
	public UonConfigApply(Class<UonConfig> c, StringResolver r) {
		super(c, r);
	}

	@Override
	public void apply(UonConfig a, PropertyStoreBuilder psb) {
		if (! a.addBeanTypes().isEmpty())
			psb.set(UON_addBeanTypes, bool(a.addBeanTypes()));
		if (! a.encoding().isEmpty())
			psb.set(UON_encoding, bool(a.encoding()));
		if (! a.paramFormat().isEmpty())
			psb.set(UON_paramFormat, string(a.paramFormat()));

		if (! a.decoding().isEmpty())
			psb.set(UON_decoding, bool(a.decoding()));
		if (! a.validateEnd().isEmpty())
			psb.set(UON_validateEnd, bool(a.validateEnd()));
	}
}
