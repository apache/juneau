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
package org.apache.juneau.reflect;

import java.lang.annotation.*;
import java.util.ArrayList;

/**
 * An ordered list of annotations and the classes/methods/packages they were found on.
 */
public class AnnotationList extends ArrayList<AnnotationInfo<?>> {
	private static final long serialVersionUID = 1L;

	@Override /* List */
	public boolean add(AnnotationInfo<?> ai) {
		if (accept(ai)) {
			super.add(ai);
			return true;
		}
		return false;
	}

	/**
	 * Overridable method for filtering annotations added to this list.
	 *
	 * @param a The annotation to check.
	 * @return <jk>true</jk> if annotation should be added to this list.
	 */
	protected boolean accept(AnnotationInfo<? extends Annotation> a) {
		return true;
	}
}
