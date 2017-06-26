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
package org.apache.juneau.dto.atom;

import java.util.*;

import org.apache.juneau.annotation.*;

/**
 * Represents an <code>atomSource</code> construct in the RFC4287 specification.
 * <p>
 * <h6 class='figure'>Schema</h6>
 * <p class='bcode'>
 * 	atomSource =
 * 		element atom:source {
 * 			atomCommonAttributes,
 * 			(atomAuthor*
 * 			&amp; atomCategory*
 * 			&amp; atomContributor*
 * 			&amp; atomGenerator?
 * 			&amp; atomIcon?
 * 			&amp; atomId?
 * 			&amp; atomLink*
 * 			&amp; atomLogo?
 * 			&amp; atomRights?
 * 			&amp; atomSubtitle?
 * 			&amp; atomTitle?
 * 			&amp; atomUpdated?
 * 			&amp; extensionElement*)
 * 		}
 * </p>
 *
 * <h6 class='topic'>Additional Information</h6>
 * <ul class='doctree'>
 * 	<li class='link'><a class='doclink' href='../../../../../overview-summary.html#DTOs'>Juneau Data Transfer Objects
 * 		(org.apache.juneau.dto)</a>
 * 	<ul>
 * 		<li class='sublink'><a class='doclink' href='../../../../../overview-summary.html#DTOs.Atom'>Atom</a>
 * 	</ul>
 * 	<li class='jp'><a class='doclink' href='package-summary.html#TOC'>org.apache.juneau.dto.atom</a>
 * </ul>
 */
@SuppressWarnings("hiding")
public class Source extends CommonEntry {

	private Generator generator;
	private Icon icon;
	private Logo logo;
	private Text subtitle;


	//--------------------------------------------------------------------------------
	// Bean properties
	//--------------------------------------------------------------------------------

	/**
	 * Returns the generator info of this source.
	 *
	 * @return The generator info of this source.
	 */
	public Generator getGenerator() {
		return generator;
	}

	/**
	 * Sets the generator info of this source.
	 *
	 * @param generator The generator info of this source.
	 * @return This object (for method chaining).
	 */
	@BeanProperty("generator")
	public Source generator(Generator generator) {
		this.generator = generator;
		return this;
	}

	/**
	 * Returns the icon of this source.
	 *
	 * @return The icon of this source.
	 */
	public Icon getIcon() {
		return icon;
	}

	/**
	 * Sets the icon of this source.
	 *
	 * @param icon The icon of this source.
	 * @return This object (for method chaining).
	 */
	@BeanProperty("icon")
	public Source icon(Icon icon) {
		this.icon = icon;
		return this;
	}

	/**
	 * Returns the logo of this source.
	 *
	 * @return The logo of this source.
	 */
	public Logo getLogo() {
		return logo;
	}

	/**
	 * Sets the logo of this source.
	 *
	 * @param logo The logo of this source.
	 * @return This object (for method chaining).
	 */
	@BeanProperty("logo")
	public Source logo(Logo logo) {
		this.logo = logo;
		return this;
	}

	/**
	 * Returns the subtitle of this source.
	 *
	 * @return The subtitle of this source.
	 */
	public Text getSubtitle() {
		return subtitle;
	}

	/**
	 * Sets the subtitle of this source.
	 *
	 * @param subtitle The subtitle of this source.
	 * @return This object (for method chaining).
	 */
	@BeanProperty("subtitle")
	public Source subtitle(Text subtitle) {
		this.subtitle = subtitle;
		return this;
	}

	/**
	 * Sets the subtitle of this source.
	 *
	 * @param subtitle The subtitle of this source.
	 * @return This object (for method chaining).
	 */
	@BeanProperty("subtitle")
	public Source subtitle(String subtitle) {
		this.subtitle = new Text(subtitle);
		return this;
	}


	//--------------------------------------------------------------------------------
	// Overridden setters (to simplify method chaining)
	//--------------------------------------------------------------------------------

	@Override /* CommonEntry */
	public Source authors(Person...authors) {
		super.authors(authors);
		return this;
	}

	@Override /* CommonEntry */
	public Source categories(Category...categories) {
		super.categories(categories);
		return this;
	}

	@Override /* CommonEntry */
	public Source contributors(Person...contributors) {
		super.contributors(contributors);
		return this;
	}

	@Override /* CommonEntry */
	public Source id(Id id) {
		super.id(id);
		return this;
	}

	@Override /* CommonEntry */
	public Source links(Link...links) {
		super.links(links);
		return this;
	}

	@Override /* CommonEntry */
	public Source rights(Text rights) {
		super.rights(rights);
		return this;
	}

	@Override /* CommonEntry */
	public Source rights(String rights) {
		super.rights(rights);
		return this;
	}

	@Override /* CommonEntry */
	public Source title(Text title) {
		super.title(title);
		return this;
	}

	@Override /* CommonEntry */
	public Source title(String title) {
		super.title(title);
		return this;
	}

	@Override /* CommonEntry */
	public Source updated(Calendar updated) {
		super.updated(updated);
		return this;
	}

	@Override /* CommonEntry */
	public Source updated(String updated) {
		super.updated(updated);
		return this;
	}

	@Override /* Common */
	public Source base(Object base) {
		super.base(base);
		return this;
	}

	@Override /* Common */
	public Source lang(String lang) {
		super.lang(lang);
		return this;
	}
}
