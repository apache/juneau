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

import org.apache.juneau.svl.*;
import org.apache.juneau.utils.*;

/**
 * Manifest file entries variable resolver.
 *
 * <p>
 * The format for this var is <js>"$MF{key[,default]}"</js>.
 *
 * <p>
 * This variable resolver requires that a {@link ManifestFile} object be made available by calling
 * the {@link #init(ManifestFile)} method.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bcode w800'>
 * 	<jc>// Create a ManifestFile object that contains the manifest of the jar file containing this class.</jc>
 * 	ManifestFile mf = <jk>new</jk> ManifestFile(<jk>this</jk>.getClass());
 *
 * 	ManifestFileVar.<jsm>init</jsm>(mf);
 *
 * 	<jc>// Create a variable resolver that resolves manifest file entries (e.g. "$MF{Main-Class}")</jc>
 * 	VarResolver r = <jk>new</jk> VarResolver().addVars(ManifestFile.<jk>class</jk>);
 *
 * 	<jc>// Use it!</jc>
 * 	System.<jsf>out</jsf>.println(r.resolve(<js>"The main class is $MF{Main-Class}"</js>));
 * </p>
 *
 * <p>
 * Since this is a {@link SimpleVar}, any variables contained in the result will be recursively resolved.
 * Likewise, if the arguments contain any variables, those will be resolved before they are passed to this var.
 *
 * <ul class='seealso'>
 * 	<li class='link'>{@doc jm.SimpleVariableLanguage}
 * 	<li class='extlink'>{@source}
 * </ul>
 */
public class ManifestFileVar extends DefaultingVar {

	/** The name of this variable. */
	public static final String NAME = "MF";

	private static volatile ManifestFile MANIFEST_FILE;

	/**
	 * Initialize the manifest file for this variable.
	 *
	 * @param manifestFile The parsed manifest file.
	 */
	public static void init(ManifestFile manifestFile) {
		MANIFEST_FILE = manifestFile;
	}

	/**
	 * Constructor.
	 */
	public ManifestFileVar() {
		super(NAME);
	}

	@Override /* Var */
	public String resolve(VarResolverSession session, String key) {
		return MANIFEST_FILE == null ? "" : MANIFEST_FILE.getString(key);
	}
}
