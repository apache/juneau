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

/*
 * Attaches this page's CSRF token to every state-changing fetch.
 *
 * The server side of this is LoopbackBoundary, which refuses any non-GET request that does not present the
 * token. Rather than adding a header at each of the app's fetch() call sites -- where the next one added would
 * omit it, and would fail at runtime in whichever corner of the UI nobody clicked before shipping -- this wraps
 * window.fetch once, so carrying the token is the default and opting out is not expressible.
 *
 * Loaded first in base.ftlh's <head>, before any script that might issue a request.
 *
 * The token is read from the <meta> tag the server rendered. It is deliberately never written to
 * document.cookie: cookies are scoped by host and ignore the port, so a cookie-borne token could be planted by
 * any page served from any other port on this host. See SynchronizerToken's javadoc.
 */
(function () {
  'use strict';

  var meta = document.querySelector('meta[name="csrf-token"]');
  var token = meta && meta.getAttribute('content');
  if (!token) {
    // No token means the page was not served through ConsolePage, or the boundary filter is not installed.
    // Say so once, loudly: every write from this page is about to be refused, and a console message is far
    // easier to act on than a screenful of unexplained 403s.
    if (window.console && console.error) {
      console.error('No csrf-token meta tag; state-changing requests from this page will be refused.');
    }
    return;
  }

  // 'simple' request methods per fetch: these never need the token, and adding it to a cross-origin GET would
  // hand it to whatever was fetched. Anything else -- including a method this list does not know -- gets it.
  var SAFE = { GET: 1, HEAD: 1, OPTIONS: 1, TRACE: 1 };

  // Only same-origin requests get the token. A relative URL is same-origin by construction; an absolute one is
  // compared against location.origin. This keeps the secret from leaking to a third-party endpoint if some
  // future code fetches one.
  function isSameOrigin(url) {
    try {
      return new URL(url, window.location.href).origin === window.location.origin;
    } catch (e) {
      return false;
    }
  }

  var nativeFetch = window.fetch.bind(window);

  window.fetch = function (resource, init) {
    var opts = init || {};
    var method = (opts.method || (resource && resource.method) || 'GET').toUpperCase();
    var url = (resource && resource.url) || resource;

    if (SAFE[method] || !isSameOrigin(url)) {
      return nativeFetch(resource, init);
    }

    // Headers may arrive as a Headers instance, an array of pairs, or a plain object; normalizing through
    // Headers handles all three without caring which the caller used.
    var headers = new Headers((opts.headers) || (resource && resource.headers) || undefined);
    headers.set('X-Csrf-Token', token);

    // The boundary also requires a JSON content type on writes, which is what rules out the form-encoded
    // shapes a cross-origin <form> can submit with no preflight. It is required on every write, including a
    // bodiless one: a POST with no body and no Content-Type is itself a no-preflight shape, so the server
    // cannot exempt it, and the client must therefore supply the header even when it has nothing to send.
    if (!headers.has('Content-Type')) {
      headers.set('Content-Type', 'application/json');
    }

    var merged = {};
    for (var k in opts) {
      if (Object.prototype.hasOwnProperty.call(opts, k)) {
        merged[k] = opts[k];
      }
    }
    merged.headers = headers;
    return nativeFetch(resource, merged);
  };

  // Exposed for any code that must build a request by hand (e.g. XMLHttpRequest, or a fetch it deliberately
  // routes around the wrapper).
  window.RmCsrf = {
    token: token,
    header: 'X-Csrf-Token'
  };
})();
