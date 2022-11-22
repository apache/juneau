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
package org.apache.juneau.examples.core.dto.atom;

import org.apache.juneau.dto.atom.Feed;
import org.apache.juneau.html.HtmlSerializer;

/**
 * Atom feed HTML example.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * </ul>
 */
public class AtomHtmlExample {

	/**
	 * HTML Atom feed example.
	 *
	 * @param args Unused.
	 * @throws Exception Unused.
	 */
	public static void main(String[] args) throws Exception {


		Feed feed = AtomFeed.getAtomFeed();

		// Example with no namespaces
		// Create a serializer with readable output, no namespaces yet.
		HtmlSerializer s = HtmlSerializer.create().sq().ws().build();

		//Produces
		/**
		 * <table>
		 *  <tr>
		 *      <td>title</td>
		 *      <td>
		 *          <table>
		 *              <tr>
		 *                  <td>text</td>
		 *                  <td>Juneau ATOM specification</td>
		 *              </tr>
		 *          </table>
		 *      </td>
		 *  </tr>
		 *  <tr>
		 *      <td>updated</td>
		 *      <td>2016-01-02T03:04:05Z</td>
		 *  </tr>
		 *  <tr>
		 *      <td>links</td>
		 *      <td>
		 *          <table _type='array'>
		 *              <tr>
		 *                  <th>rel</th>
		 *                  <th>href</th>
		 *                  <th>hreflang</th>
		 *                  <th>type</th>
		 *              </tr>
		 *              <tr>
		 *                  <td>alternate</td>
		 *                  <td><a href='http://juneau.apache.org'>http://juneau.apache.org</a></td>
		 *                  <td>en</td>
		 *                  <td>text/html</td>
		 *              </tr>
		 *              <tr>
		 *                  <td>self</td>
		 *                  <td><a href='http://juneau.apache.org/feed.atom'>http://juneau.apache.org/feed.atom</a></td>
		 *                  <td><null/></td>
		 *                  <td>application/atom+xml</td>
		 *              </tr>
		 *          </table>
		 *      </td>
		 *  </tr>
		 *  <tr>
		 *      <td>id</td>
		 *      <td>
		 *          <table>
		 *              <tr>
		 *                  <td>text</td>
		 *                  <td>tag:juneau.apache.org</td>
		 *              </tr>
		 *          </table>
		 *      </td>
		 *  </tr>
		 *  <tr>
		 *      <td>subtitle</td>
		 *      <td>
		 *          <table>
		 *              <tr>
		 *                  <td>text</td>
		 *                  <td>Describes &lt;em&gt;stuff&lt;/em&gt; about Juneau</td>
		 *              </tr>
		 *              <tr>
		 *                  <td>type</td>
		 *                  <td>html</td>
		 *              </tr>
		 *          </table>
		 *      </td>
		 *  </tr>
		 *  <tr>
		 *      <td>generator</td>
		 *      <td>
		 *          <table>
		 *              <tr>
		 *                  <td>version</td>
		 *                  <td>1.0</td>
		 *              </tr>
		 *              <tr>
		 *                  <td>text</td>
		 *                  <td>Juneau</td>
		 *              </tr>
		 *              <tr>
		 *                  <td>uri</td>
		 *                  <td><a href='http://juneau.apache.org'>http://juneau.apache.org</a></td>
		 *              </tr>
		 *          </table>
		 *      </td>
		 *  </tr>
		 *  <tr>
		 *      <td>entries</td>
		 *      <td>
		 *          <table _type='array'>
		 *          <tr>
		 *              <th>title</th>
		 *              <th>updated</th>
		 *              <th>links</th>
		 *              <th>contributors</th>
		 *              <th>authors</th>
		 *              <th>id</th>
		 *              <th>content</th>
		 *          </tr>
		 *          <tr>
		 *              <td>
		 *                  <table>
		 *                          <tr>
		 *                              <td>text</td>
		 *                              <td>Juneau ATOM specification snapshot</td>
		 *                          </tr>
		 *                  </table>
		 *              </td>
		 *              <td>2016-01-02T03:04:05Z</td>
		 *              <td>
		 *                  <table _type='array'>
		 *                      <tr>
		 *                          <th>rel</th>
		 *                          <th>href</th>
		 *                          <th>type</th>
		 *                          <th>length</th>
		 *                      </tr>
		 *                      <tr>
		 *                          <td>alternate</td>
		 *                          <td><a href='http://juneau.apache.org/juneau.atom'>http://juneau.apache.org/juneau.atom</a></td>
		 *                          <td>text/html</td>
		 *                          <td><null/></td>
		 *                      </tr>
		 *                      <tr>
		 *                          <td>enclosure</td>
		 *                          <td><a href='http://juneau.apache.org/audio/juneau_podcast.mp3'>http://juneau.apache.org/audio/juneau_podcast.mp3</a></td>
		 *                          <td>audio/mpeg</td>
		 *                          <td>1337</td>
		 *                      </tr>
		 *                  </table>
		 *               </td>
		 *               <td>
		 *                   <table _type='array'>
		 *                      <tr>
		 *                          <th>uri</th>
		 *                          <th>email</th>
		 *                          <th>name</th>
		 *                      </tr>
		 *                      <tr>
		 *                          <td><a href='http://juneau.apache.org'>http://juneau.apache.org</a></td>
		 *                          <td>janesmith@apache.org</td>
		 *                          <td>Jane Smith</td>
		 *                      </tr>
		 *                   </table>
		 *                </td>
		 *                <td>
		 *                   <table _type='array'>
		 *                      <tr>
		 *                          <th>name</th>
		 *                      </tr>
		 *                      <tr>
		 *                          <td>John Smith</td>
		 *                      </tr>
		 *                   </table>
		 *                </td>
		 *                <td>
		 *                  <table>
		 *                      <tr>
		 *                          <td>text</td>
		 *                          <td>tag:juneau.sample.com,2013:1.2345</td>
		 *                      </tr>
		 *                  </table>
		 *                </td>
		 *                <td>2016-01-02T03:04:05Z</td>
		 *                <td>
		 *                  <table>
		 *                      <tr>
		 *                          <td>lang</td>
		 *                          <td>en</td>
		 *                      </tr>
		 *                      <tr>
		 *                          <td>base</td>
		 *                          <td><a href='http://www.apache.org/'>http://www.apache.org/</a></td>
		 *                      </tr>
		 *                      <tr>
		 *                          <td>text</td>
		 *                          <td>&lt;div&gt;&lt;p&gt;&lt;i&gt;[Update: Juneau supports ATOM.]&lt;/i&gt;&lt;/p&gt;&lt;/div&gt;</td>
		 *                      </tr>
		 *                      <tr>
		 *                          <td>type</td>
		 *                          <td>xhtml</td>
		 *                      </tr>
		 *                   </table>
		 *                </td>
		 *             </tr>
		 *          </table>
		 *       </td>
		 *     </tr>
		 *  </table>
		 */
		System.out.print(s.serialize(feed));
	}
}