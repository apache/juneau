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

import static org.apache.juneau.dto.atom.AtomBuilder.*;
import static org.apache.juneau.dto.atom.AtomBuilder.content;
import static org.apache.juneau.dto.atom.AtomBuilder.person;

public class AtomFeed {

    public static Feed GetAtomFeed(){

        Feed feed =
                feed("tag:juneau.apache.org", "Juneau ATOM specification", "2016-01-02T03:04:05Z")
                        .subtitle(text("html").text("Describes <em>stuff</em> about Juneau"))
                        .links(
                                link("alternate", "text/html", "http://juneau.apache.org").hreflang("en"),
                                link("self", "application/atom+xml", "http://juneau.apache.org/feed.atom")
                        )
                        .generator(
                                generator("Juneau").uri("http://juneau.apache.org").version("1.0")
                        )
                        .entries(
                                entry("tag:juneau.sample.com,2013:1.2345", "Juneau ATOM specification snapshot",
                                        "2016-01-02T03:04:05Z")
                                        .links(
                                                link("alternate", "text/html",
                                                        "http://juneau.apache.org/juneau.atom"),
                                                link("enclosure", "audio/mpeg",
                                                        "http://juneau.apache.org/audio/juneau_podcast.mp3").
                                                        length(1337)
                                        )
                                        .published("2016-01-02T03:04:05Z")
                                        .authors(
                                                person("Jane Smith").
                                                        uri("http://juneau.apache.org").
                                                        email("janesmith@apache.org")
                                        )
                                        .contributors(
                                                person("John Smith")
                                        )
                                        .content(
                                                content("xhtml")
                                                        .lang("en")
                                                        .base("http://www.apache.org/")
                                                        .text("<div><p><i>[Update: Juneau supports ATOM.]</i></p></div>")
                                        )
                        );

        return feed;

    }
}
