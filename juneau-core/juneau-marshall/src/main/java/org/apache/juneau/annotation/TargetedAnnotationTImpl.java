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
package org.apache.juneau.annotation;

import static org.apache.juneau.internal.ArrayUtils.*;

/**
 * An implementation of an annotation that has an <code>on</code> value targeting classes/methods/fields/constructors.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * </ul>
 */
public class TargetedAnnotationTImpl extends TargetedAnnotationImpl {

	private final Class<?>[] onClass;

	/**
	 * Constructor.
	 *
	 * @param b The builder used to instantiate the fields of this class.
	 */
	public TargetedAnnotationTImpl(TargetedAnnotationTBuilder b) {
		super(b);
		this.onClass = copyOf(b.onClass);
	}

	/**
	 * The targets this annotation applies to.
	 *
	 * @return The targets this annotation applies to.
	 */
	public Class<?>[] onClass() {
		return onClass;
	}
}
