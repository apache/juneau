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
 * Implements the <c>{@del xxx}</c> tag that identifies a dead link
 */
public class DelTag implements Taglet {

	private static final String NAME = "del";

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
		DelTag tag = new DelTag();
		tagletMap.put(tag.getName(), tag);
	}

	@Override
	public String toString(Tag tag) {
		String label = tag.text();
		int i = label.indexOf(' ');
		if (i != -1) {
			label = label.substring(i+1);
		} else {
			i = label.indexOf('#');
			if (i != -1) {
				i = label.lastIndexOf('.');
				if (i != -1) {
					label = label.substring(i+1);
				}
				label = label.replace('#','.');
			}
		}

		label = label.replace('#','.');
		return "<dc>" + label + "</dc>";
	}

	@Override
	public String toString(Tag[] tags) {
		return null;
	}
}