"""Constants and the step table.  Endpoints mirror config/TargetProfile.prodDefault in the Java engine."""
import re
from collections import namedtuple

GIT_URL = "https://gitbox.apache.org/repos/asf/juneau.git"
GH_SLUG = "apache/juneau"
NEXUS_URL = "https://repository.apache.org"
NEXUS_PROFILE = "1a24bc7f954a70"
DIST_DEV = "https://dist.apache.org/repos/dist/dev/juneau"
DIST_RELEASE = "https://dist.apache.org/repos/dist/release/juneau"
DIST_ROOT = "https://dist.apache.org/repos/dist"
DOWNLOADS = "https://downloads.apache.org/juneau"
DEV_LIST = "dev@juneau.apache.org"
ANNOUNCE_LIST = "announce@apache.org"
JUNEAU_CHECKOUT = "~/git/apache/juneau"
MVN_CHECKSUMS = "-Daether.checksums.algorithms=MD5,SHA-1,SHA-512"

Step = namedtuple("Step", "num phase name title label skippable")

STEPS = [
    Step(1, "rc", "prerequisites", "Prerequisites", "Checks", False),
    Step(2, "rc", "prechecks", "Prechecks", "Checks", False),
    Step(3, "rc", "compose-propose-email", "Propose email", "Email", False),
    Step(4, "rc", "workspace", "Workspace", "Git", False),
    Step(5, "rc", "build-verify", "Build verify", "Maven", False),
    Step(6, "rc", "javadoc-verify", "Javadoc verify", "Maven", True),
    Step(7, "rc", "test-workspace-verify", "Test-workspace verify", "Maven", True),
    Step(8, "rc", "gpg-unlock", "Unlock GPG", "GPG", False),
    Step(9, "rc", "deploy-snapshot", "Deploy snapshot", "Maven", False),
    Step(10, "rc", "release-prepare", "Release prepare", "Maven", False),
    Step(11, "rc", "release-diff-review", "Release diff review", "Git", False),
    Step(12, "rc", "release-perform", "Release perform", "Maven", False),
    Step(13, "rc", "nexus-close", "Close Nexus repository", "Nexus", False),
    Step(14, "rc", "dist-stage", "Stage on dist/dev", "SVN", False),
    Step(15, "rc", "dist-verify", "Verify dist/dev", "SVN", False),
    Step(16, "rc", "compose-vote-email", "Vote email", "Email", False),
    Step(17, "rc", "vote-gate", "Vote gate", "Vote", False),
    Step(18, "vote", "vote-tally", "Tally vote", "Vote", False),
    Step(19, "vote", "compose-result-email", "Result email", "Email", False),
    Step(20, "promote", "nexus-release", "Release Nexus repository", "Nexus", False),
    Step(21, "promote", "dist-promote", "Promote on dist/release", "SVN", False),
    Step(22, "promote", "final-tag", "Final tag", "Git", False),
    Step(23, "promote", "github-release", "GitHub release", "GitHub", False),
    Step(24, "promote", "compose-announce-email", "Announce email", "Email", False),
    Step(25, "promote", "followup-checklist", "Follow-up checklist", "Manual", False),
    Step(26, "promote", "finalize", "Finalize", "Done", False),
]
NAMES = [s.name for s in STEPS]
SKIPPABLE = [s.name for s in STEPS if s.skippable]
PHASES = (("rc", "Release candidate"), ("vote", "Vote"), ("promote", "Promote"))


def step(name):
    return next(s for s in STEPS if s.name == name)


def parse_version(v):
    m = re.fullmatch(r"(\d+)\.(\d+)\.(\d+)(-SNAPSHOT)?", v or "")
    return (int(m[1]), int(m[2]), int(m[3])) if m else None


def next_dev_version(v):
    """x.y.(z+1)-SNAPSHOT for a maintenance release (z > 0); None when z == 0 (the caller must ask)."""
    t = parse_version(v)
    return f"{t[0]}.{t[1]}.{t[2] + 1}-SNAPSHOT" if t and t[2] > 0 else None


def prior_on_line(tags, v):
    """Highest non-RC juneau-x.y.z tag on v's major.minor line strictly below v, as a version string, or None."""
    t = parse_version(v)
    best = None
    for tag in tags:
        m = re.fullmatch(r"juneau-(\d+)\.(\d+)\.(\d+)", tag.strip())
        if m:
            c = tuple(int(g) for g in m.groups())
            if c[:2] == t[:2] and c < t and (best is None or c > best):
                best = c
    return ".".join(map(str, best)) if best else None
