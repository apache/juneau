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

import static org.apache.juneau.commons.utils.Shorts.*;

import java.util.*;

import org.apache.juneau.commons.bean.*;

/**
 * The leading brand cluster of an {@link AppHeaderDef}: an optional logo tile and a title with optional trailing
 * crumb segments.
 *
 * <p>
 * Crumbs are painted with {@code textContent} and joined by the existing {@code .jc-brand-sep} separator span the
 * renderer inserts &mdash; never author HTML.
 *
 * @since 10.0.0
 */
@BeanType(properties="logo,title,crumbs")
public class Brand {

	/** Whether to render the {@code .jc-logo} tile.  Default (when unset) is {@code true}. */
	public Boolean logo;

	/** The primary brand title ({@code .jc-brand-title}), painted as {@code textContent}. */
	public String title;

	/** Optional trailing crumb segments, {@code .jc-brand-sep}-separated; each painted as {@code textContent}. */
	public List<String> crumbs;

	/**
	 * Creates an empty brand cluster.
	 *
	 * @return A new {@link Brand}.
	 */
	public static Brand create() {
		return new Brand();
	}

	/**
	 * Sets whether the logo tile renders.
	 *
	 * @param value <jk>true</jk> to render the {@code .jc-logo} tile.
	 * @return This object.
	 */
	public Brand logo(boolean value) {
		logo = value;
		return this;
	}

	/**
	 * Sets the primary brand title.
	 *
	 * @param value The title text.
	 * @return This object.
	 */
	public Brand title(String value) {
		title = value;
		return this;
	}

	/**
	 * Sets the trailing crumb segments.
	 *
	 * @param value The crumb segments, in display order.
	 * @return This object.
	 */
	public Brand crumbs(String...value) {
		crumbs = l(value);
		return this;
	}
}
