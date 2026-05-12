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
package org.apache.juneau.commons.bean;

/**
 * Utility classes and methods for the {@link BeanConfig @BeanConfig} annotation.
 *
 * <p>
 * This class is intentionally a metadata-only placeholder in the bean-modeling layer.
 * The actual application logic that pushes {@link BeanConfig @BeanConfig} attributes into a
 * marshalling context builder lives in {@code juneau-marshall} (in the sibling
 * {@code org.apache.juneau.annotation.BeanConfigAnnotation.Applier} class), since
 * {@code juneau-commons} cannot depend on {@code juneau-marshall}.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='ja'>{@link BeanConfig}
 * </ul>
 */
public class BeanConfigAnnotation {

	/**
	 * Prevents instantiation.
	 */
	private BeanConfigAnnotation() {}
}
