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
package org.apache.juneau.doc.internal;

import static org.apache.juneau.doc.internal.Console.*;

import com.sun.tools.doclets.Taglet;
import com.sun.javadoc.*;

import java.io.*;
import java.util.Map;

/**
 * Implements the <c>{@doc link}</c> tag.
 */
public class DocTag implements Taglet {

	private static final String NAME = "doc";
	private static final DocStore STORE = new DocStore(new File("resources/docs.txt"));

	@Override
	public String getName() {
		return NAME;
	}

	@Override
	public boolean inField() {
		return true;
	}

	@Override
	public boolean inConstructor() {
		return true;
	}

	@Override
	public boolean inMethod() {
		return true;
	}

	@Override
	public boolean inOverview() {
		return true;
	}

	@Override
	public boolean inPackage() {
		return true;
	}

	@Override
	public boolean inType() {
		return true;
	}

	@Override
	public boolean isInlineTag() {
		return true;
	}

	@SuppressWarnings({ "javadoc", "rawtypes", "unchecked" })
	public static void register(Map tagletMap) {
		DocTag tag = new DocTag();
		tagletMap.put(tag.getName(), tag);
	}

	@Override
	public String toString(Tag tag) {
		File f = tag.position().file();
		String key = tag.text();
		String href = null;
		String label = null;

		int i = key.indexOf(' ');
		if (key.indexOf(' ') != -1) {
			label = key.substring(i + 1);
			key = key.substring(0, i).trim();
		}

		String hrefRemainder = "";

		if (key.matches("^\\w+\\:\\/\\/.*")) {
			href = key;
			if (label == null)
				label = href;
		} else if (key.startsWith("org.apache.juneau")) {
			i = firstDelimiter(key, 0);
			if (i == -1)
				href = key.replace('.', '/') + "/package-summary.html";
			else
				href = key.substring(0, i).replace('.', '/') + "/package-summary.html" + key.substring(i);
			if (label == null)
				label = href;
		} else {
			i = firstDelimiter(key, 0);
			if (i != -1) {
				hrefRemainder = key.substring(i);
				key = key.substring(0, i);
			}
			DocStore.Link l = STORE.getLink(key);
			if (l == null) {
				error("Unknown doc tag: {0}", key);
				return tag.text();
			}
			href = l.href;
			if (label == null)
				label = l.label;
		}

		if (label == null)
			label = "link";
		label = label.replace("<", "&lt;").replace(">", "&gt;");

		if (href.startsWith("#") && !f.getName().equals("overview.html")) {
			StringBuilder sb = new StringBuilder();
			while (true) {
				f = f.getParentFile();
				if (f == null) {
					error("Unknown doc tag href: {0}", tag.text());
					return tag.text();
				}
				if (f.getName().equals("java"))
					break;
				sb.append("../");
			}
			sb.append("overview-summary.html" + href);
			href = sb.toString();
		}
		return "<a class='doclink' href='" + href + hrefRemainder + "'>" + label + "</a>";
	}

	@Override
	public String toString(Tag[] tags) {
		return null;
	}

	private int firstDelimiter(String path, int from) {
		int i = path.indexOf('/', from);
		if (i == -1)
			i = path.indexOf('#', from);
		return i;
	}
}