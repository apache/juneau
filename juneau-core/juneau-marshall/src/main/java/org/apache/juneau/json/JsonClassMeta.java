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
package org.apache.juneau.json;

import static org.apache.juneau.common.internal.StringUtils.*;

import org.apache.juneau.*;
import org.apache.juneau.json.annotation.*;

/**
 * Metadata on classes specific to the JSON serializers and parsers pulled from the {@link Json @Json} annotation on
 * the class.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../index.html#jm.JsonDetails">JSON Details</a>
 * </ul>
 */
public class JsonClassMeta extends ExtendedClassMeta {

	private final String wrapperAttr;

	/**
	 * Constructor.
	 *
	 * @param cm The class that this annotation is defined on.
	 * @param mp JSON metadata provider (for finding information about other artifacts).
	 */
	public JsonClassMeta(ClassMeta<?> cm, JsonMetaProvider mp) {
		super(cm);

		Value<String> wrapperAttr = Value.empty();
		cm.forEachAnnotation(Json.class, x -> isNotEmpty(x.wrapperAttr()), x -> wrapperAttr.set(x.wrapperAttr()));
		this.wrapperAttr = wrapperAttr.orElse(null);
	}

	/**
	 * Returns the {@link Json#wrapperAttr() @Json(wrapperAttr)} annotation defined on the class.
	 *
	 * @return The value of the annotation, or <jk>null</jk> if not specified.
	 */
	protected String getWrapperAttr() {
		return wrapperAttr;
	}
}
