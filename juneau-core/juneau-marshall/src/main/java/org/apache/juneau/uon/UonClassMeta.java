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
package org.apache.juneau.uon;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.uon.annotation.*;

/**
 * Metadata on classes specific to the UON serializers and parsers pulled from the {@link Uon @Uon} annotation on
 * the class.
 *
 * <ul class='seealso'>
 * 	<li class='link'>{@doc jm.UonDetails}
 * 	<li class='extlink'>{@source}
 * </ul>
 */
public class UonClassMeta extends ExtendedClassMeta {

	private final List<Uon> uons;

	/**
	 * Constructor.
	 *
	 * @param cm The class that this annotation is defined on.
	 * @param mp Uon metadata provider (for finding information about other artifacts).
	 */
	public UonClassMeta(ClassMeta<?> cm, UonMetaProvider mp) {
		super(cm);
		this.uons = cm.getAnnotations(Uon.class);
	}

	/**
	 * Returns the {@link Uon @Uon} annotations defined on the class.
	 *
	 * @return An unmodifiable list of annotations ordered parent-to-child, or an empty list if not found.
	 */
	protected List<Uon> getAnnotations() {
		return uons;
	}
}
