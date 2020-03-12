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
package org.apache.juneau.plaintext;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.plaintext.annotation.*;

/**
 * Metadata on classes specific to the PlainText serializers and parsers pulled from the {@link PlainText @PlainText} annotation on
 * the class.
 */
public class PlainTextClassMeta extends ExtendedClassMeta {

	private final List<PlainText> plainTexts;

	/**
	 * Constructor.
	 *
	 * @param cm The class that this annotation is defined on.
	 * @param mp PlainText metadata provider (for finding information about other artifacts).
	 */
	public PlainTextClassMeta(ClassMeta<?> cm, PlainTextMetaProvider mp) {
		super(cm);
		this.plainTexts = cm.getAnnotations(PlainText.class);
	}

	/**
	 * Returns the {@link PlainText @PlainText} annotations defined on the class.
	 *
	 * @return An unmodifiable list of annotations ordered parent-to-child, or an empty list if not found.
	 */
	protected List<PlainText> getAnnotations() {
		return plainTexts;
	}
}
