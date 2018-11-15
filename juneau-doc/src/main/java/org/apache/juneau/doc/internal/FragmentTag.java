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
 * Implements the <code>{@fragment name}</code> tag that resolves to a fragment file located in the resources/fragments folder.
 */
public class FragmentTag implements Taglet {

	private static final String NAME = "fragment";

	private static final String FRAGMENTS = "resources/fragments";

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
		FragmentTag tag = new FragmentTag();
		tagletMap.put(tag.getName(), tag);
	}

	@Override
	public String toString(Tag tag) {
		String name = tag.text();
		String s = null;
		try {
			s = IOUtils.read(new File(FRAGMENTS + "/" + name));
			if (name.endsWith(".html"))
				s = s.replaceAll("(?s)\\<\\!\\-\\-.*?\\-\\-\\>", "");
		} catch (IOException e) {
			s = e.getLocalizedMessage();
			e.printStackTrace(System.err);
		}
		if (s == null)
			System.err.println("Unknown fragment '"+tag.text()+"'");
		return s == null ? tag.text() : s;
	}

	@Override
	public String toString(Tag[] tags) {
		return null;
	}
}