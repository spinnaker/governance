# Monorepo, Composite Build, and Unified Test/Release

|               |                                                             |
| ------------- |-------------------------------------------------------------|
| **Status**    | Proposed                                                    |
| **RFC #**     | [336](https://github.com/spinnaker/governance/pull/336)     |
| **Author(s)** | Joe Cavanagh ([`@jcavanagh`](https://github.com/jcavanagh)) |
| **SIG / WG**  | Platform SIG                                                |

## Overview

This document describes the unification of all Spinnaker service, UI, and directly supporting code into a Git monorepo.

Within that repository, create a single build, test, and release process.

This process will be somewhat intelligent - detecting changes in order to minimize the amount of work performed for a given PR or release build.

This is closely related to [previously-closed RFC 154](https://github.com/spinnaker/governance/pull/154)

### Goals and Non-Goals

This document describes:

- Consolidation of all Spinnaker service, UI, and directly supporting code into a single Git monorepo
- Unified build, test, and release processes for that single repository, smart enough to minimize work for a given task
- A mechanism to achieve this while also not destroying externally-maintained forks/extensions
  - Methods for maintaining external forks - both of old single repositories, and the new monorepo
- Date-oriented release versioning for all Spinnaker components, containers, and published libraries

## Motivation and Rationale

The many repositories that constitute Spinnaker today create excessive complexity and toil.  Build and release tooling is duplicated across many repositories, the release process itself is convoluted and laborious, and making changes to shared code is toilsome and error-prone.

A monorepo significantly streamlines the contribution, build, test, publish, and release process, which is the source of the majority of the aforementioned toil.

A major goal of the project is to improve new developer accessiblity.  A single repository to checkout, build and test a change, and submit a pull request is about the minimum possible burden to place on a new contributor.

Spinnaker's microservices have significant shared code.  For example, the `kork` library is used by every other service, including handling the key role of defining dependency versions across the application.  Changes here take many steps and pull requests to propagate across the application - the history is cluttered with auto-merges of `kork` bumps, and breaking changes downstream are difficult to catch early.  The `fiat` libraries parallel `kork` in most services, and are also widely shared and often version-bumped.

Spinnaker's microservices also share a common build target (the JVM) and share common build tooling (Gradle).  Gradle composite builds are an extremely natural fit for this project.

### A Developer Workflow Example

Developing on Spinnaker across its many repositories can be an unpleasant experience.

This is an example workflow of how a developer would make a breaking change to a common library, `kork`:

**The Separate-Repo Flow**

1. Checkout `spinnaker/kork` and make the change
2. Publish the `kork` library to `mavenLocal`
3. Find all references to that class or method
    - Let's say it's used in `orca`, `clouddriver`, and `fiat` for this hypothetical
4. Checkout `spinnaker/orca`, `spinnaker/clouddriver`, and `spinnaker/fiat`
5. Configure each of their Gradle builds to use the `kork` library you just published to `mavenLocal`
6. Make the requisite changes in the `orca`, `clouddriver`, and `fiat` projects, and fix any tests/other issues that may arise
7. Undo the Gradle changes to each project, removing the `mavenLocal` version of `kork`

Unfortunately, we are not done yet - we still need to submit and integrate our four PRs in the proper order.

8. Submit four PRs to each of our four impacted repositories
9. The PRs to `orca`, `clouddriver`, and `fiat` will fail, since they do not have access to that new `kork` version
10. Merge the `kork` PR, and wait for the new version to publish
11. Update all three of the `orca`, `clouddriver`, and `fiat` PRs with the new `kork` pin
    - If more than one breaking `kork` change is happening simultaneously, this change may cause conflicts on the `korkVersion` line
12. Merge all three service PRs

This is a significant amount of unnecessary toil - the level of effort required to make cross-cutting changes discourages new developers, and burns out maintainers.

This inertia against cross-cutting change affects the quality of that code.  If it is not easy to change, it will remain unchanged, be shimmed or facade-ed, or other means engaged in order to avoid the above toil.

**The Monorepo Flow**

1. Checkout `spinnaker/spinnaker`, and make the change in the `kork` subtree
2. The compiler will tell you what you have broken in `orca`, `clouddriver`, and `fiat`
    - Since our IDE is now aware of all code in the project, we can find all references to that code in seconds, and have access to things like IntelliJ's automatic refactoring tools
3. Make the requisite changes in the `orca`, `clouddriver`, and `fiat` projects, and fix any tests/other issues that may arise
4. Submit a single PR containing the complete change
5. Merge the single PR

A single repository for all code has many benefits:

 - No more version pinning or bumping
 - Cross-cutting changes across several services can be done atomically
 - Breaking changes in internal dependencies can be found quickly and resolved immediately
 - Minimizes barriers to new contribution

Expanding on the scope of the previous related RFC, `deck` and `spin` will also be moved into this monorepo.

## Timeline

TBD

## Design

### Gradle Composite Builds

The key component of the monorepo design is [Gradle's composite build feature](https://docs.gradle.org/current/userguide/composite_builds.html).  Explained in short:

1. All existing Spinnaker projects are combined into one source tree
1. A top-level Gradle build is created, which "includes" all existing Gradle projects into one build context
    - This requires little to no changes to the original projects.  Gradle will automatically understand Java conventions and dependencies, and rewrite libraries to use a just-built version from the local repository
    - e.g. If a dependency of `implementation "io.spinnaker.fiat:fiat-api"` exists in any other service, Gradle will match that to an included build's Java-convention group/package name, build Fiat locally, and provide that output as the dependency to the downstream project
    - One can imagine this as a completely automated `mavenLocal` -style workflow, but with the full power of Gradle behind it
1. New top-level Gradle tasks can wrap these projects together in arbitrary ways, and allows us to implement a fully unified build, test, publish, and release process
1. Consolidate all existing Github Actions tooling to leverage these composite tasks to build/test/publish/release one, several, or all services/libraries/etc
1. New contributions can implement complex features across many services trivially, in a single pull request
    - This drastically reduces the amount of labor required of both contributors and reviewers/committers
    - A change that does not function as intended can be reverted (or fixed) quickly in a single pull request

This does not come entirely for free, however:

1. The build can be heavy.  There's a lot of graph for Gradle to chew through, and it can take a few minutes even with a "hot" Gradle daemon.  Cold startup is a few minutes longer, to get dependencies built once.
    - Fortunately, Gradle caches work perfectly well with composite builds, and the cache has a *dramatic* impact.  Operating a Gradle cache is mandatory for CI (if the Gradle-GHA cache functionality is limited or insufficient), and should also be strongly considered for all development teams.
    - It is notable that included builds can be referenced just like Gradle project syntax from the command line to drill down to specific tasks.  e.g. `./gradlew :fiat:fiat-api:test` is perfectly valid in composite-monorepo form.  This is very useful when making a smaller change that does not require waiting for a larger test suite to run locally, and Gradle is not building more than it needs to.
1. With heavy builds comes longer CI times.  Some intelligence will need to be added to the Github Actions workflows to select Gradle tasks intelligently based on the incoming diff.
    - We use this kind of functionality heavily in our own forks/extensions - in combination with the cache, our CI times are ~30 minutes for a full build and test of all Spinnaker code.  Code that does not touch `kork` can omit many tests, and takes between 5-15 minutes.
1. Libraries still need to be published, even though the project itself no longer uses them - there are many external consumers.

### Monorepo Github Location

The monorepo will live at `github.com/spinnaker/spinnaker`.  All other repositories will be archived once the transition is completed.

### Primary/Default Branch Name

The primary/default branch of the monorepo will be renamed from `master` to `main`.

### Yes, Deck Too

This design explicitly integrates `deck` and `deck-kayenta` into the monorepo.  Cross-cutting changes do occur between the UI and the backend, and Deck is already a [Lerna](https://lerna.js.org) monorepo within itself - building all of it from its existing Gradle entry point is simple.

Linking the builds of `deck`, and `deck-kayenta` is not something Gradle can do, but we have two good options here:

1. Use the local `deck-kayenta` code during the build process of `deck`, using `yarn`'s workspaces feature
1. Just integrate `deck-kayenta` as a regular `deck` module, like all the other Lerna subprojects under `packages/*`

### The `spin` CLI

This project is actually the most difficult to integrate into the monorepo, as golang builds tend to be really finicky about their environment.  I think Github Actions should afford us the flexibility to partition that out reasonably, but that requires some investigative work.

There are definitely concrete benefits to including it in the build cycle - it can directly benefit from using adjacent Gate libraries, and it's much more testable when colocated against the APIs it is calling.  Setting up ephemeral CLI testing environments (on every change to Gate, even), is well within GHA's capabilities.

### Halyard

Halyard will also be included in the monorepo.  While it is a tool everyone wants to see replaced, it has no replacement yet.  It consumes many Spinnaker service libraries, and would greatly benefit from composite builds.  

### `spinnaker-monitoring`

The `spinnaker-monitoring` project will not be included in the monorepo.  The project is frozen, and not part of a release build.  The final revision's artifact will continue to be pinned in Halyard and re-tagged as needed for external consumption.

### Integrating Gradle Build Plugins

Gradle composite can also build plugins to be used in the same build, allowing us to also combine [Spinnaker's Gradle build plugin](https://github.com/spinnaker/spinnaker-gradle-project) into the project itself.

This is the only non-Spinnaker-service version that was pinned in Spinnaker Java projects, and including `spinnaker-gradle-project` into the monorepo enables trivial iteration on the Gradle build process.

### Releases and Versioning

Currently, each Java project within Spinnaker maintains its own semver-shaped version designation pinned within various other Spinnaker projects.  This is unnecessary with composite builds, as the project builds as one cohesive unit without any dependency versions at all.

However, containers, libraries, and other artifacts must still be published, and there is a need for a contract with library consumers (such as plugins).

#### Spinnaker Release Versioning and Scope

Spinnaker releases will use a date-based versioning scheme of `<year>.<number>.<patch>`, where `number` represents the number of major releases in that calendar year, and `patch` designates new builds produced for reasons outlined below.  The first numbered release of 2023 would therefore be `2023.0`, followed by `2023.1` and so on.  Patches to numbered releases are appended to the numbered release, e.g. `2023.0.1`.

The initial version of a numbered release is considered to have patch version zero, even if not explicitly stated for brevity.  Unqualified numbered releases refer to the latest patch of that numbered release.

Significant new feature work should result in a new release number.  Bugfixes, security fixes, or other important non-breaking, non-feature changes may be introduced in patch releases.  Small updates to existing functionality may be considered for patch releases if the changes are additive and non-breaking - this is left to the judgement of the reviewer and approver, along with the release manager.

All patch releases (`<year>.<number>.<patch>`) should be fully compatible with that version's initial release, and all previous patches to that release number.  Patch releases should be fully non-breaking, from external API all the way down to public function signatures in libraries.  Exceptions can be made to mitigate critical bugs or security issues.

Features and API annotated with `@Alpha` or `@Beta` annotations may change at any time.  However, it is desirable to minimize breakage in patch releases when possible, particularly if annotated as `@Beta`.  This is also left to the judgement of the reviewer, approver, and release manager.

No contract is given between numbered releases (`<year>.<number>`) - these may break, add, remove, or otherwise change any functionality deemed necessary by the project.

#### Release Timing and Cadence

Release timing and cadence is not addressed by this RFC.  When a new release is made during a calendar year, the release number increases by one.  When a patch release is made, the patch number increases by one.

The release manager retains the authority to release as they see fit.  Backports to older versions will be judgements made based on the criticality of the issue, risk to a release's stability, and the difficulty of backporting the change itself.

#### Release Branch Names

Release branches will have a naming scheme similar to today, but date-oriented.  A release branch is prefixed by `release-`, appends the release version, and is suffixed with `.x`, e.g. `release-2020.3.x`.

This branch will produce containers, libraries, and other artifacts for that numbered release.

#### Artifact Versioning Scheme

Spinnaker library and container versions from release branches will mirror the release version (e.g. `2023.3.4`).  This should be defined in the root `gradle.properties` file, and used throughout included builds and subprojects to publish their respective artifacts.

From `main`, library and container versions will be published as `main-<gha_run_id>`, e.g. `main-4041166540`.  Github Actions run IDs always increase, so this functions identically to timestamp-based snapshot versions with two key benefits:

1. The workflow that produced the artifact can be located trivially, which is extremely helpful when diagnosing many build and publish issues.
    - A Docker container named `main-4041166540` or jar named `clouddriver-aws-main-4041166540` can be found at `https://github.com/spinnaker/spinnaker/actions/runs/4041166540`
1. The shorter build ID is a lot more human readable, and there is no need for.

This number follows [Gradle's rules for version ordering](https://docs.gradle.org/current/userguide/single_versions.html#version_ordering) to allow prefix patterns and version ranges to work naturally.  For example, the latest build of the `main` branch's Java libraries can be used by asking Gradle to find version `main-+`.

Individual services/libraries will no longer have semver-shaped versions of their own.  Since all code is always built and published together, there is no need to declare any inter-service compatibility.  The compatibility contract is instead made against the project as a whole, rather than individual components.

Examples:
`main` branch publishes as: `<package>-main-<build_number>.jar`, e.g. `clouddriver-kubernetes-main-4041166540.jar`
`release-<version>` branches publish as: `<java_package>-release-<full_version>.jar`, e.g. `clouddriver-kubernetes-release-2023.3.4.jar`

This allows much simpler consumption of Spinnaker libraries, particularly in the case of plugins.  In this publishing form, building and testing plugins against many different Spinnaker releases can be achieved entirely via [Gradle's dependency resolution configuration](https://docs.gradle.org/current/userguide/resolution_rules.html), or via Gradle properties.  This is an extremely powerful feature that is critical to our everyday workflows.

### Plugin Repository Compatibility

The plugin system has a feature to allow a plugin repository entry to declare compatibility with certain Spinnaker service versions.  Those values are currently the service-specific semver-shaped library versions.

That compatibility declaration may need to be reworked to support date-oriented release numbering.  Code within the Spinnaker plugin system implementation may need to be reworked to accommodate the new Spinnaker library versioning.

## Monorepo Creation and Fork Management

Many forks of the open-source project exist in the wild, and it is critical that these survive the transition to a monorepo.

A key issue with [the prior RFC](https://github.com/spinnaker/governance/pull/154) was that the method of integrating the changes from each sub-project fully rewrote the Git history of each project, which would be terminally destructive to existing forks.

Instead, the monorepo will be created by importing each Spinnaker project as a [Git subtree](https://git-scm.com/docs/merge-strategies#Documentation/merge-strategies.txt-subtree).

The subtree method has one critical benefit:

1. The Git history of each project is entirely preserved, and can easily be be merged either direction with simple `git` commands.  Examples of each below.
    - Existing single-project forks can pull from a subtree of the new monorepo exactly as they would pull from a single repository today
    - Existing single-project forks can also contribute changes back with minor workflow changes
    - If an existing fork is itself converted into a monorepo via the subtree method, it can then pull directly from the main project.


There are some downsides, though:

1. Existing web links to pulls or issues will be broken.  However, links to indivdual commits will actually still work just fine.
    - `https://github.com/spinnaker/clouddriver/pull/5856` does NOT translate to `https://github.com/spinnaker/spinnaker/pull/5856`
    - `https://github.com/spinnaker/clouddriver/commit/46706fec79b0e909b8e99516f3328067f1504ad4` DOES translate to `https://github.com/spinnaker/spinnaker/commit/46706fec79b0e909b8e99516f3328067f1504ad4`

1. Subtree-merges performed to create the monorepo will not have their child commits rendered properly in the Github UI.  All commits are still present and directly addressable (as above), but Github does not follow the subtree pseudo-rename in the UI.  Github will report tens of thousands of commits in the entire tree, but only show the top-level commits for each subtree-imported project - future commits will be fully traversable in the Github UI as expected.

### Subtree Merges and Contributions From Existing Forks

This section details how to integrate changes between a single project repository (e.g. an existing OSS project or a private fork) and a subtree-ified monorepo of all Spinnaker projects.

Heavy use is made of Git's [`subtree` merge strategy](https://git-scm.com/docs/merge-strategies#Documentation/merge-strategies.txt-subtree).

Expects that you have [SSH-based Github authentication](https://docs.github.com/en/authentication/connecting-to-github-with-ssh) configured on your local machine.

#### Creation of the OSS Monorepo

A subtree-ified monorepo of the entire Spinnaker project can be created with the following script, following the `git init` command in your folder of choice:

```sh
#!/usr/bin/env sh

# Subtree clones
git subtree add -P clouddriver git@github.com:spinnaker/clouddriver.git master
git subtree add -P deck git@github.com:spinnaker/deck.git master
git subtree add -P deck-kayenta git@github.com:spinnaker/deck-kayenta.git master
git subtree add -P echo git@github.com:spinnaker/echo.git master
git subtree add -P fiat git@github.com:spinnaker/fiat.git master
git subtree add -P front50 git@github.com:spinnaker/front50.git master
git subtree add -P gate git@github.com:spinnaker/gate.git master
git subtree add -P halyard git@github.com:spinnaker/halyard.git master
git subtree add -P igor git@github.com:spinnaker/igor.git master
git subtree add -P kayenta git@github.com:spinnaker/kayenta.git master
git subtree add -P keel git@github.com:spinnaker/keel.git master
git subtree add -P kork git@github.com:spinnaker/kork.git master
git subtree add -P orca git@github.com:spinnaker/orca.git master
git subtree add -P rosco git@github.com:spinnaker/rosco.git master

git fetch --all
```

Congratulations!  All Git setup for the new monorepo is done!

#### Transitional Merges to the OSS Monorepo

Work will continue on the individual projects for some time.  Fortunately, the new monorepo can be easily updated from the individual projects with the following process:

1. Add remotes
    - `git remote add <individual_project_name> <git_url>`
    - Repeat for all projects

1. Merge changes from the individual repo to the monorepo using the `subtree` strategy
    - `git merge -X subtree=<subtree> <remote_name>/<branch>`
    - Repeat for each remote project and each branch that needs reintegration

This is a simple shell script that implements a process like the above:

<details>
<summary>Expand pull.sh</summary>

```sh
#!/usr/bin/env bash
set -e

# Usage:
# ./scripts/pull.sh [repo1] [repo2] [repo...] [-r|--ref REF]
# -r|--ref: Specify a ref to use for all remotes (default: main or master, varying if it's an internal or external repo)

# Examples:
# Pull from Clouddriver and Orca mirrors from ref release-1.27.x
# ./scripts/pull.sh clouddriver orca -r release-1.27.x

# Some OSS repos do not have release branches
MAIN_ONLY_PULLS=(deck-kayenta)

# Setup arguments
REF='main'
REPOS=()
INTERNAL=''
while [[ $# -gt 0 ]]; do
  key="$1"

  case $key in
    -r|--ref)
      REF="$2"
      shift
      shift
      ;;
    *) # Collect all positional arguments as remote repo names
      REPOS+=("$1")
      shift
      ;;
  esac
done

# Add in default repos if user did not provide any
if [ ${#REPOS[@]} -eq 0 ]; then
  REPOS=(clouddriver deck deck-kayenta echo fiat front50 gate igor kayenta keel kork orca rosco spin)
fi

function pull() {
  local repo="$1"
  local ref="$REF"

  # Some OSS repos do not have release branches - exclude them
  # shellcheck disable=SC2076
  if [[ $ref != 'master' ]] && [[ $ref != 'main' ]] && [[ " ${MAIN_ONLY_PULLS[*]} " =~ " ${repo} " ]]; then
    echo "Not merging $repo - marked for main-only pulls"
    return 0
  fi

  local prefix="$repo"
  local remote="github.com:spinnaker/$repo.git"

  echo "Merging into $prefix from $remote ($ref)..."
  git merge -X subtree="$prefix" "git@$remote" "$ref";
}

for repo in "${REPOS[@]}"; do
  pull "$repo"
done
```

</details>

This can be run daily, manually, or otherwise to integrate all changes from each individual repo to the monorepo.  The monorepo's build system is free to do as it wishes with regard to publishing these intermediate artifacts during the transitional period.  It's okay if this transition process takes significant time - switching does not become any more difficult as more work is performed on the individual projects - you just pull in all the changes wholesale at any time.

#### Integrating Individual Project Changes into a Subtree-ified Monorepo

This is basically the above transition process in more detail. This technique can also be useful for building your own bespoke monorepo of Spinnaker, and perhaps other Spinnaker-related projects or plugins that might benefit from composite builds or colocated code.

1. Add the OSS monorepo to your private individual fork as a remote
    - `git remote add oss git@github.com:spinnaker/spinnaker.git`
    - `git fetch oss`

1. Merge the OSS monorepo branch into your fork with the `subtree` strategy.  **DO NOT SQUASH OR REBASE THESE CHANGES - MERGE COMMIT ONLY!**
    - `git merge -X subtree=<subtree> oss/<branch>`
    - For example, if you have a `clouddriver` fork, and you want to integrate changes from `main`:
      - `git merge -X subtree=clouddriver oss/main`

1. A (mostly) equivalent command is `git subtree pull` or `git subtree merge`, but using that will create multiple "split" points in the tree, and make the history harder to traverse.  It is generally preferable to use `git merge -X subtree=<subtree> ...` instead.

1. Keep in mind that the merged code won't necessarily actually run - the changes could depend on additional changes in other projects, like `kork`.  Repeat the above for all Spinnaker projects that require re-integration.

#### Cherry-picking OSS Monorepo Changes to Private Individual Project Forks

Some fork maintainers may wish to pick and choose which code they take, rather than integrating the OSS branch wholesale.  This process looks a bit roundabout, but works similarly to how `git cherry-pick` operates under the hood.

1. Add the OSS monorepo to your private individual fork as a remote
    - `git remote add oss git@github.com:spinnaker/spinnaker.git`
    - `git fetch oss`

1. Retrieve and apply the diff to your tree with the following command:
    - `git show <commit_sha_to_pick> --no-color -- "<subtree>/*" | git apply -p2 -3 --index -`
      - The pattern matching string in this example is a subtree's folder, but it can be any path(s) if additional filtering is desired

1. This will pipe the diff of your desired change to `git apply`, filtering the files to one project subtree only (` -- "<subtree>/*"`), trimming the file paths in the diff by one additional directory (`-p2`), using three-way merge (`-3`), and updating the index (`--index`).  Modify other options to `git apply` as desired to suit your preferences and workflow.

#### Contributing a Change from a Private Individual Project Fork To the OSS Monorepo

Individual project forks can still contribute changes back to the OSS Monorepo, using a similar diff/apply flow as above, but in the reverse direction:

1. Fork the OSS monorepo on Github, and clone that monorepo fork locally - as one would any project.

1. Add your private individual fork as a remote to the cloned monorepo fork
    - `git remote add <remote_name> <url_to_private_individual_fork>`
    - `git fetch <remote_name> <url_to_private_individual_fork>`

1. Choose a branch name, and check it out using the appropriate remote base branch
    - `git checkout -b <name> <remote_name>/<remote_ref>`

1. Retrieve and apply the diff to your tree with the following command:
    - `git show <commit_sha_to_pick> | git apply --directory <subtree> -3 --index -`

1. This will pipe the diff of your desired change to `git apply`, adding the destination subtree directory prefix (`--directory <subtree>`), using three-way merge (`-3`), and updating the index (`--index`).  Modify other options to `git apply` as desired to suit your preferences and workflow.

1. Push your branch to your Github organization, and open a PR from there as usual
#### Integrating OSS Monorepo Changes into a Private Individual Project Fork

Once the OSS monorepo is the source of truth, private individual project forks will still exist and need to be maintained.

Unfortunately, this process is the most difficult.  There is not a way to cleanly merge from an OSS monorepo to a private individual fork, as changes to the OSS monorepo will include files from other services that do not exist in the destination single-service tree.

My recommendation here is that all private individual forks monorepo-ify themselves, using the creation and import process described elsewhere in this document, and leveraging the OSS composite build process.  Once all of your forks are combined into a private monorepo, the file paths will align and OSS changes can be integrated cleanly.

If you are already wholesale-integrating OSS changes into your forks, you can just import your forks as they are into your new monorepo, then pull from the OSS monorepo directly.

If you are not wholesale-integrating OSS changes from your forks, you can still pick changes from the OSS monorepo to your private monorepo using the standard cherry-pick process.

#### Viewing Subtree History

While the Github UI may not visualize subtree-merges correctly, all commits are present and one can still view it from the command line.  Here is a very basic example script that traverses subtree merge points and combines the log:

```bash
#!/usr/bin/env bash

# Usage: ./history.sh <subtree>/<file_path>

# TODO: Passthrough arguments to `git log`

# Split up the argument into subtree + path
SUBTREE_FILE_PATH=${1#*/}
SUBTREE=${1%%/*}

# Find the meta commit where the subtree was added, and find the parent of the subtree merge point (aka the "split")
SPLIT_COMMIT_MSG=$(git log --grep "git-subtree-dir: $SUBTREE" | grep "git-subtree-split: ")
SUBTREE_SPLIT_SHA=${SPLIT_COMMIT_MSG#*: }

# Output the combined log
(git log --color=always HEAD -- "$SUBTREE/$SUBTREE_FILE_PATH" && git log --color=always "$SUBTREE_SPLIT_SHA" -- "$SUBTREE_FILE_PATH") | less
```

## Composite Build Implementation

The monorepo build implementation we have internally is bespoke in many ways, and its exact implementation would likely not fit the open source project.  However, I'll go over some of the basic components here, what they look like, and how they would be used.  After RFC feedback, we can proceed with a proof-of-concept implementation for the open source project taking all of that into account.

Composite builds require the following basic components:

1. `includeBuild` statements in `settings.gradle`
1. `build.gradle` files at the root of each `includeBuild` path

There are a couple gotchas of composite builds that should be noted:

1. Top-level wrappers for commonly-used command-line tasks need to be separately implemented
    - Running tasks like `./gradlew check` or `./gradlew test` at the root from the command line implicitly recurses through Gradle subprojects to find those tasks, but not through Gradle included builds
    - In order to replicate this behavior, we need to define a task that does that recursion

1. Gradle configuration from `gradle.properties` is not passed on to included builds
    - Configuration from the environment or from command-line arguments IS passed on to composite builds, however
    - It's not difficult to copy these properties to an included build's context, it's just annoying

### Top-level Task Wrappers

As mentioned in the previous section, there is some Gradle subproject-recursion magic that needs to be replaced when using composite builds.

There are two places where top-level task wrappers should be defined:

1. At the root of the project, providing tasks to build/check/test/autoformat/etc all included builds:

    ```gradle
    // build.gradle
    ...
    // This finds all tasks named `test` from included builds, and creates a top-level `test` task that depends on all of them
    tasks.register('test') {
      it.dependsOn gradle.includedBuilds*.task(':test')
    }
    ...
    ```

1. Similar things must also be replicated for each included build, so we have the convenience of typing things like `./gradlew :clouddriver:test` - that does not work out of the box in composite builds.

    ```gradle
    // spinnaker-gradle-project plugin
    ...
    //This plugin is applied to all Spinnaker projects

    // `afterEvaluate` is a lifecycle method of a Gradle plugin that happens after initial evaluation of the Gradle task graph
    // All included builds are discovered and available, and we are generating some tasks for them here
    def afterEvaluate(Project project) {
      ...
      // Iterate through subprojects and collect tasks of our desired name
      project.task('test') {
        dependsOn project.subprojects*.tasks*.findByName('test').minus(null)
      }
      ...
    }
    ```
### Composite Examples

Below is a sketch of what `settings.gradle` looks like in a composite configuration:

<details>
<summary>Expand settings.gradle</summary>

```gradle
// Plugins need to be included first
includeBuild 'spinnaker-gradle-project'

// All non-buildscript projects
includeBuild 'clouddriver'
includeBuild 'deck'
includeBuild 'deck-kayenta'
includeBuild 'echo'
includeBuild 'fiat'
includeBuild 'front50'
includeBuild 'gate'
includeBuild 'halyard'
includeBuild 'igor'
includeBuild 'kayenta'
includeBuild 'kork'
includeBuild 'orca'
includeBuild 'rosco'
includeBuild 'spin'
```

</details>

Below is a sketch of what `build.gradle` looks like in a composite configuration:

<details>
<summary>Expand build.gradle</summary>

```gradle
defaultTasks 'build'

// Create top-level meta tasks, as Gradle does not automatically recurse through included builds to find tasks
tasks.register('build') { task ->
  // Add subproject 'build' tasks (or equivalent) here
  gradle.includedBuilds.each { build ->
    task.dependsOn build.task(':build')
  }
}

tasks.register('check') { task ->
  // Add subproject 'check' tasks (or equivalent) here
  gradle.includedBuilds.each { build ->
    task.dependsOn build.task(':check')
  }
}

tasks.register('clean') {
  it.dependsOn gradle.includedBuilds*.task(':clean')
}
```

</details>
