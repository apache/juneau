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
package org.apache.juneau.svl.vars;

import static org.apache.juneau.internal.ThrowableUtils.*;

import org.apache.juneau.svl.*;

/**
 * TODO
 * 
 */
public class PatternReplaceVar extends MultipartVar {

	/** The name of this variable. */
	public static final String NAME = "PR";

	/**
	 * Constructor.
	 */
	public PatternReplaceVar() {
		super(NAME);
	}

	@Override /* MultipartVar */
	public String resolve(VarResolverSession session, String[] args) {
		if (args.length < 3)
			illegalArg("Invalid number of arguments passed to $PR var.  Must have 3 or more arguments.");

		String stringArg = args[0];
		String pattern = args[1];
		String replace = args[2];
		
		pattern = pattern.replace("*", ".*").replace("?", ".");
		return stringArg.replaceAll(pattern, replace);
	}
}