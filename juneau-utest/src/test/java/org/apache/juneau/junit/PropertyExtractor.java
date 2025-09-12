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
package org.apache.juneau.junit;

/**
 * Interface for custom property extraction strategies in the Bean-Centric Testing framework.
 *
 * <p>Property extractors define how the converter accesses object properties during nested
 * field navigation (e.g., {@code "user.address.city"}). The framework uses a chain-of-responsibility
 * pattern, trying each registered extractor until one can handle the property access.</p>
 *
 * <h5 class='section'>Extraction Strategy:</h5>
 * <p>The two-phase approach ensures efficient and flexible property access:</p>
 * <ol>
 *    <li><b>{@link #canExtract(BeanConverter, Object, String)}:</b> Quick check if this extractor can handle the property</li>
 *    <li><b>{@link #extract(BeanConverter, Object, String)}:</b> Perform the actual property extraction</li>
 * </ol>
 *
 * <h5 class='section'>Common Use Cases:</h5>
 * <ul>
 *    <li><b>JavaBean properties:</b> Standard getter methods and public fields</li>
 *    <li><b>Map-style access:</b> Key-based property retrieval from Map objects</li>
 *    <li><b>Collection indices:</b> Numeric access for arrays and lists</li>
 *    <li><b>Custom data structures:</b> Domain-specific property access patterns</li>
 *    <li><b>Dynamic properties:</b> Computed or cached property values</li>
 * </ul>
 *
 * <h5 class='section'>Implementation Example:</h5>
 * <p class='bjava'>
 *    <jc>// Custom extractor for database entities</jc>
 *    <jk>public class</jk> DatabaseEntityExtractor <jk>implements</jk> PropertyExtractor {
 *
 *       <ja>@Override</ja>
 *       <jk>public boolean</jk> canExtract(BeanConverter <jv>converter</jv>, Object <jv>obj</jv>, String <jv>property</jv>) {
 *          <jk>return</jk> <jv>obj</jv> <jk>instanceof</jk> DatabaseEntity;
 *       }
 *
 *       <ja>@Override</ja>
 *       <jk>public</jk> Object extract(BeanConverter <jv>converter</jv>, Object <jv>obj</jv>, String <jv>property</jv>) {
 *          DatabaseEntity <jv>entity</jv> = (DatabaseEntity) <jv>obj</jv>;
 *          <jk>switch</jk> (<jv>property</jv>) {
 *             <jk>case</jk> <js>"id"</js>: <jk>return</jk> <jv>entity</jv>.getPrimaryKey();
 *             <jk>case</jk> <js>"lastModified"</js>: <jk>return</jk> <jv>entity</jv>.getTimestamp();
 *             <jk>case</jk> <js>"metadata"</js>: <jk>return</jk> <jv>entity</jv>.getMetadata().asMap();
 *             <jk>default</jk>: <jk>return</jk> <jv>entity</jv>.getAttribute(<jv>property</jv>);
 *          }
 *       }
 *    }
 * </p>
 *
 * <h5 class='section'>Registration and Usage:</h5>
 * <p class='bjava'>
 *    <jk>var</jk> <jv>converter</jv> = BasicBeanConverter.<jsm>builder</jsm>()
 *       .defaultSettings() <jc>// Adds standard extractors</jc>
 *       .addPropertyExtractor(<jk>new</jk> DatabaseEntityExtractor())
 *       .addPropertyExtractor((<jp>conv</jp>, <jp>obj</jp>, <jp>prop</jp>) -&gt; {
 *          <jc>// Lambda-based extractor for simple cases</jc>
 *          <jk>if</jk> (<jp>obj</jp> <jk>instanceof</jk> MyConfig <jv>config</jv> &amp;&amp; <js>"timeout"</js>.equals(<jp>prop</jp>)) {
 *             <jk>return</jk> <jv>config</jv>.getTimeoutMillis();
 *          }
 *          <jk>return</jk> <jk>null</jk>; <jc>// Let next extractor try</jc>
 *       })
 *       .build();
 * </p>
 *
 * <h5 class='section'>Best Practices:</h5>
 * <ul>
 *    <li><b>Fast canExtract() checks:</b> Use efficient type checking and avoid expensive operations</li>
 *    <li><b>Handle edge cases:</b> Gracefully handle null objects and invalid property names</li>
 *    <li><b>Consider caching:</b> Cache reflection results for better performance</li>
 * </ul>
 *
 * @see PropertyExtractors
 * @see BasicBeanConverter.Builder#addPropertyExtractor(PropertyExtractor)
 * @see BeanConverter#getProperty(Object, String)
 */
public interface PropertyExtractor {

	/**
	 * Determines if this extractor can handle property access for the given object and property name.
	 *
	 * <p>This method should perform a quick check to determine compatibility without doing
	 * expensive operations. It's called frequently during property navigation.</p>
	 *
	 * @param converter The bean converter instance (for recursive operations if needed)
	 * @param o The object to extract the property from
	 * @param key The property name to extract
	 * @return {@code true} if this extractor can handle the property access, {@code false} otherwise
	 */
	boolean canExtract(BeanConverter converter, Object o, String key);

	/**
	 * Extracts the specified property value from the given object.
	 *
	 * <p>This method is only called after {@link #canExtract(BeanConverter, Object, String)}
	 * returns {@code true}. It should perform the actual property extraction and return
	 * the property value.</p>
	 *
	 * @param converter The bean converter instance (for recursive operations if needed)
	 * @param o The object to extract the property from
	 * @param key The property name to extract
	 * @return The property value
	 * @throws PropertyNotFoundException if the property cannot be found on the object
	 */
	Object extract(BeanConverter converter, Object o, String key);
}
