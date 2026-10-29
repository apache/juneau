# ***************************************************************************************************************************
# * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file
# * distributed with this work for additional information regarding copyright ownership.  The ASF licenses this file
# * to you under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance
# * with the License.  You may obtain a copy of the License at
# *
# *  http://www.apache.org/licenses/LICENSE-2.0
# *
# * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an
# * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the License for the
# * specific language governing permissions and limitations under the License.
# ***************************************************************************************************************************
"""Nexus 2 staging REST over urllib.  Every POST goes through Runner.gate, so --dry-run never mutates."""
import base64
import json
import urllib.error
import urllib.request

from . import inventory

BASE = inventory.NEXUS_URL + "/service/local/staging"


class HttpError(RuntimeError):
    def __init__(self, method, url, status):
        super().__init__(f"{method} {url} -> {status}")
        self.status = status


def _default_opener(request):
    try:
        with urllib.request.urlopen(request, timeout=60) as r:   # noqa: S310 - fixed https endpoints
            return r.status, r.read()
    except urllib.error.HTTPError as e:
        return e.code, e.read()


class Nexus:
    def __init__(self, runner, auth, opener=_default_opener):
        self.runner, self.auth, self.opener = runner, auth, opener

    def _request(self, method, url, body=None):
        user, pw = self.auth()
        req = urllib.request.Request(url, method=method, data=json.dumps(body).encode() if body is not None else None)
        req.add_header("Accept", "application/json")
        req.add_header("Authorization", "Basic " + base64.b64encode(f"{user}:{pw}".encode()).decode())
        if body is not None:
            req.add_header("Content-Type", "application/json")
        return self.opener(req)

    def _get(self, url):
        status, body = self._request("GET", url)
        if status != 200:
            raise HttpError("GET", url, status)
        return json.loads(body or b"{}")

    def repositories(self):
        data = self._get(f"{BASE}/profile_repositories/{inventory.NEXUS_PROFILE}").get("data", [])
        return [{"repositoryId": r["repositoryId"], "state": r.get("type") or r.get("state"),
                 "created": r.get("created", ""), "transitioning": r.get("transitioning", False),
                 "description": r.get("description", "")} for r in data]

    def repo(self, rid):
        r = self._get(f"{BASE}/repository/{rid}")
        out = {"state": r.get("type") or r.get("state"), "transitioning": r.get("transitioning", False),
               "notifications": []}
        try:   # rule failures appear in the activity feed
            for act in self._get(f"{BASE}/repository/{rid}/activity"):
                for ev in act.get("events", []):
                    if "failed" in ev.get("name", "").lower():
                        out["notifications"] += [p.get("value", "") for p in ev.get("properties", [])
                                                 if p.get("name") == "failureMessage"]
        except Exception:   # noqa: BLE001 - activity is best effort
            pass
        return out

    def _action(self, action, rid, desc):
        if not self.runner.gate(f"POST {BASE}/bulk/{action} {rid}"):
            return
        status, _ = self._request("POST", f"{BASE}/bulk/{action}",
                                  {"data": {"stagedRepositoryIds": [rid], "description": desc}})
        if status not in (200, 201):
            raise HttpError("POST", f"{BASE}/bulk/{action}", status)

    def close(self, rid):
        self._action("close", rid, "Close for Apache Juneau release candidate")

    def drop(self, rid):
        self._action("drop", rid, "Drop")

    def promote(self, rid):
        self._action("promote", rid, "Release Apache Juneau")

    def walk(self, rid, path="org/apache/juneau/"):
        """{file name: url} of every leaf under path in the staging repo (Nexus JSON directory listing)."""
        # The REST content path; /content/repositories/... ignores Accept and returns an HTML index.
        base = f"{inventory.NEXUS_URL}/service/local/repositories/{rid}/content/{path}"
        found = {}
        for e in self._get(base).get("data", []):
            if e.get("leaf"):
                found[e["text"]] = e["resourceURI"]
            else:
                found.update(self.walk(rid, path + e["text"].rstrip("/") + "/"))
        return found

    def download(self, url, dest):
        status, body = self._request("GET", url)
        if status != 200:
            raise HttpError("GET", url, status)
        with open(dest, "wb") as f:
            f.write(body)
