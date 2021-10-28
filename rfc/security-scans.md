# Security Scans Proposal
### Overview
sig-security is proposing that the project integrate security scans of spinnaker.  There are multiple types of security scans including:
* OS Patches missing
* Static code analysis scans
* Out of date library scans

None of these are currently implemented in the spinnaker project.  We propose starting with static code analysis and adding additional scanning in time.

### Implementation
We plan to start by enabling CodeQL scans on a limited basis and expanding the scope of scans.  We'll start with Rosco
and roll out to other services after 5 PRs or 1 month or when the solution
meets the community needs and governance allows it to be rolled out faster via sig lead communications.

Steps to be taken:
* Install CodeQL analysis github action on "rosco".  This uses the "security" tab on the code repo to enable this functionality.
* Scans will be run on a CRON schedule.  NOT on PRs.  The intent is to identify these via internal/private results so that
any findings can be remediated before public disclosure.  This is making the assumption that it will find vulnerabilities
and they're valid.  If any are found, it validates the need for this tooling.  Once all vulnerabilities are remediated,
we can apply on PRs going forward to prevent any from being merged in.
* Once a set amount of time (1 month) has passed, repeat on additional projects:
  * Fiat
  * Deck
  * Gate
  * Orca
  * CloudDriver
  * Any remaining OSS repositories (Monitoring, Spin, Halyard, etc.)
* At anytime this can be halted or rolled back as needed based upon review at the 1 month mark from contributors/approvers.
* Additional scanning options can be added post review of at least two repositories succesfully scanning with CodeQL

### Definition of success
* At least one vulnerability uncovered and fixed
* PRs with security vulnerabilities fail the PR until fixed

