# Versioning: branches, versions released

This page explains relationship between Jackson git branches and versions being released.

## Major Versions

Jackson has three major versions:

* 1.x (`org.codehaus.jackson`) is deprecated and no versions are released -- new use is strongly discouraged
    * Sources are available via [jackson-1](https://github.com/FasterXML/jackson-1) repo.
* 2.x (`com.fasterxml.jackson`) is the older actively developed and maintained version; also most widely adopted as of June 2026
* 3.x (`tools.jackson`) is the newer actively developed and maintained version, on which new functionality will be added

All per-component repositories (like [jackson-databind](../../../jackson-databind)) contain branches for major versions 2 and 3 (but not 1); changes from 2.x branches are generally rolled forward to 3.x branches; reverse is not possible.

## Branching structure, naming

For each of 2 open major branches (2.x, 3.x), each component repositories have:

* "Development Branch": `2.x`, `3.x`
    * For developing the next new minor version (2.23, 3.2)
    * All feature development occurs in these branches
* "Latest Release Branch": currently, `2.22`, `3.1`
    * Most recently published minor version
* Zero or more "Maintenance Branches": currently, `2.0` .. `2.21`, `3.0`

Once a new minor version is released from Development Branch, a new LRB is created, and previous LRB becomes regular Maintenance Branch.

## Branch status: Open vs Closed

Open branch means branch from which new releases may be made: conversely no releases are planned from Closed branches (NOTE: releases from Closed branches MAY be made but only under exceptional circumstances -- for security vulnerabilities -- and if so, usually "micro-patches" -- single-component (usually [jackson-core](../../../jackson-core) or [jackson-databind](../../../jackson-databind)), special 4-digit version (`2.21.4-20260607`)).

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





