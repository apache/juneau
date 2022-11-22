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

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.*;

import java.lang.annotation.*;

import org.apache.juneau.*;

/**
 * Applied to Config annotations to identify the class used to push the values into a property store.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='jc'>{@link AnnotationApplier}
 * </ul>
 */
@Documented
@Target({ANNOTATION_TYPE})
@Retention(RUNTIME)
@Inherited
public @interface ContextApply {

	/**
	 * Identifies the class used to push values from an annotation into a property store.
	 *
	 * @return The annotation value.
	 */
	@SuppressWarnings("rawtypes")
	public Class<? extends AnnotationApplier>[] value();
}
