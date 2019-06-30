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
 * Implements the <c>{@doc source}</c> tag.
 */
public class SourceTag implements Taglet {

	private static final String NAME = "source";
	private static final String GITHUB_LINK = "https://github.com/apache/juneau/blob/master";

	private static volatile String JUNEAU_ROOT;

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
		SourceTag tag = new SourceTag();
		tagletMap.put(tag.getName(), tag);
	}

	@Override
	public String toString(Tag tag) {
		File f = tag.position().file();
		String label = tag.text();
		if (label == null || label.isEmpty())
			label = "Source";

		// Locate root directory as the one containing RELEASE-NOTES.txt
		if (JUNEAU_ROOT == null) {
			File f2 = f;
			while (true) {
				f2 = f2.getParentFile();
				if (f2 == null)
					break;
				File f3 = new File(f2, "RELEASE-NOTES.txt");
				if (f3.exists()) {
					JUNEAU_ROOT = f2.getAbsolutePath();
					break;
				}
			}
		}

		if (JUNEAU_ROOT == null)
			return label;

		String path = f.getAbsolutePath();
		String href = GITHUB_LINK + path.substring(JUNEAU_ROOT.length());

		return "<a class='doclink' target='_blank' href='" + href + "'>" + label + "</a>";
	}

	@Override
	public String toString(Tag[] tags) {
		return null;
	}
}