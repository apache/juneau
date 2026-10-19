import os

import pytest

from jrm_release import details, rc
from jrm_release.ctx import StepError

pytestmark = pytest.mark.usefixtures("clean_env")

POM = "<project><parent><version>1</version></parent><version>9.2.1-SNAPSHOT</version></project>"


def test_details_branch_first_then_pom_defaults(make_ctx):
    ctx = make_ctx(with_details=False, answers={
        "release.branch": "juneau-9.2.1-branch",
        "release.details": {"version": "9.2.1", "developmentVersion": "9.2.2-SNAPSHOT", "rc": 1, "summary": "Fixes."}})
    ctx.fake.script("git", "-C", details.checkout(), "show", out=POM)
    d = details.ask(ctx)
    assert [a[0] for a in ctx.prompt.asked] == ["text", "form"]
    form = {f.name: f.default for f in ctx.prompt.asked[1][3]["fields"]}
    assert form["version"] == "9.2.1" and form["developmentVersion"] == "9.2.2-SNAPSHOT"
    assert ["git", "-C", details.checkout(), "show", "origin/juneau-9.2.1-branch:pom.xml"] in ctx.fake.argvs()
    assert d["branch"] == "juneau-9.2.1-branch" and ctx.rf.get("details")["rc"] == 1


def test_details_validation_rejects_bad_versions():
    ok = dict(version="9.2.1", developmentVersion="9.2.2-SNAPSHOT", rc=1)
    assert details.validate(ok) is None
    assert "x.y.z" in details.validate({**ok, "version": "9.2"})
    assert "SNAPSHOT" in details.validate({**ok, "developmentVersion": "9.2.2"})
    assert "greater" in details.validate({**ok, "developmentVersion": "9.2.0-SNAPSHOT"})
    assert "rc" in details.validate({**ok, "rc": 0})


def test_details_reasks_after_invalid(make_ctx):
    bad = {"version": "9.2", "developmentVersion": "9.2.2-SNAPSHOT", "rc": 1, "summary": "s"}
    good = {**bad, "version": "9.2.1"}
    ctx = make_ctx(with_details=False, answers={"release.branch": "b", "release.details": [bad, good]})
    assert details.ask(ctx)["version"] == "9.2.1"
    assert sum(1 for a in ctx.prompt.asked if a[0] == "form") == 2


def test_workspace_argv(make_ctx):
    ctx = make_ctx()
    rc.workspace(ctx)
    ws = str(ctx.ws)
    assert ctx.fake.argvs()[0] == ["git", "clone", "--branch", "juneau-9.2.1-branch",
                                   "https://gitbox.apache.org/repos/asf/juneau.git", ws]
    assert ["git", "-C", ws, "fetch", "--tags", "origin"] in ctx.fake.argvs()
    assert ["git", "-C", ws, "config", "user.email", "jamesbognar@apache.org"] in ctx.fake.argvs()


def test_build_verify_and_javadoc_argv(make_ctx):
    ctx = make_ctx(answers={"review.javadoc": True})
    rc.build_verify(ctx)
    rc.javadoc_verify(ctx)
    assert ["mvn", "-B", "clean", "verify"] in ctx.fake.argvs()
    assert ["mvn", "-B", "javadoc:aggregate"] in ctx.fake.argvs()
    assert all(c["cwd"] == ctx.ws for c in ctx.fake.calls if c["argv"][0] == "mvn")


def test_javadoc_not_accepted_fails(make_ctx):
    ctx = make_ctx(answers={"review.javadoc": False})
    with pytest.raises(StepError, match="javadoc"):
        rc.javadoc_verify(ctx)


def test_workspace_zips_skip_absent_modules(make_ctx):
    ctx = make_ctx(answers={"review.workspaces": True})
    zdir = ctx.ws / "juneau-examples" / "juneau-examples-core" / "target"
    zdir.mkdir(parents=True)
    (zdir / "juneau-examples-core-9.2.1-SNAPSHOT-bin.zip").write_bytes(b"z")
    out = rc.FUNCS["test-workspace-verify"](ctx)
    assert out == "1 workspaces checked"
    assert len(ctx.fake.ran("unzip")) == 1
    assert any("skip juneau-examples-mcp" in s for s in ctx.fake.said)


def test_workspace_zips_fail_when_none_exist(make_ctx):
    ctx = make_ctx(answers={"review.workspaces": True})
    with pytest.raises(StepError, match="no workspace zips"):
        rc.FUNCS["test-workspace-verify"](ctx)


def test_gpg_unlock_passphrase_on_stdin_only(make_ctx):
    ctx = make_ctx()
    rc.gpg_unlock(ctx)
    call = ctx.fake.calls[0]
    assert call["argv"][:2] == ["gpg", "--batch"] and "--passphrase-fd" in call["argv"]
    assert call["input"] == b"s3cr3t-gpg\n"
    assert "s3cr3t-gpg" not in " ".join(call["argv"])


def test_deploy_snapshot_argv(make_ctx):
    ctx = make_ctx()
    rc.deploy_snapshot(ctx)
    assert ctx.fake.argvs()[-1] == ["mvn", "-B", "deploy", "-Daether.checksums.algorithms=MD5,SHA-1,SHA-512"]


def _write_pom(ctx, version):
    ctx.ws.mkdir(parents=True, exist_ok=True)
    (ctx.ws / "pom.xml").write_text(f"<project><version>{version}</version></project>")


def test_release_prepare_refuses_non_snapshot(make_ctx):
    ctx = make_ctx()
    _write_pom(ctx, "9.2.1")
    with pytest.raises(StepError, match="SNAPSHOT"):
        rc.release_prepare(ctx)
    assert not ctx.fake.ran("mvn")


def test_release_prepare_refuses_existing_tag(make_ctx):
    ctx = make_ctx()
    _write_pom(ctx, "9.2.1-SNAPSHOT")
    ctx.fake.script("git", "-C", str(ctx.ws), "tag", "--list", out="juneau-9.2.1-RC1\n")
    with pytest.raises(StepError, match="juneau-9.2.1-RC1"):
        rc.release_prepare(ctx)
    assert not ctx.fake.ran("mvn")


def test_release_prepare_argv_and_askpass_env(make_ctx):
    ctx = make_ctx()
    _write_pom(ctx, "9.2.1-SNAPSHOT")
    rc.release_prepare(ctx)
    call = ctx.fake.calls[-1]
    assert call["argv"] == ["mvn", "-B", "release:prepare", "-DautoVersionSubmodules=true",
                            "-DreleaseVersion=9.2.1", "-Dtag=juneau-9.2.1-RC1",
                            "-DdevelopmentVersion=9.2.2-SNAPSHOT"]
    assert call["env"]["GIT_ASKPASS"].endswith("jrm_release/askpass.py")
    assert call["env"]["GIT_TERMINAL_PROMPT"] == "0"
    assert call["env"]["JRM_ASKPASS_PASS"] == "s3cr3t-pw" and "s3cr3t-pw" not in " ".join(call["argv"])


def test_release_diff_review_needs_acceptance(make_ctx):
    ctx = make_ctx(answers={"review.diff": False})
    with pytest.raises(StepError, match="diff"):
        rc.release_diff_review(ctx)
    assert ["git", "-C", str(ctx.ws), "diff", "juneau-9.2.1-RC1^", "juneau-9.2.1-RC1"] in ctx.fake.argvs()


def test_release_perform_offers_stale_repo_drop(make_ctx):
    from conftest import FakeNexus
    nx = FakeNexus()
    nx.repos = [{"repositoryId": "orgapachejuneau-1001", "state": "open", "created": "2026-10-01"}]
    nx.repos_after = [{"repositoryId": "orgapachejuneau-1002", "state": "open", "created": "2026-10-19"}]
    ctx = make_ctx(nexus=nx, answers={"confirm.drop-stale": True})
    rc.release_perform(ctx)
    assert ("drop", "orgapachejuneau-1001") in nx.posts
    assert ctx.rf.get("nexusRepoId") == "orgapachejuneau-1002"


def test_nexus_close_polls_until_closed(make_ctx):
    from conftest import FakeNexus
    nx = FakeNexus()
    nx.states["r-1"] = [{"state": "open", "transitioning": True}, {"state": "open", "transitioning": True},
                        {"state": "closed", "transitioning": False}]
    ctx = make_ctx(nexus=nx, state={"nexusRepoId": "r-1"})
    rc.nexus_close(ctx)
    assert nx.posts == [("close", "r-1")]


def test_nexus_close_fails_on_rule_notifications(make_ctx):
    from conftest import FakeNexus
    nx = FakeNexus()
    nx.states["r-1"] = [{"state": "open", "transitioning": False, "notifications": ["Missing signature: x.jar"]}]
    ctx = make_ctx(nexus=nx, state={"nexusRepoId": "r-1"})
    with pytest.raises(StepError, match="Missing signature"):
        rc.nexus_close(ctx)


WANTED = ["juneau-9.2.1-source-release.zip", "juneau-9.2.1-source-release.zip.asc",
          "juneau-9.2.1-source-release.zip.sha512", "juneau-distrib-9.2.1-bin.zip",
          "juneau-distrib-9.2.1-bin.zip.asc", "juneau-distrib-9.2.1-bin.zip.sha512"]


def _staged(ctx, extra=(), missing=()):
    ctx.nexus.files = {n: f"https://repository.apache.org/content/repositories/r-1/org/apache/juneau/x/{n}"
                       for n in [*WANTED, *extra] if n not in missing}


def test_dist_stage_lists_entries_no_glob(make_ctx):
    ctx = make_ctx(state={"nexusRepoId": "r-1"})
    _staged(ctx, extra=["juneau-9.2.1-source-release.zip.sha1", "juneau-9.2.1-source-release.zip.md5",
                        "juneau-core-9.2.1.jar"])
    ctx.fake.script("svn", "ls", out="juneau-9.2.0-RC1/\njuneau-9.1.0-RC2/\n.hidden/\n")
    rc.dist_stage(ctx)
    assert all("*" not in " ".join(a) for a in ctx.fake.argvs())
    mucc = ctx.fake.ran("svnmucc")[0]
    flat = " ".join(mucc)
    assert "rm https://dist.apache.org/repos/dist/dev/juneau/source/juneau-9.2.0-RC1" in flat
    assert "juneau-9.1.0-RC2" not in flat and ".hidden" not in flat      # other lines are left alone


def test_dist_stage_renames_and_drops_sha1_md5(make_ctx):
    ctx = make_ctx(state={"nexusRepoId": "r-1"})
    _staged(ctx, extra=["juneau-9.2.1-source-release.zip.sha1", "juneau-9.2.1-source-release.zip.md5"])
    rc.dist_stage(ctx)
    local = sorted(p.name for p in (ctx.run_dir / "dist").iterdir())
    assert local == ["apache-juneau-9.2.1-bin.zip", "apache-juneau-9.2.1-bin.zip.asc",
                     "apache-juneau-9.2.1-bin.zip.sha512", "apache-juneau-9.2.1-src.zip",
                     "apache-juneau-9.2.1-src.zip.asc", "apache-juneau-9.2.1-src.zip.sha512"]
    mucc = ctx.fake.ran("svnmucc")[0]
    assert mucc.count("put") == 6 and mucc.count("mkdir") == 2
    assert "--password-from-stdin" in mucc and "s3cr3t-pw" not in " ".join(mucc)
    call = next(c for c in ctx.fake.calls if c["argv"][0] == "svnmucc")
    assert call["input"] == b"s3cr3t-pw\n"


def test_dist_stage_refuses_missing_file(make_ctx):
    ctx = make_ctx(state={"nexusRepoId": "r-1"})
    _staged(ctx, missing=["juneau-distrib-9.2.1-bin.zip.asc"])
    with pytest.raises(StepError, match="juneau-distrib-9.2.1-bin.zip.asc"):
        rc.dist_stage(ctx)
    assert not ctx.fake.ran("svnmucc")


def _local_dist(ctx, digest_ok=True):
    import hashlib
    out = ctx.run_dir / "dist"
    out.mkdir(parents=True, exist_ok=True)
    for name in ("apache-juneau-9.2.1-src.zip", "apache-juneau-9.2.1-bin.zip"):
        (out / name).write_bytes(b"payload " + name.encode())
        digest = hashlib.sha512((out / name).read_bytes()).hexdigest()
        (out / (name + ".sha512")).write_text(digest if digest_ok else "0" * 128)
        (out / (name + ".asc")).write_text("sig")
    listing = "".join(f"apache-juneau-9.2.1-{k}.zip{s}\n" for k in ("src", "bin") for s in ("", ".asc", ".sha512"))
    ctx.fake.script("svn", "ls", out=listing)


def test_dist_verify_checks_digest_and_signature(make_ctx):
    ctx = make_ctx()
    _local_dist(ctx)
    assert rc.dist_verify(ctx) == "dist/dev verified"
    assert len(ctx.fake.ran("gpg", "--batch", "--verify")) == 2
    bad = make_ctx()
    _local_dist(bad, digest_ok=False)
    with pytest.raises(StepError, match="SHA-512"):
        rc.dist_verify(bad)


def test_dist_verify_refuses_listing_gap(make_ctx):
    ctx = make_ctx()
    ctx.fake.script("svn", "ls", out="apache-juneau-9.2.1-src.zip\n")
    with pytest.raises(StepError, match="lacks"):
        rc.dist_verify(ctx)


def test_compose_vote_email_facts(make_ctx):
    ctx = make_ctx(state={"nexusRepoId": "orgapachejuneau-1002"}, answers={"email.vote": True})
    _local_dist(ctx)
    ctx.fake.script("git", "-C", str(ctx.ws), "rev-parse", out="abc123\n")
    assert rc.compose_vote_email(ctx) == "vote email sent"
    kind, key, subject, kw = ctx.prompt.asked[-1]
    assert key == "email.vote" and subject == "[VOTE] Release Apache Juneau 9.2.1 RC1"
    assert "h=abc123" in kw["body"] and "orgapachejuneau-1002" in kw["body"]
    assert "SHA512_NOT_FOUND" not in kw["body"]
    assert ctx.rf.get("rcCommit") == "abc123" and ctx.rf.get("voteDeadline")


# ---- vote phase

from datetime import datetime, timedelta, timezone  # noqa: E402

from jrm_release import promote, vote  # noqa: E402


def test_vote_gate_pauses_phase_vote(make_ctx):
    ctx = make_ctx()
    with pytest.raises(ctx.pause) as e:
        vote.vote_gate(ctx)
    assert ctx.rf.get("phase") == "vote" and e.value.done is True
    assert "--resume" in e.value.message


def test_vote_tally_early_pause_not_done(make_ctx):
    soon = (datetime.now(timezone.utc) + timedelta(hours=10)).isoformat()
    ctx = make_ctx(state={"voteDeadline": soon}, answers={"vote.early": False})
    ctx.now = lambda: datetime.now(timezone.utc)
    with pytest.raises(ctx.pause) as e:
        vote.vote_tally(ctx)
    assert e.value.done is False


def test_vote_tally_validates_passed(make_ctx):
    past = (datetime.now(timezone.utc) - timedelta(hours=1)).isoformat()
    for tally, outcome in (({"plus1": 3, "zero": 0, "minus1": 0, "binding": "A, B, C"}, "passed"),
                           ({"plus1": 2, "zero": 0, "minus1": 0, "binding": "A, B"}, "failed"),
                           ({"plus1": 4, "zero": 0, "minus1": 4, "binding": "A"}, "failed")):
        ctx = make_ctx(state={"voteDeadline": past}, answers={"vote.tally": tally})
        ctx.now = lambda: datetime.now(timezone.utc)
        vote.vote_tally(ctx)
        assert ctx.rf.get("vote")["outcome"] == outcome


def test_result_failed_sets_vote_failed(make_ctx):
    ctx = make_ctx(state={"vote": {"outcome": "failed", "plus1": 2, "zero": 0, "minus1": 0, "binding": "A, B"}},
                   answers={"email.result": True})
    with pytest.raises(ctx.pause):
        vote.compose_result_email(ctx)
    assert ctx.rf.get("phase") == "vote-failed"


def test_result_passed_continues(make_ctx):
    ctx = make_ctx(state={"vote": {"outcome": "passed", "plus1": 3, "zero": 0, "minus1": 0, "binding": "A, B, C"}},
                   answers={"email.result": True})
    assert vote.compose_result_email(ctx) == "result email sent"
    assert ctx.rf.get("phase") != "vote-failed"


# ---- promote phase

PASSED = {"vote": {"outcome": "passed"}, "nexusRepoId": "r-1", "rcCommit": "abc123", "phase": "vote"}


def test_nexus_release_needs_typed_version(make_ctx):
    from conftest import FakeNexus
    nx = FakeNexus()
    ctx = make_ctx(nexus=nx, state=PASSED, answers={"confirm.nexus-release": False})
    with pytest.raises(StepError):
        promote.nexus_release(ctx)
    assert nx.posts == []
    assert ctx.prompt.asked[-1][3]["expect"] == "9.2.1" and ctx.prompt.asked[-1][3]["danger"] is True
    ctx2 = make_ctx(nexus=nx, state=PASSED, answers={"confirm.nexus-release": True})
    promote.nexus_release(ctx2)
    assert nx.posts == [("promote", "r-1")] and ctx2.rf.get("phase") == "promote"


def test_nexus_release_refuses_without_passed_vote(make_ctx):
    from conftest import FakeNexus
    nx = FakeNexus()
    ctx = make_ctx(nexus=nx, state={"nexusRepoId": "r-1"}, answers={"confirm.nexus-release": True})
    with pytest.raises(StepError, match="vote"):
        promote.nexus_release(ctx)
    assert nx.posts == [] and not ctx.prompt.asked


def test_dist_promote_single_svnmucc_with_prior_rm(make_ctx):
    ctx = make_ctx(state=PASSED)
    ctx.fake.script("git", "-C", str(ctx.ws), "tag", "--list", out="juneau-9.2.0\njuneau-9.1.5\njuneau-9.2.1-RC1\n")
    ctx.fake.script("svn", "ls", out="9.2.0/\nKEYS\n")
    promote.dist_promote(ctx)
    mucc = ctx.fake.ran("svnmucc")
    assert len(mucc) == 1
    flat = " ".join(mucc[0])
    assert flat.index("rm https://dist.apache.org/repos/dist/release/juneau/9.2.0") < flat.index("mkdir")
    assert flat.count(" cp ") == 6


def test_dist_promote_skips_prior_not_listed(make_ctx):
    ctx = make_ctx(state=PASSED)
    ctx.fake.script("git", "-C", str(ctx.ws), "tag", "--list", out="juneau-9.2.0\n")
    ctx.fake.script("svn", "ls", out="KEYS\n")
    promote.dist_promote(ctx)
    assert " rm " not in " ".join(ctx.fake.ran("svnmucc")[0])


def test_final_tag_on_rc_commit_and_skip_when_present(make_ctx):
    ctx = make_ctx(state=PASSED)
    promote.final_tag(ctx)
    assert ["git", "-C", str(ctx.ws), "tag", "-a", "juneau-9.2.1", "abc123", "-m", "Apache Juneau 9.2.1"] in \
        ctx.fake.argvs()
    assert any(a[:3] == ["git", "-C", str(ctx.ws)] and "push" in a for a in ctx.fake.argvs())
    ctx2 = make_ctx(state=PASSED)
    ctx2.fake.script("git", "-C", str(ctx2.ws), "ls-remote", out="abc123\trefs/tags/juneau-9.2.1\n")
    promote.final_tag(ctx2)
    assert not [a for a in ctx2.fake.argvs() if "tag" in a and "-a" in a]


def test_github_release_on_final_tag(make_ctx):
    ctx = make_ctx(state=PASSED)
    ctx.fake.script("git", "-C", str(ctx.ws), "tag", "--list", out="juneau-9.2.1\njuneau-9.2.0\njuneau-10.0.0\n")
    ctx.fake.script("gh", "release", "create", out="https://github.com/apache/juneau/releases/tag/juneau-9.2.1\n")
    promote.github_release(ctx)
    call = ctx.fake.ran("gh")[0]
    assert call[:4] == ["gh", "release", "create", "juneau-9.2.1"] and "--verify-tag" in call
    assert "--latest=false" in call            # 10.0.0 exists and is higher
    gh = next(c for c in ctx.fake.calls if c["argv"][0] == "gh")
    assert gh["env"]["GH_TOKEN"] == "s3cr3t-gh" and "s3cr3t-gh" not in " ".join(gh["argv"])
    assert ctx.rf.get("githubReleaseUrl").endswith("juneau-9.2.1")


def test_github_release_latest_left_to_gh_when_highest(make_ctx):
    ctx = make_ctx(state=PASSED)
    ctx.fake.script("git", "-C", str(ctx.ws), "tag", "--list", out="juneau-9.2.1\njuneau-9.2.0\njuneau-9.2.1-RC1\n")
    promote.github_release(ctx)
    assert "--latest=false" not in ctx.fake.ran("gh")[0]


def test_announce_waits_for_downloads(make_ctx):
    seen = []
    results = iter([False, False, True])
    ctx = make_ctx(state=PASSED, answers={"email.announce": True},
                   http_ok=lambda u: (seen.append(u), next(results))[1])
    promote.compose_announce_email(ctx)
    assert len(seen) == 3 and seen[0] == "https://downloads.apache.org/juneau/9.2.1/"


def test_followup_pause_on_no(make_ctx):
    ctx = make_ctx(state=PASSED, answers={"confirm.followup": False})
    with pytest.raises(ctx.pause) as e:
        promote.followup_checklist(ctx)
    assert e.value.done is False


def test_finalize_marks_released(make_ctx):
    ctx = make_ctx(state=PASSED)
    assert "released" in promote.finalize(ctx)
    assert ctx.rf.get("phase") == "released" and ctx.rf.get("last") == "finalize"


REFS = "origin/master\norigin/juneau-9.2.1-branch\norigin/9.3.x\n"
FORM = {"rc": 1, "summary": "Fixes."}


def _fields(ctx):
    return {f.name: f for f in ctx.prompt.asked[1][3]["fields"]}


def test_details_version_prefills_form_and_derives_dev(make_ctx):
    ctx = make_ctx(with_details=False, answers={"release.branch": "master", "release.details": dict(FORM)})
    ctx.fake.script("git", "-C", details.checkout(), "show", out="<version>10.0.0-SNAPSHOT</version>")
    d = details.ask(ctx, version="9.2.1")
    f = _fields(ctx)
    assert f["version"].default == "9.2.1" and f["developmentVersion"].default == "9.2.2-SNAPSHOT"
    assert d["version"] == "9.2.1" and d["developmentVersion"] == "9.2.2-SNAPSHOT"


@pytest.mark.parametrize("refs,want", [
    ("origin/master\norigin/juneau-9.2.1-branch\n", "juneau-9.2.1-branch"),
    ("origin/master\norigin/9.2.x\n", "9.2.x"),
    ("origin/master\norigin/9.3.x\n", "master"),
    ("", "master")])
def test_details_version_picks_branch_default(make_ctx, refs, want):
    ctx = make_ctx(with_details=False, answers={"release.branch": want, "release.details": dict(FORM)})
    ctx.fake.script("git", "-C", details.checkout(), "for-each-ref", out=refs)
    details.ask(ctx, version="9.2.1")
    assert ctx.prompt.asked[0][0] == "text" and ctx.prompt.asked[0][3]["default"] == want


def test_details_blank_dev_derives_from_entered_version(make_ctx):
    # the master pom is 10.0.0-SNAPSHOT: next_dev_version(10.0.0) is None, so no form default; user enters 9.2.1
    ctx = make_ctx(with_details=False, answers={"release.branch": "master",
                                                "release.details": {**FORM, "version": "9.2.1"}})
    ctx.fake.script("git", "-C", details.checkout(), "show", out="<version>10.0.0-SNAPSHOT</version>")
    d = details.ask(ctx)
    assert _fields(ctx)["developmentVersion"].default is None
    assert d["developmentVersion"] == "9.2.2-SNAPSHOT"


def test_details_stale_dev_default_follows_edited_version(make_ctx):
    ctx = make_ctx(with_details=False, answers={"release.branch": "b", "release.details": {
        **FORM, "version": "9.4.1", "developmentVersion": "9.2.2-SNAPSHOT"}})
    ctx.fake.script("git", "-C", details.checkout(), "show", out="<version>9.2.1-SNAPSHOT</version>")
    assert details.ask(ctx)["developmentVersion"] == "9.4.2-SNAPSHOT"


def test_details_bare_dev_version_gets_snapshot_suffix(make_ctx):
    ctx = make_ctx(with_details=False, answers={"release.branch": "b", "release.details": {
        **FORM, "version": "9.2.1", "developmentVersion": "9.2.2"}})
    ctx.fake.script("git", "-C", details.checkout(), "show", out=POM)
    assert details.ask(ctx)["developmentVersion"] == "9.2.2-SNAPSHOT"
