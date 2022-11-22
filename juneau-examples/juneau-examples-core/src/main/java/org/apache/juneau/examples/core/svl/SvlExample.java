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
package org.apache.juneau.examples.core.svl;

import org.apache.juneau.svl.*;

/**
 * Simple Variable Language examples.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * </ul>
 */
public class SvlExample {

	/**
	 * Main method.
	 *
	 * @param args Unused.
	 */
	public static void main(String[] args) {

		VarResolver vr = VarResolver.DEFAULT;

		// $E{key[,default]} for getting environment variables
		System.out.println(vr.resolve("JAVA_HOME=$E{JAVA_HOME, not defined}"));

		// $S{key[,default]} for getting system properties (uses System.getProperty() )
		System.out.println(vr.resolve("os.name=$S{os.name, not defined}"));

		// $IF{key[,default]} general if or if-else condition
		// $NE{arg} will return true if not empty
		System.out.println(vr.resolve("TEST_VAR is $IF{$NE{$E{TEST_VAR}}, not empty, empty}"));

		// $SW{arg,pattern1:then1[,pattern2:then2...]} switch-case
		System.out.println(vr.resolve("$SW{Carrot, *Ap*:Fruit, *Car*:Veg, *:N/A}"));

		// $PR{arg,pattern,replace} pattern replace
		System.out.println(vr.resolve("Java version=$PR{$S{java.version}, (_([0-9]+)), \\ build=\\$2}"));

		// $UC{arg} uppercase $LC{arg} lowecase
		System.out.println(vr.resolve("$LC{JAVA_HOME} $UC{$E{JAVA_HOME}}"));

		// $LN{arg[,delimiter]} length var example
		System.out.println(vr.resolve("parts = $LN{$S{os.version},.}, charcount = $LN{$S{os.version}}"));

		// $ST{arg,start[,end]} substring var example
		System.out.println(vr.resolve("version = $ST{$S{java.version}, 0, 3}"));

		// $PE{arg,start[,end]} pattern extractor var example
		System.out.println(vr.resolve("update = $PE{$S{java.version},_([0-9]+),1}"));

		/*
		 *  See all supported variable types at,
		 *  http://juneau.apache.org/site/apidocs-8.0.0/overview-summary.html#juneau-marshall.SimpleVariableLanguage
		 *  NOTE - juneau-marshall.SimpleVariableLanguage supports nested variables well
		 * */
	}

}
