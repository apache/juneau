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
 * The format for this var is <js>"$MF{key}"</js> or <js>"$MF{key,defaultValue}"</js>
 *
 * <p>
 * This variable resolver requires that a {@link ManifestFile} object be set as a context object on the resolver or a
 * session object on the resolver session.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bcode'>
 * 	<jc>// Create a ManifestFile object that contains the manifest of the jar file containing this class.</jc>
 * 	ManifestFile mf = <jk>new</jk> ManifestFile(<jk>this</jk>.getClass());
 *
 * 	<jc>// Create a variable resolver that resolves manifest file entries (e.g. "$MF{Main-Class}")</jc>
 * 	VarResolver r = <jk>new</jk> VarResolver().addVars(ManifestFile.<js>class</js>)
 * 		.addContextObject(<jsf>SESSION_manifest</jsf>, mf);
 *
 * 	<jc>// Use it!</jc>
 * 	System.<jsf>out</jsf>.println(r.resolve(<js>"The main class is $MF{Main-Class}"</js>));
 * </p>
 *
 * <p>
 * Since this is a {@link SimpleVar}, any variables contained in the result will be recursively resolved.
 * Likewise, if the arguments contain any variables, those will be resolved before they are passed to this var.
 *
 * @see org.apache.juneau.utils.ManifestFile
 * @see org.apache.juneau.svl
 */
public class ManifestFileVar extends DefaultingVar {

	/**
	 * The name of the session or context object that identifies the {@link ManifestFile} object.
	 */
	public static final String SESSION_manifest = "manifest";

	/** The name of this variable. */
	public static final String NAME = "MF";

	/**
	 * Constructor.
	 */
	public ManifestFileVar() {
		super(NAME);
	}

	@Override /* Var */
	public String resolve(VarResolverSession session, String key) {
		return session.getSessionObject(ManifestFile.class, SESSION_manifest).getString(key);
	}
}
