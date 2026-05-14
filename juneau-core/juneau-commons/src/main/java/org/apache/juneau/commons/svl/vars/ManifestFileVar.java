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
package org.apache.juneau.commons.svl.vars;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.*;

import org.apache.juneau.commons.runtime.*;
import org.apache.juneau.commons.svl.*;

/**
 * Manifest file entries variable resolver.
 *
 * <p>
 * The format for this var is <js>"$MF{key[,default]}"</js>.
 *
 * <p>
 * This variable resolver requires that a {@link ManifestFile} object be made available by either:
 * <ul class='spaced-list'>
 * 	<li>Calling {@link #init(ManifestFile)} (process-wide).
 * 	<li>Constructing the var via {@link #create(Supplier)} (per-instance).
 * </ul>
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	<jc>// Create a ManifestFile object that contains the manifest of the jar file containing this class.</jc>
 * 	ManifestFile <jv>manifestFile</jv> = <jk>new</jk> ManifestFile(<jk>this</jk>.getClass());
 *
 * 	ManifestFileVar.<jsm>init</jsm>(<jv>manifestFile</jv>);
 *
 * 	<jc>// Create a variable resolver that resolves manifest file entries (e.g. "$MF{Main-Class}")</jc>
 * 	VarResolver <jv>varResolver</jv> = VarResolver.<jsm>create</jsm>().vars(ManifestFileVar.<jk>class</jk>).build();
 *
 * 	<jc>// Use it!</jc>
 * 	System.<jsf>out</jsf>.println(<jv>varResolver</jv>.resolve(<js>"The main class is $MF{Main-Class}"</js>));
 * </p>
 *
 * <p>
 * Since this is a {@link SimpleVar}, any variables contained in the result will be recursively resolved.
 * Likewise, if the arguments contain any variables, those will be resolved before they are passed to this var.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/SimpleVariableLanguageBasics">Simple Variable Language Basics</a>
 * </ul>
 */
public class ManifestFileVar extends DefaultingVar {

	/** The name of this variable. */
	public static final String NAME = "MF";

	private static final AtomicReference<ManifestFile> manifestFile = new AtomicReference<>();

	/**
	 * Initialize the manifest file for this variable.
	 *
	 * <p>
	 * This sets a process-wide reference that all newly-constructed {@link ManifestFileVar} instances will read from.
	 * For isolated, per-resolver state without mutating global state, use {@link #create(Supplier)} instead.
	 *
	 * @param manifestFile The parsed manifest file.
	 */
	public static void init(ManifestFile manifestFile) {
		ManifestFileVar.manifestFile.set(manifestFile);
	}

	/**
	 * Creates a {@link ManifestFileVar} bound to a per-instance {@link Supplier} of {@link ManifestFile}.
	 *
	 * <p>
	 * Use this when wiring a {@link ManifestFileVar} into a single {@code VarResolver} without mutating the
	 * process-wide state established by {@link #init(ManifestFile)}.  The supplier is invoked on every resolve.
	 *
	 * @param supplier The supplier of {@link ManifestFile} for this var instance.  Must not be <jk>null</jk>.
	 * @return A new {@link ManifestFileVar} instance backed by the supplier.
	 */
	public static ManifestFileVar create(Supplier<ManifestFile> supplier) {
		return new ManifestFileVar(supplier);
	}

	private final Supplier<ManifestFile> manifestSupplier;

	/**
	 * Constructor.
	 */
	public ManifestFileVar() {
		super(NAME);
		this.manifestSupplier = manifestFile::get;
	}

	private ManifestFileVar(Supplier<ManifestFile> supplier) {
		super(NAME);
		this.manifestSupplier = supplier;
	}

	@Override /* Overridden from Var */
	public String resolve(VarResolverSession session, String key) {
		var mf = manifestSupplier.get();
		return mf == null ? "" : mf.get(key).orElse(null);
	}
}
