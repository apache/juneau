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
package org.apache.juneau.rest.server.views;

import static org.apache.juneau.commons.utils.Shorts.*;

/**
 * One field slot in a {@link DetailSection}.
 *
 * <p>
 * Values are filled from the expand GET JSON {@code fields} map via {@code textContent} only.  There is no
 * {@code render} field in this slice.
 *
 * @since 10.0.0
 */
public class DetailField {

	/** The key into the expand JSON {@code fields} map.  Unique across the whole {@link RowDetailDef}. */
	public String data;

	/** The label shown above the value slot. */
	public String title;

	/**
	 * Creates a field bound to the specified expand-JSON key.
	 *
	 * @param data The {@code fields} map key.  Must not be <jk>null</jk> or blank.
	 * @return A new {@link DetailField}.
	 */
	public static DetailField of(String data) {
		if (data == null || data.isBlank())
			throw iaex("DetailField data must not be null or blank.");
		var f = new DetailField();
		f.data = data;
		return f;
	}

	/**
	 * Sets the label shown above the value slot.
	 *
	 * @param value The label.  May be <jk>null</jk> (the {@link #data} key is used as a fallback at emit time).
	 * @return This object.
	 */
	public DetailField title(String value) {
		title = value;
		return this;
	}
}
