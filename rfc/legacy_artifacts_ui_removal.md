# Legacy Artifacts UI Removal

| | |
|-|-|
| **Status**     | _Proposed, **Accepted**, Implemented, Obsolete_ |
| **RFC #**      | [111](https://github.com/spinnaker/governance/pull/111) |
| **Author(s)**  | Maggie Neterval (`@maggieneterval`) |
| **SIG / WG**   | UI/UX SIG |
| **Obsoletes**  | [spinnaker/spinnaker/4777](https://github.com/spinnaker/spinnaker/issues/4777) |

## Overview

Spinnaker currently supports two artifact configuration experiences: the legacy
UI and a feature-flagged "rewrite" UI. This RFC outlines a plan to test the new
UI, make it the default artifact configuration experience, and remove the legacy
UI.

### Vocabulary

The rest of this RFC will refer to the two UIs as the _legacy UI_ and the _new
UI_:

- Legacy UI: Enabled by setting Deck's `artifacts` feature flag to `true`.
Disabled by default. The
[documentation](https://www.spinnaker.io/reference/artifacts/) currently
refers to this UI as the "standard UI", but it has also been referred to as the
"pre-rewrite UI."

- New UI: Enabled by setting Deck's `artifactsRewrite` feature flag to `true`.
Disabled by default. The
[documentation](https://www.spinnaker.io/reference/artifacts-with-artifactsrewrite/)
currently refers to this UI as the "new UI," but it has also been referred to as
the "artifacts rewrite UI."

### Goals
The goal of this RFC is to propose a plan to:
- Test the new UI.
- Resolve all bugs reported by users or discovered while testing the new UI.
- Enable the new UI by default.
- Communicate the new default to users.
- Remove the old UI.

### Non-Goals

- Compare the implementation details of the legacy and new UIs.
- Gather user feedback about the new UI in order to further improve it: while
this might be worthwhile, the scope of this RFC is limited to auditing the new
UI as-is to ensure that it is backwards-compatible with the legacy UI.

## Motivation and Rationale

- Pivotal wrote this
[design document](https://docs.google.com/document/d/10y1KIBoSWlerFxkkW8nJLZ_zspz7ToCRifnWoFHyp8k/edit?usp=sharing)
for improving the artifact configuration experience, and released an initial
implementation behind a feature flag in Spinnaker 1.13 (March 2019). However,
the design document did not include a plan for identifying validating users,
exposing the new UI by default, or removing support for the legacy UI.
- Pivotal originally
[intended to default the new UI to enabled](https://github.com/spinnaker/spinnaker/issues/4777)
in Spinnaker 1.16, but Pivotal is no longer working on Spinnaker.
- Supporting two artifacts-related feature flags has increased the complexity of
the artifacts configuration experience, which was already confusing for end
users, as evidenced by many conversations in Slack, GitHub issues, and the
Kubernetes SIG. There have also been a number of bugs reported in relation to
the new UI, but no comprehensive audit of the new UI by Spinnaker maintainers.

## Timeline

Each set of milestones should be complete by the time the corresponding Spinnaker version is released.

| Spinnaker Version | Release Window Opens | Milestones |
|-------------------|----------------------|------------|
| 1.20              | 2020-04-27           | New UI is enabled by default. Legacy UI is behind a feature flag.
| 1.21              | 2020-06-22           | Legacy UI is removed.
| 1.23              | TBD                  | Legacy UI documentation is removed.

## Design

- [x] Create a
[GitHub project](https://github.com/spinnaker/spinnaker/projects/17)
to organize existing user-reported issues related to the new UI.

- [x] Manually test the new UI. For each type of artifact and artifact-driven
workflow, test that pipelines configured with the legacy UI still work with the
new UI, and that pipelines configured with the new UI work as expected. This
includes artifacts configured in the pipeline configuration's Expected Artifacts
section, artifacts configured as expected artifacts inline in stages, and
artifacts configured in Produces Artifacts sections of stages. (Huge thank you
to ezimanyi@ for helping with this.)

- [x] Document and resolve all bugs discovered while manually testing the new UI
([deck/pull/8179](https://github.com/spinnaker/deck/pull/8179),
[deck/pull/8160](https://github.com/spinnaker/deck/pull/8160),
[deck/pull/8165](https://github.com/spinnaker/deck/pull/8165),
[deck/pull/8121](https://github.com/spinnaker/deck/pull/8121),
[deck/pull/8111](https://github.com/spinnaker/deck/pull/8111),
[deck/pull/8096](https://github.com/spinnaker/deck/pull/8096),
[deck/pull/8071](https://github.com/spinnaker/deck/pull/8071),
[orca/pull/3562](https://github.com/spinnaker/orca/pull/3562)).

- [x] For release 1.20, remove the `artifacts` and `artifactsRewrite` feature
flags from Deck ([deck/pull/8184](https://github.com/spinnaker/deck/pull/8184)).
The default UI will be the new UI, previously gated by the `artifactsRewrite` flag.
Users can revert to the legacy UI by enabling a new temporary feature flag,
`legacyArtifactsEnabled`. We will not expose this flag in Halyard and require it
be added manually to `settings-local.js`. Communicate this change by adding the
`@ValidForSpinnakerVersion` annotation to the `artifacts` and `artifactsRewrite`
feature flags in Halyard with an appropriate `upperBound` and `tooHighMessage`,
updating the artifacts documentation, and adding a section to the 1.20 release
notes ([halyard/pull/1620](https://github.com/spinnaker/halyard/pull/1620),
[spinnaker.github.io/pull/1806](https://github.com/spinnaker/spinnaker.github.io/pull/1806)).

- [] For release 1.21, remove the `legacyArtifactsEnabled` flag and all legacy
UI code. Communicate this change by updating the artifacts documentation and
adding a section to the 1.21 release notes.

- [] After release 1.23, we can remove the legacy UI documentation and Halyard
feature flags because no supported release (1.21, 1.22, 1.23) will include
legacy UI support.

## Prior Art and Alternatives

Instead of removing the legacy UI, we could do one of the following:
1. Continue to support both UIs. However, this
increases the maintenance burden for Spinnaker developers and adds complexity
for end-users.
2. Remove the new UI. However, initial feedback from users indicates that the
new UI is easier to use than the legacy UI.

Instead of eventually removing all of Deck's artifacts feature flags in favor
of a unified experience, we could preserve the ability to disable artifacts
altogether. However, the only change in pipeline configuration experience for
users who currently have artifacts disabled will be the addition of the optional
_Artifacts Constraints_ input to the Automated Triggers section. Since this is a
minimally intrusive addition, the benefits of maintaining and documenting a
single configuration experience likely outweigh this risk.

## Known Unknowns and Risks

The new UI has not been exhaustively tested with all artifact types,
so there are an unknown number of bugs to resolve before Spinnaker 1.20 is
released and we default the new UI to enabled. If we cannot resolve all of the
bugs we encounter while manually testing the new UI, we will delay defaulting
the new UI to enabled to release 1.21 and update this RFC's timeline
accordingly.
