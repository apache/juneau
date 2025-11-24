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
package org.apache.juneau.common.annotation;

import static org.apache.juneau.common.utils.CollectionUtils.*;

/**
 * An implementation of an annotation that has an <code>on</code> value targeting classes/methods/fields/constructors.
 *
 */
public class TargetedAnnotationTImpl extends TargetedAnnotationImpl {

	private final Class<?>[] onClass;

	/**
	 * Constructor.
	 *
	 * @param b The builder used to instantiate the fields of this class.
	 */
	public TargetedAnnotationTImpl(TargetedAnnotationTBuilder<?> b) {
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