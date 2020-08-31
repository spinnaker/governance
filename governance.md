The Spinnaker OSS project is run by a group of appointed members who hold specific roles, managed by the Technical Oversight Committee. The Steering Committee manages the governance structure of the project, revisiting and refining it as needs evolve.

The broader community is organized around a set of Special Interest Groups (SIGs) focused on particular areas of interest.

## Roles

To make clearer to community members how they might progressively get more involved and take more ownership in the project, we define the following appointed roles with their respective duties and qualifications.

Roles are appointed by the Technical Oversight Committee (TOC). Roles can be revoked for reasons such as falling out of qualification, inactivity, or for violations of community guidelines.

| | | | |
| - | - | - | - |
| **Role** | **Responsibilities** | **Requirements** | **Defined By** |
| Member | Active contributor in the community | Sponsored by 2 Reviewers. | Spinnaker GitHub org member |
| Reviewer | Review contributions from other members | History of review and authorship in a project | [membership.yml](membership.yml) file |
| Approver | Approve incoming contributions | Highly experienced and active reviewer / contributor to a project | [membership.yml](membership.yml) file |
| SIG Lead | Facilitate SIG organization, set direction and priorities for a SIG | Demonstrated responsibility and excellent judgment for the domain | [membership.yml](membership.yml) file | 

### New Contributors

Everyone is a welcome community member! Here are some things that we find extraordinarily helpful:

* Answering questions in our [Slack team](https://spinnakerteam.slack.com/)
* Leave feedback on PRs - questions and bug spotting are all helpful
* Help new developers get started
* Reproduce bugs and add detail to issues
* Join (or lead!) a SIG and discuss issues facing our community

### Established Contributors

We expect established contributors to be welcoming and helpful towards new contributors, assisting new-comers on both the technical and non-technical aspects of the Spinnaker project.

### Member

Members are continuously active contributors in the community. They can have issues and PRs assigned to them. Members are expected to remain active contributors to the community.

#### Requirements

* Enabled [two-factor authentication](https://help.github.com/articles/about-two-factor-authentication) on their GitHub account.
* Have read the [Contributor Guide](https://spinnaker.io/community/contributing/).
* Have made multiple contributions to the project or community. Contributions may include, but is not limited to:
  * Authoring or reviewing PRs on GitHub
  * Filing or commenting on Issues in GitHub
  * Contributing to SIG or community discussions (e.g. meetings, Slack, email discussions)
* Subscribed to [spinnaker-dev@spinnaker.io](https://groups.google.com/a/spinnaker.io/g/spinnaker-dev)
* Sponsored by 2 Reviewers. **Note the following requirements for sponsors**:
  * Sponsors must have close interactions with the prospective member - e.g. code/design/proposal review, coordinating issues, etc.
  * Sponsors must be Reviewers or Approvers in the [Spinnaker org](https://github.com/spinnaker).
  * Sponsors must be from multiple member companies to demonstrate integration across the community.
* **[Open an issue](https://github.com/spinnaker/governance/issues/new) against the spinnaker/governance repo**
  * Ensure your sponsors are @mentioned on the issue.
  * Complete every item on the checklist.
  * Make sure that the list of contributions included are representative of your work on the project.
* Have your sponsoring reviewers reply confirmation of sponsorship: `:+1:`.
* Once your sponsors have responded, your request will be reviewed by the TOC. Any missing information will be requested.

#### Responsibilities and Privileges

* Responsive to issues and PRs assigned to them.
* Responsive to mentions of SIG teams they are members of.
* Active owner of code they have contributed (unless ownership is explicitly transferred).
  * Code is well tested.
  * Tests consistently pass.
  * Addresses bugs or issues discovered after code is accepted.
* They can be assigned to issues and PRs, and people can ask members for reviews.

**Note**: Members who frequently contribute code are expected to proactively perform code reviews and work towards becoming a Reviewer.


### Reviewer

Reviewers are able to review code for quality and correctness on some part of the project. They are knowledgable about both the codebase and software engineering principles. Reviewership is required before becoming an Approver (except under specific conditions; see Approver).

#### Requirements

* An active member of the community.
* Reviewed at least 5 [substantial](committee-technical-oversight/substantial-contributions.md) PRs to the codebase.
* Knowledgable about the codebase (exhibited via discussion, issues, thoughtful PR review, etc.)
* Sponsored by 1 Approver or TOC member.
  * Done through a PR to update the [membership.yml](membership.yml) file
* May either self-nominated or be nominated by an Approver of the project

#### Responsibilities and Privileges

The following apply to the part of the codebase for which one would be a Reviewer.

* Reviewer status may be a precondition to accepting large code contributions.
* Responsible for project quality control via code reviews.
  * Focus on code quality and correctness, including testing and factoring.
  * May also review for more holistic issues, but not a requirement.
* Expected to be responsive to reviews requested.
* Assigned PRs to review related to project of expertise.
  * Reviewers can self-assign into automatic reviewership by opening PRs against the `CODEOWNERS` file of the projects.

### Approvers

Approvers are able to both review and approve code contributions. While code review is focused on code quality and correctness, approval is focused on holistic acceptance of a contribution: backwards / forwards compatibility, adhering to conventions, performance and correctness issues, interactions with other parts of the system, and so-on.

Approver status is scoped to a part of the codebase.

#### Requirements

* Reviewer of the codebase for at least 3 months.
* Reviewed or authored at least 20 PRs to the codebase of [significant scope](committee-technical-oversight/substantial-contributions.md).
* Nominated by at least 2 other Approvers, SIG Leads, or TOC members.
  * With no objections from other Approvers or SIG Leads.
  * Done through PR to update the [membership.yml](membership.yml) file.

#### Responsibilities and Privileges

* Approver status may be a precondition to accepting large code contributions.
* Demonstrate sound judgement.
* Responsible for project quality control via code reviews.
  * Focus on holistic acceptance of a contribution, such as dependencies with other features, backwards / forwards compatibility, API definitions, etc.
* Expected to be responsive to review requests.
* Mentor contributors and Reviewers.
* May approve code contributions for acceptance.

#### Expedited Approvership

In rare circumstances, Members can directly climb to Approver without going through Reviewer. In these cases, unanimous approval must be reached by the TOC. Examples of scenarios that have caused an expedited Approver role have been:

* Subproject ownership transfers.


### SIG Leads

SIG Leads are responsible for management of a particular Special Interest Group within the project, managing day-to-day operations of their interest scope. If a particular SIG involves altering Spinnaker's codebase, then SIG leads must be active Approvers. 

#### Requirements

The process for becoming a SIG Lead should be defined in the SIG charter. Unlike the roles outlined above, the Leads of a SIG are typically limited to a small group of decision makers and updated as fits the needs of the subproject. A SIG Lead may be any level of contributor before being nominated.

* Deep understanding of the goals and direction of the SIG.
* Deep understanding of the domain of the SIG.
* Sustained contributions to design and direction by doing all of:
  * Authoring and reviewing RFCs.
  * Initiating, contributing, and resolving discussions.
  * Identifying subtle or complex issues in designs and implementation PRs.
* Directly contributed to the SIG's domain through implementation and/or review.

#### Responsibilities and Privileges

* Make and approve design decisions for the SIG.
* Set direction, priorities, and milestones for the SIG.
* Mentor and guide Approvers, Reviewers, and contributors to areas of the SIG's domain.
* Ensure continued health of the SIG.
  * Adequate test coverage for areas covered by SIG.
  * Tests are passing reliably (i.e. not flaky) and are fixed when they fail.
* Ensure a healthy process for discussion and decision making is in place.
* Work with other SIG Leads to maintain the project's overall health and success.
* Triage inbound issues related to the SIG.

**Note**: SIG Leads who are not Approvers are expected to proactively perform code reviews and submit contributions, working towards becoming an Approver.


### Technical Oversight Committee

The TOC is responsible for the overall technical management of the project, ultimately managing the day-to-day running of the project.

The TOC will have three, five, or seven members, as deemed appropriate by the Steering Committee. In the event that someone from the TOC leaves during their term, the Steering Committee shall appoint someone to fill the remainder of the term. Each year, the TOC will nominate a member to serve as the Chairperson for that year. The Chair's duties will include calling and runnings meetings, calling votes, and ensuring that meeting notes are recorded.

#### Requirements

* Nominated and Appointed by Steering Committee
  * 2-year terms, term limit of consecutive 4 years (after, individual must take at least 1 year off)
  * Terms should be staggered within the TOC and off by 6 months from SC 
  * Staggering should result in approximately half of the committee up for election each year
  * Term starts mid-calendar year (i.e. July 1st) 

Election to the TOC requires a majority of the SC.

#### Responsibilities and Privileges

* Set agenda for, facilitate, and drive consensus in the TOC meetings
* Final escalation point for technical guidance
* Review, approve and revoke appointments for Reviewers and Approvers
* Drive technical management and any specific guidelines (e.g. code requirements/conventions, SLA on PR reviews)
* Promote the best interests of the project


### Steering Committee

The steering committee’s responsibility and function lies in the continual shaping of the governance structure to serve the project’s needs best.

#### Requirements

* Nominated and Appointed by Steering Committee
  * 2 year terms, term limit of consecutive 4 years (after, individual must take off at least 1 year)
  * Terms should be staggered, which should result in approximately half of the committee up for election each year
  * Term starts w/calendar year (i.e. January 1st)

Election to the SC requires a majority of the SC. If a person is up for reelection they are not allowed to vote for their reelection. 

#### Responsibilities and Privileges

* Set governance structure of the project
* Address questions or concerns from the community about project culture, structure, and clarifications of roles, responsibilities and duties
* Ratify new SIGs

#### Logistics

* [Agenda doc](https://docs.google.com/document/d/1HMdwvBPM4uRFqoeAd7eEkVWIC8dQP40zFavOE5Kq-Eg/edit)
* Email: [sc@spinnaker.io](mailto:sc@spinnaker.io)

## Documentation Contributors

The role definitions above were written optimized for code contributors. Documentation contributors will not be held to the same technical requirements. Instead, we look for writers who can express concepts clearly and simply. Documentation contributors still follow the same Member, Reviewer, Approver progression ladder.

## Special Interest Groups

Special Interest Groups (SIGs) are created as community interest and demand around particular topics become self evident, and continue on as their relevance and needs do. SIGs and their Leads are ratified and green-lit by the Steering Committee via a [SIG proposal process](sig-lifecycle.md). 

## Inactive Members

Members are expected to be continuously active contributors in the community.

A core principle in maintaining a healthy community is encouraging active participation. It is inevitable that peoples' focuses will change over time and they are not expected to be actively contributing forever. However, being a member of the Spinnaker GitHub organization comes with an elevated set of permissions. These capabilities should not be used by those who are unfamiliar with the current state of the Spinnaker project.

Members with an extended period away from the project with no activity will be removed from the Spinnaker GitHub organization and will be required to go through the membership process again after re-familiarizing themselves with the current state. Prior contribution stats will not be counted towards role requirements. Any time restrictions on a role will be waived when rejoining the organization.

### How Inactivity is Measured

Inactive members are defined as members of the Spinnaker GitHub organization with **no** contributions across any project within 12 months. This is measured by the [CDF DevStats](https://spinnaker.devstats.cd.foundation/d/8/dashboards?orgId=1&refresh=15m) project.

**Note**: DevStats only accounts for code contributions. If a non-code contributing member is accidentally removed, they may open an issue in the governance repo to be quickly re-instated.
