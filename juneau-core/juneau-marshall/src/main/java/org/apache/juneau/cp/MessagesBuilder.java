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
package org.apache.juneau.cp;

import static org.apache.juneau.internal.StringUtils.*;
import static org.apache.juneau.internal.ResourceBundleUtils.*;

import java.util.*;

import org.apache.juneau.collections.*;
import org.apache.juneau.internal.*;

/**
 * Builder for {@link Messages} objects.
 */
public class MessagesBuilder {

	private Class<?> forClass;
	private Locale locale = Locale.getDefault();
	private String name;
	private Messages parent;

	private String[] baseNames = {"{package}.{name}","{package}.i18n.{name}","{package}.nls.{name}","{package}.messages.{name}"};

	MessagesBuilder(Class<?> forClass) {
		this.forClass = forClass;
		this.name = forClass.getSimpleName();
	}

	/**
	 * Adds a parent bundle.
	 *
	 * @param parent The parent bundle.  Can be <jk>null</jk>.
	 * @return This object (for method chaining).
	 */
	public MessagesBuilder parent(Messages parent) {
		this.parent = parent;
		return this;
	}

	/**
	 * Specifies the bundle name (e.g. <js>"Messages"</js>).
	 *
	 * @param name
	 * 	The bundle name.
	 * 	<br>If <jk>null</jk>, the forClass class name is used.
	 * @return This object (for method chaining).
	 */
	public MessagesBuilder name(String name) {
		this.name = isEmpty(name) ? forClass.getSimpleName() : name;
		return this;
	}

	/**
	 * Specifies the base name patterns to use for finding the resource bundle.
	 *
	 * @param baseNames
	 * 	The bundle base names.
	 * 	<br>The default is the following:
	 * 	<ul>
	 * 		<li><js>"{package}.{name}"</js>
	 * 		<li><js>"{package}.i18n.{name}"</js>
	 * 		<li><js>"{package}.nls.{name}"</js>
	 * 		<li><js>"{package}.messages.{name}"</js>
	 * 	</ul>
	 * @return This object (for method chaining).
	 */
	public MessagesBuilder baseNames(String...baseNames) {
		this.baseNames = baseNames == null ? new String[]{} : baseNames;
		return this;
	}

	/**
	 * Specifies the locale.
	 *
	 * @param locale
	 * 	The locale.
	 * 	If <jk>null</jk>, the default locale is used.
	 * @return This object (for method chaining).
	 */
	public MessagesBuilder locale(Locale locale) {
		this.locale = locale == null ? Locale.getDefault() : locale;
		return this;
	}

	/**
	 * Creates a new {@link Messages} based on the setting of this builder.
	 *
	 * @return A new {@link Messages} object.
	 */
	public Messages build() {
		return new Messages(forClass, getBundle(), locale, parent);
	}

	private ResourceBundle getBundle() {
		ClassLoader cl = forClass.getClassLoader();
		OMap m = OMap.of("name", name, "package", forClass.getPackage().getName());
		for (String bn : baseNames) {
			bn = StringUtils.replaceVars(bn, m);
			ResourceBundle rb = findBundle(bn, locale, cl);
			if (rb != null)
				return rb;
		}
		return null;
	}
}
