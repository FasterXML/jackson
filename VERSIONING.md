# Versioning: branches, versions released

This page explains relationship between Jackson git branches and versions being released.

## Major Versions

Jackson has three major versions:

* 1.x (`org.codehaus.jackson`) is deprecated and no versions are released -- new use is strongly discouraged
    * Sources are available via [jackson-1](https://github.com/FasterXML/jackson-1) repo.
* 2.x (`com.fasterxml.jackson`) is the previous major version, still actively maintained and (as of June 2026) the most widely adopted.
* 3.x (`tools.jackson`) is the newer actively developed and maintained version, on which new functionality will be added. It is recommended for new projects

All per-component repositories (like [jackson-databind](../../../jackson-databind)) contain branches for major versions 2 and 3 (but not 1).
Changes are rolled forward from 2.x to 3.x branches, but not in the reverse direction.

## Branching structure, naming

For each of 2 open major branches (2.x, 3.x), component repositories have:

* "Development Branch": `2.x`, `3.x`
    * For developing the next new minor version (2.23, 3.2)
    * All feature development occurs in these branches
* "Latest Release Branch" (LRB): currently, `2.22`, `3.1`
    * Most recently published minor version, always Open
* Zero or more "Maintenance Branches": currently, `2.0` .. `2.21`, `3.0`
    * Only some are open (see LTS section below)

Once a new minor version is released from Development Branch, a new LRB is created, and previous LRB becomes regular Maintenance Branch.

## Branch status: Open vs Closed

Open branch means branch from which new releases may be made: conversely no releases are planned from Closed branches (NOTE: releases from Closed branches may be made only under exceptional circumstances -- for severe security vulnerabilities -- and if so, usually "micro-patches" -- single-component (usually [jackson-core](../../../jackson-core) or [jackson-databind](../../../jackson-databind)), special 4-digit version (`2.21.4-20260607`)).

Current status (Open/Closed) of all branches is listed on [Jackson Releases](../../wiki/Jackson-Releases).

Of branch types:

* "Development Branches" are always Open since they are for developing the next minor version to release
* "Latest Release Branches" are also always Open: patches are released for the most recently released minor versions
* Some of "Maintenance Branches" designated as "Long-Term Support" (LTS) are Open; non-LTS branches are always Closed

### Long-Term Support (LTS) branches/versions

Jackson "Long-Term Support" (LTS) version concept was defined in [JSTEP-13](https://github.com/FasterXML/jackson-future-ideas/wiki/JSTEP-13).

When a new minor version is released from "Development Branch" (and matching "Latest Release Branch" created), a decision is made whether this branch should become an LTS branch or not:

* If it is, it will remain Open for _at least 2 years_
    * Status of each LTS is independent of other Branches -- EOL determined at the time of initial release
* If not, it will only remain Open as long as it is the "Latest Release Branch" (i.e. closed after following minor version is released).

## Version Numbering (X.Y.Z)

Jackson follows [Apache versioning](https://apr.apache.org/versioning.html) convention, and is similar to [Semantic Versioning](http://semver.org/) from external user perspective (but not strictly following SemVer specification).

That is:

* Major version upgrades (1.x -> 2.x, 2.x -> 3.x) can include all kinds of changes, regardless of compatibility. We do this so that:
    * Neither Java nor Maven package names are reused (we use different packages names): this allows different major versions to co-exist
*  Minor version upgrades can contain additions, new methods, and may deprecate existing functionality
* Patch releases need to fully replaceable and have no changes to source or binary compatibility

### 2.20.0 (most) vs 2.20 (jackson-annotations)

Starting with Jackson 2.20, one of components -- [jackson-annotations](https://github.com/FasterXML/jackson-annotations/) -- is versioned with just "major.minor" version, with NO PATCH version (in normal cases). See [JSTEP-1](https://github.com/FasterXML/jackson-future-ideas/wiki/JSTEP-1) for explanation of this difference.

NOTE: before 2.20, jackson-annotations versions did use patch level as well -- but all variations were identical (no changes in patch releases).

