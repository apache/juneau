/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.juneau.examples.core.svl;

import org.apache.juneau.commons.logging.*;
import org.apache.juneau.commons.svl.*;

/**
 * Simple Variable Language examples.
 *
 */
public class SvlExample {

	/**
	 * Main method.
	 *
	 * @param args Unused.
	 */
	public static void main(String[] args) {

		var vr = VarResolver.DEFAULT;

		// $E{key[,default]} for getting environment variables
		System.out.println(vr.resolve("JAVA_HOME=$E{JAVA_HOME, not defined}"));

		// $S{key[,default]} for getting system properties (uses System.getProperty() )
		Logger.getLogger(SvlExample.class).info(vr.resolve("os.name=$S{os.name, not defined}"));

		// #{if(cond, then, else)} general if or if-else condition
		// #{notEmpty(s)} returns true if not empty
		Logger.getLogger(SvlExample.class).info(vr.resolve("TEST_VAR is #{if(#{notEmpty($E{TEST_VAR})}, not empty, empty)}"));

		// #{switch(value, pattern1, val1, ..., default)} glob-pattern switch-case
		System.out.println(vr.resolve("#{switch(Carrot, *Ap*, Fruit, *Car*, Veg, *, N/A)}"));

		// #{replaceRegex(s, regex, replacement)} pattern replace
		Logger.getLogger(SvlExample.class).info(vr.resolve("Java version=#{replaceRegex($S{java.version}, \"(_([0-9]+))\", \" build=$2\")}"));

		// #{upper(s)} / #{lower(s)} case conversion
		Logger.getLogger(SvlExample.class).info(vr.resolve("#{lower(JAVA_HOME)} #{upper($E{JAVA_HOME})}"));

		// #{len(s[,delimiter])} length / part count
		Logger.getLogger(SvlExample.class).info(vr.resolve("parts = #{len($S{os.version}, \".\")}, charcount = #{len($S{os.version})}"));

		// #{substring(s, start[, end])} substring extraction
		Logger.getLogger(SvlExample.class).info(vr.resolve("version = #{substring($S{java.version}, 0, 3)}"));

		// #{extract(s, regex[, group])} regex group extraction
		Logger.getLogger(SvlExample.class).info(vr.resolve("update = #{extract($S{java.version}, \"_([0-9]+)\", 1)}"));

		/*
		 *  See all supported variable types at,
		 *  http://juneau.apache.org/site/apidocs-8.0.0/overview-summary.html#juneau-marshall.SimpleVariableLanguage
		 *  NOTE - juneau-marshall.SimpleVariableLanguage supports nested variables well
		 * */
	}
}