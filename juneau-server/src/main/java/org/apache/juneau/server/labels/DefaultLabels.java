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
package org.apache.juneau.server.labels;

import static java.lang.String.*;

import java.util.*;

import org.apache.juneau.server.converters.*;

/**
 * Reusable label constructs for REST OPTIONS requests.
 *
 * @author James Bognar (james.bognar@salesforce.com)
 */
public final class DefaultLabels {

	private static String
		bundleName = DefaultLabels.class.getPackage().getName() + ".nls.DefaultLabels",
		typeKey = "%s.%s.type",	// "{category}.{name}.type"
		descKey = "%s.%s.desc";	// "{catetory}.{name}.desc"

	private static Map<Locale,ParamDescription[]> queryableParams = new HashMap<Locale,ParamDescription[]>();
	private static Map<Locale,ParamDescription[]> headerParams = new HashMap<Locale,ParamDescription[]>();

	/**
	 * OPTIONS request labels for query/view/sort/paging features on POJOs
	 * 	when {@link Queryable} filter is associated with a resource method.
	 * @param locale The client locale.
	 * @return A list of localized parameter descriptions.
	 */
	public static ParamDescription[] getQueryableParamDescriptions(Locale locale) {
		if (! queryableParams.containsKey(locale)) {
			ResourceBundle rb = ResourceBundle.getBundle(bundleName, locale);
			String category = "QueryableParam";
			String[] p = {"q","v","s","g","i","p","l"};
			ParamDescription[] l = new ParamDescription[p.length];
			for (int i = 0; i < p.length; i++)
				l[i] = new ParamDescription(p[i],
					rb.getString(format(typeKey, category, p[i])),
					rb.getString(format(descKey, category, p[i]))
				);
			queryableParams.put(locale, l);
		}
		return queryableParams.get(locale);
	}

	/**
	 * OPTIONS request labels for header values that can be specified as GET parameters.
	 * @param locale The client locale.
	 * @return A list of localized parameter descriptions.
	 */
	public static ParamDescription[] getHeaderParamDescriptions(Locale locale) {
		if (! headerParams.containsKey(locale)) {
			ResourceBundle rb = ResourceBundle.getBundle(bundleName, locale);
			String category = "HeaderParam";
			String[] p = {"Accept","Accept-Encoding","Method","Content","plainText"};
			ParamDescription[] l = new ParamDescription[p.length];
			for (int i = 0; i < p.length; i++)
				l[i] = new ParamDescription(p[i],
					rb.getString(format(typeKey, category, p[i])),
					rb.getString(format(descKey, category, p[i]))
				);
			headerParams.put(locale, l);
		}
		return headerParams.get(locale);
	}
}
