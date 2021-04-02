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
 * Implements the <c>{@property reviewed}</c> tag that identifies documentation that has already been reviewed.
 */
public class ReviewedTag implements Taglet {

	private static final String NAME = "reviewed";

	static final Set<String> REVIEWED = Collections.synchronizedSet(new LinkedHashSet<>());
	static {
		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				System.out.println("[WARNING] " + REVIEWED.size() + " files reviewed.  Remove {@reviewed} tags before release.");
				for (String s : REVIEWED)
					System.out.println("[REVIEWED] " + s);
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
		ReviewedTag tag = new ReviewedTag();
		tagletMap.put(tag.getName(), tag);
	}

	@Override
	public String toString(Tag tag) {
		String label = tag.text();
		if (label == null || label.isEmpty())
			label = "Reviewed";

		String file = tag.position().file().getAbsolutePath();
		REVIEWED.add(file);
		return "<reviewed>" + label + "</reviewed>";
	}

	@Override
	public String toString(Tag[] tags) {
		return null;
	}
}