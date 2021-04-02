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

import java.util.*;

/**
 * Implements the <c>{@property review}</c> tag that identifies documentation in need of review.
 */
public class ReviewTag implements Taglet {

	private static final String NAME = "review";

	static final Set<String> TO_REVIEW = Collections.synchronizedSet(new LinkedHashSet<>());
	static {
		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				if (TO_REVIEW.size() > 0)
					System.out.println("[INFO] No files needing review");
				else
					System.out.println("[WARNING] " + TO_REVIEW.size() + " files needing review");

				for (String s : TO_REVIEW)
					System.out.println("[NEEDS-REVIEW] " + s);
			}
		});
	}

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
		ReviewTag tag = new ReviewTag();
		tagletMap.put(tag.getName(), tag);
	}

	@Override
	public String toString(Tag tag) {
		String label = tag.text();
		if (label == null || label.isEmpty())
			label = "Needs Review";

		String file = tag.position().file().getAbsolutePath().replaceFirst("\\/juneau\\/.*\\/java\\/", "/juneau/target/site/apidocs/").replace(".java", ".html");
		TO_REVIEW.add(file);
		return "<review>" + label + "</review>";
	}

	@Override
	public String toString(Tag[] tags) {
		return null;
	}
}