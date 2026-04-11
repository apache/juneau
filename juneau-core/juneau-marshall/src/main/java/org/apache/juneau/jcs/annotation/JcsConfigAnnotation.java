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
package org.apache.juneau.jcs.annotation;

import org.apache.juneau.*;
import org.apache.juneau.commons.reflect.*;
import org.apache.juneau.jcs.*;
import org.apache.juneau.svl.*;

/**
 * Utility classes and methods for the {@link JcsConfig @JcsConfig} annotation.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://www.rfc-editor.org/rfc/rfc8785">RFC 8785 — JSON Canonicalization Scheme</a>
 * </ul>
 */
public class JcsConfigAnnotation {

	private JcsConfigAnnotation() {}

	/**
	 * Applies {@link JcsConfig} annotations to a {@link org.apache.juneau.jcs.JcsSerializer.Builder}.
	 */
	public static class SerializerApply extends AnnotationApplier<JcsConfig,JcsSerializer.Builder> {

		/**
		 * Constructor.
		 *
		 * @param vr The resolver for resolving values in annotations.
		 */
		public SerializerApply(VarResolverSession vr) {
			super(JcsConfig.class, JcsSerializer.Builder.class, vr);
		}

		@Override
		public void apply(AnnotationInfo<JcsConfig> ai, JcsSerializer.Builder b) {
			// No-op: JCS uses fixed canonical output; annotation provides consistency and future extensibility
		}
	}
}
