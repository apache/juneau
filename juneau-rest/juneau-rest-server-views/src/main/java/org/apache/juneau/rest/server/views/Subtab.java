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

import org.apache.juneau.commons.bean.*;

/**
 * A single sub-tab within a {@link Tab} that has {@link Tab#subtabs}, referencing exactly one child {@link ViewDef}
 * (TODO-399 Phase C, design doc §"Bean model").
 *
 * <p>
 * A {@link Subtab} carries a stable {@code id} (the third hash segment, {@code #pageId/tabId/<subtabId>}) and
 * {@code label} (the sub-tab bar button text), plus the referenced {@link #view}.  Built via
 * {@link #create(String, String)} + {@link #view(ViewDef)}, mirroring the Phase B builder ergonomics.
 *
 * <h5 class='section'>See Also:</h5>
 * <ul>
 * 	<li class='jc'>{@link PageDef}
 * 	<li class='jc'>{@link Tab}
 * </ul>
 *
 * @since 10.0.0
 */
@BeanType(properties="id,label,view")
public class Subtab {

	/** The stable sub-tab id (the third hash segment). */
	public String id;

	/** The sub-tab bar button text. */
	public String label;

	/** The referenced child view. */
	public ViewDef view;

	/**
	 * Starts a new {@link Subtab} builder with the specified stable id and display label.
	 *
	 * @param id The stable sub-tab id.  Must not be <jk>null</jk> or blank.
	 * @param label The sub-tab bar button text.
	 * @return A new mutable {@link Subtab} to chain builder calls on.
	 */
	public static Subtab create(String id, String label) {
		if (id == null || id.isBlank())
			throw iaex("Subtab id must not be null or blank.");
		var s = new Subtab();
		s.id = id;
		s.label = label;
		return s;
	}

	/**
	 * Sets the referenced child view.
	 *
	 * @param value The built child view.  Must not be <jk>null</jk>.
	 * @return This object.
	 */
	public Subtab view(ViewDef value) {
		view = value;
		return this;
	}
}
