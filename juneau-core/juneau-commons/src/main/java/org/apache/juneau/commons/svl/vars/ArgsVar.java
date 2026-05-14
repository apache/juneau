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

import java.util.concurrent.atomic.*;
import java.util.function.*;

import org.apache.juneau.commons.runtime.*;
import org.apache.juneau.commons.settings.*;
import org.apache.juneau.commons.svl.*;

/**
 * JVM args variable resolver.
 *
 * <p>
 * The format for this var is <js>"$A{arg[,default]}"</js>.
 *
 * <p>
 * The argument can be either a positional index (e.g. <js>"$A{0}"</js>) or a named option (e.g. <js>"$A{port}"</js>).
 *
 * <p>
 * This variable resolver requires that the command-line arguments be made available through any of the following:
 * <ul class='spaced-list'>
 * 	<li><js>"sun.java.command"</js> system property.
 * 	<li><js>"juneau.args"</js> system property.
 * 	<li>{@link #init(Args)} has been called.
 * 	<li>The instance was created via {@link #create(Supplier)}.
 * </ul>
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	<jc>// Create an args object from the main(String[]) method.</jc>
 * 	Args <jv>args</jv> = <jk>new</jk> Args(<jv>argv</jv>);
 *
 * 	ArgsVar.<jsm>init</jsm>(<jv>args</jv>);
 *
 * 	<jc>// Create a variable resolver that resolves JVM arguments (e.g. "$A{1}")</jc>
 * 	VarResolver <jv>varResolver</jv> = VarResolver.<jsm>create</jsm>().vars(ArgsVar.<jk>class</jk>).build();
 *
 * 	<jc>// Use it!</jc>
 * 	System.<jsf>out</jsf>.println(<jv>varResolver</jv>.resolve(<js>"Arg #1 is set to $A{1}"</js>));
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
public class ArgsVar extends DefaultingVar {

	/** The name of this variable. */
	public static final String NAME = "A";

	private static final AtomicReference<Supplier<Args>> STATIC_ARGS_SUPPLIER = new AtomicReference<>(ArgsPropertySource::createDefaultArgs);

	/**
	 * Initialize the args for this variable.
	 *
	 * <p>
	 * This sets a process-wide reference that all newly-constructed {@link ArgsVar} instances will read from.  For
	 * isolated, per-resolver state without mutating global state, use {@link #create(Supplier)} instead.
	 *
	 * @param args The parsed command-line arguments.
	 */
	public static void init(Args args) {
		STATIC_ARGS_SUPPLIER.set(() -> args);
	}

	/**
	 * Creates an {@link ArgsVar} bound to a per-instance {@link Supplier} of {@link Args}.
	 *
	 * <p>
	 * Use this when wiring an {@link ArgsVar} into a single {@code VarResolver} without mutating the process-wide
	 * state established by {@link #init(Args)}.  The supplier is invoked on every resolve, so callers can update the
	 * underlying {@link Args} between resolves if needed.
	 *
	 * @param supplier The supplier of {@link Args} for this var instance.  Must not be <jk>null</jk>.
	 * @return A new {@link ArgsVar} instance backed by the supplier.
	 */
	public static ArgsVar create(Supplier<Args> supplier) {
		return new ArgsVar(supplier);
	}

	private final ArgsPropertySource source;

	/**
	 * Constructor.
	 */
	public ArgsVar() {
		super(NAME);
		this.source = new ArgsPropertySource(() -> STATIC_ARGS_SUPPLIER.get().get());
	}

	private ArgsVar(Supplier<Args> supplier) {
		super(NAME);
		this.source = new ArgsPropertySource(supplier);
	}

	@Override /* Overridden from Var */
	public String resolve(VarResolverSession session, String key) {
		var result = source.get(key);
		return result.isPresent() ? result.value().orElse(null) : null;
	}
}
