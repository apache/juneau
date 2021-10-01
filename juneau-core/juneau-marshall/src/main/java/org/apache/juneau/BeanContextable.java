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
package org.apache.juneau;

import static java.util.Optional.*;

import org.apache.juneau.collections.*;

/**
 * Context class for classes that use {@link BeanContext} objects.
 */
public abstract class BeanContextable extends Context {

	final BeanContext beanContext;

	/**
	 * Constructor.
	 *
	 * @param b The builder for this object.
	 */
	protected BeanContextable(BeanContextableBuilder b) {
		super(b);
		beanContext = ofNullable(b.bc).orElse(b.bcBuilder.build());
	}

	@Override
	public abstract BeanContextableBuilder copy();

	/**
	 * Returns the bean context for this object.
	 *
	 * @return The bean context for this object.
	 */
	public BeanContext getBeanContext() {
		return beanContext;
	}

	@Override
	public BeanSession createSession() {
		return beanContext.createBeanSession(beanContext.createDefaultSessionArgs());
	}

	/**
	 * Create a new bean session based on the properties defined on this context combined with the specified
	 * runtime args.
	 *
	 * <p>
	 * Use this method for creating sessions if you don't need to override any
	 * properties or locale/timezone currently set on this context.
	 *
	 * @param args
	 * 	The session arguments.
	 * @return A new session object.
	 */
	public BeanSession createSession(BeanSessionArgs args) {
		return beanContext.createBeanSession(args);
	}

	@Override /* Context */
	public final Session createSession(Context.Args args) {
		throw new NoSuchMethodError();
	}

	@Override /* Context */
	public BeanSessionArgs createDefaultSessionArgs() {
 		return beanContext.createDefaultSessionArgs();
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Other methods
	//-----------------------------------------------------------------------------------------------------------------

	@Override /* Context */
	public OMap toMap() {
		return super.toMap()
			.a(
				"BeanContextable",
				OMap.of("beanContext", beanContext.toMap())
			);
	}
}
