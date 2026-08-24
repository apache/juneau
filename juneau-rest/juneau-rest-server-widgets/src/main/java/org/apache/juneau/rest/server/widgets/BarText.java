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
package org.apache.juneau.rest.server.widgets;

import org.apache.juneau.commons.bean.*;

/**
 * A static contextual label widget in a {@link BarSlot}.
 *
 * @since 10.0.0
 */
@BeanType(properties="id,text")
public final class BarText implements BarWidget {

	/** The stable widget id, unique within its {@link BarSlot}.  Required, non-blank. */
	public String id;

	/** The label text painted as {@code textContent}.  Required, non-blank. */
	public String text;

	/**
	 * Creates a bar text widget.
	 *
	 * @param id The stable widget id.  Must not be <jk>null</jk> or blank.
	 * @param text The label text.  Must not be <jk>null</jk> or blank.
	 * @return A new {@link BarText}.
	 */
	public static BarText of(String id, String text) {
		var b = new BarText();
		b.id = id;
		b.text = text;
		return b;
	}
}
