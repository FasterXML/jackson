# Versioning: branches, versions released

This page explains the relationship between Jackson git branches and versions being released.

See the [Jackson README](README.md#jackson-versions-releases) for the module catalog and current latest-stable versions; this page covers the branch/version model in full.

## Major Versions

Jackson has three major versions:

* 1.x (`org.codehaus.jackson`) is deprecated and no versions are released -- new use is strongly discouraged
    * Sources are available via [jackson-1](https://github.com/FasterXML/jackson-1) repo.
* 2.x (`com.fasterxml.jackson`) is the previous major version, still actively maintained -- and, as of June 2026, the most widely adopted
* 3.x (`tools.jackson`) is the newer actively developed and maintained version, on which new functionality will be added. It is recommended for new projects

All per-component repositories (like [jackson-databind](../../../jackson-databind)) contain branches for major versions 2 and 3 (but not 1).
Changes are rolled forward from 2.x to 3.x branches, but not in the reverse direction. See the [Jackson 3 Migration Guide](jackson3/MIGRATING_TO_JACKSON_3.md) for moving from 2.x to 3.x.

## Branching structure, naming

For each of 2 open major branches (2.x, 3.x), component repositories have:

* "Development Branch": `2.x`, `3.x`
    * For developing the next new minor version (2.23, 3.2)
    * All feature development occurs in these branches
* "Latest Release Branch" (LRB): currently (as of June 2026), `2.22`, `3.1`
    * Most recently published minor version, always Open
* Zero or more "Maintenance Branches": currently (as of June 2026), `2.0` .. `2.21`, `3.0`
    * Only some are open (see LTS section below)

(branch numbers above are examples valid as of June 2026; see [Jackson Releases](../../wiki/Jackson-Releases) for the current set)

Once a new minor version is released from Development Branch, a new LRB is created, and previous LRB becomes regular Maintenance Branch.

## Branch status: Open vs Closed

Open branch means branch from which new releases may be made: conversely no releases are planned from Closed branches (NOTE: releases from Closed branches may be made only under exceptional circumstances -- for severe security vulnerabilities -- and if so, usually "micro-patches" (see below)).
Closed branches are sometimes referred to as "End-of-Life" (EOL) branches; EOL and Closed can be considered synonyms.

Current status (Open/Closed) of all branches is listed on [Jackson Releases](../../wiki/Jackson-Releases).

Status for branch types is determined as follows

* "Development Branches" are always Open since they are for developing the next minor version to release
* "Latest Release Branches" are also always Open: patches are released for the most recently released minor versions
* Some of "Maintenance Branches" (ones designated as "Long-Term Support" (LTS), see below) are Open; non-LTS branches are always Closed

### Long-Term Support (LTS) branches/versions

Jackson "Long-Term Support" (LTS) version concept was defined in [JSTEP-13](https://github.com/FasterXML/jackson-future-ideas/wiki/JSTEP-13).

When a new minor version is released from "Development Branch" (and matching "Latest Release Branch" created), a decision is made whether this branch should become an LTS branch or not:

* If it is, it will remain Open for _at least 2 years_
    * Status of each LTS is independent of other Branches -- EOL determined at the time of initial release
* If not, it will only remain Open as long as it is the "Latest Release Branch" (i.e. closed after following minor version is released).

Current LTS branches are `2.18`, `2.21` and `3.1`; see [Jackson Releases](../../wiki/Jackson-Releases) for the authoritative, up-to-date list.

## Version Numbering (Apache)

Jackson follows [Apache versioning](https://apr.apache.org/versioning.html) convention, and is similar to [Semantic Versioning](http://semver.org/) from external user perspective (but not strictly following SemVer specification).

That is:

* Major version upgrades (1.x -> 2.x, 2.x -> 3.x) can include all kinds of changes, regardless of compatibility. To make this possible, each major version uses different Java packages and Maven groupIds, so that different major versions can co-exist on the same classpath -- which enables smoother incremental migration between major versions
* Minor version upgrades are backward-compatible: code compiled against the previous minor version keeps working. They may contain additions and new methods, and may deprecate existing functionality, but do not remove or break it
* Patch releases must be fully replaceable, with no source- or binary-compatibility changes

### Micro-patches

In rare cases the Project may release an updated version of just a single component -- this is usually for a significant security issue -- from a Closed branch. If so, a special version format is used:

* 4th version component is appended: it uses DATE of micro-patch after "base version" (most recent release): for example, `2.21.4-20260607`
* Micro-patch versions are meant to be used along with regular patches of other components, so, for example:
    * `jackson-core` `2.21.4-20260607` would be used with `2.21.4` of `jackson-databind`
    * You can think of Micro-patch versions as overrides of base patch version
* Micro-patches are usually released only for [jackson-core](../../../jackson-core) or [jackson-databind](../../../jackson-databind)

NOTE: there are no micro-patches of 3.x versions and it is possible there will not be any -- they were used more often with 2.x patches of "Polymorphic Deserialization" CVEs.

### 2.20.0 (most) vs 2.20 (jackson-annotations)

Starting with Jackson 2.20, one of components -- [jackson-annotations](https://github.com/FasterXML/jackson-annotations/) -- is versioned with just "major.minor" version, with NO PATCH version (in normal cases). See [JSTEP-1](https://github.com/FasterXML/jackson-future-ideas/wiki/JSTEP-1) for explanation of this difference.

NOTE: before 2.20, jackson-annotations versions did use patch level as well -- but all variations were identical (no changes in patch releases).
