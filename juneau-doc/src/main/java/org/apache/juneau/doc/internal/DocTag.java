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

import com.sun.tools.doclets.Taglet;
import com.sun.javadoc.*;

import java.io.*;
import java.util.Map;

/**
 * Implements the <code>{@doc link}</code> tag.
 */
public class DocTag implements Taglet {

	private static final String NAME = "doc";
	private static final DocStore STORE = new DocStore(new File("../../../juneau-doc/docs.txt"));

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
		DocStore.Link l = STORE.getLink(key);
		if (l == null) {
			System.err.println("Unknown doc tag '" + key + "'");
			return tag.text();
		}
		href = l.href;
		if (label == null)
			label = l.label;
		if (href.startsWith("#") && !f.getName().equals("overview.html")) {
			StringBuilder sb = new StringBuilder();
			while (true) {
				f = f.getParentFile();
				if (f == null)
					System.err.println("Unknown doc tag href: " + tag.text());
				if (f == null || f.getName().equals("java"))
					break;
				sb.append("../");
			}
			sb.append("overview-summary.html" + href);
			href = sb.toString();
		}
		return "<a class='doclink' href='" + href + "'>" + label + "</a>";
	}

	@Override
	public String toString(Tag[] tags) {
		return null;
	}
}