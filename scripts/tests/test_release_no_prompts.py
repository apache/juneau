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
"""Guard: release.py and jrm_release never touch the prompt channel (promptless releases pause instead of asking)."""
import ast
from pathlib import Path

SCRIPTS = Path(__file__).resolve().parent.parent
# askpass.py is out of scope: it is the GIT_ASKPASS helper that answers *git's* Username/Password prompt from the
# environment (`prompt = sys.argv[1]`), not our prompt channel.  Never rename it to satisfy this guard.
EXCLUDED = {"askpass.py"}
FILES = [SCRIPTS / "release.py", *sorted(f for f in (SCRIPTS / "jrm_release").glob("*.py") if f.name not in EXCLUDED)]


def violations(source, name="<src>"):
    """Where the source touches the prompt channel: a `prompt` name/attribute/argument/keyword/import, or a `.review(` call."""
    found = []
    for node in ast.walk(ast.parse(source, filename=name)):
        line = getattr(node, "lineno", 0)
        if isinstance(node, ast.Attribute) and node.attr in ("prompt", "review"):
            found.append((line, f"attribute .{node.attr}"))
        elif isinstance(node, ast.Name) and node.id == "prompt":
            found.append((line, "name prompt"))
        elif isinstance(node, ast.arg) and node.arg == "prompt":
            found.append((line, "argument prompt"))
        elif isinstance(node, ast.keyword) and node.arg == "prompt":
            found.append((line, "keyword prompt="))
        elif isinstance(node, (ast.Import, ast.ImportFrom)) and any(
                a.name.split(".")[-1] == "prompt" or a.asname == "prompt" for a in node.names):
            found.append((line, "import prompt"))
    return found


def test_checker_flags_each_form():
    for src in ("ctx.prompt.confirm('x')", "deps.prompt", "ctx.review('q')", "from a import prompt", "import a.prompt",
                "def f(prompt): pass", "f(prompt=1)", "prompt = 1"):
        assert violations(src), src


def test_checker_ignores_words_in_comments_and_strings():
    assert violations("# prompt\nx = 'ask a prompt'\n\"\"\"the prompt channel\"\"\"\n") == []


def test_askpass_is_the_only_exclusion_and_still_exists():
    assert EXCLUDED == {"askpass.py"} and (SCRIPTS / "jrm_release" / "askpass.py").exists()


def test_no_release_script_uses_the_prompt_channel():
    bad = {f.name: v for f in FILES if (v := violations(f.read_text(), str(f)))}
    assert bad == {}, bad
