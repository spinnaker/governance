# Migration of Spinnaker build and release artifacts to `spinnaker-community`

| | |
|-|-|
| **Status**     | _**Proposed**, Accepted, Implemented, Obsolete_ |
| **RFC #**      | TBD |
| **Author(s)**  | Travis Tomsu (`@ttomsu`) |
| **SIG / WG**   | Platform, Spinnaker-as-Code |

## Overview

The nightly builds and top-level Spinnaker release infrastructure is currently being migrated from `google.com`-owned GCP projects (multiple) to the CDF/`spinnaker.io` owned `spinnaker-community` project, so that we can open up the build and release process and make it more community-owned and operated.

There are several artifacts that currently live among the `google.com` projects that need to move to the new project that can't necessarily be simply copied over, including:
* `halconfig` bucket (owned by `spinnaker-marketplace`) - contains all BOMs and service-specific config for each of those BOMs. Configured in Halyard
* Each microservice container (owned by `spinnaker-marketplace`) - all containers registered in Google Container Registry (GCR), with each layer being stored in Google Cloud Storage (GCS)
* `spinaker-artifacts` bucket (owned by `spinnaker-marketplace`) - all released `spin` binaries, Halyard binaries, and `sponnet` library.
* `spinnakerbot` GKE cluster (owned by `spinnaker-build`) - Github bot for various issue and pull-request management runs here.

### Goals and Non-Goals

Goals:
* Move all resulting artifacts to the new project with minimal disruption

Non-Goals:
* Change *how* the artifacts are built or published - just where they are done. including:
  * Start using Github Package Repo
  * Use Google Cloud Soure Repo during container builds
  * Use of Jenkins to orchestrate the release process

## Motivation and Rationale

_What is the current state of the world?_
* Docker containers are orchestrated from a Jenkins instance in `spinnaker-community` and actually built, stored, and served from the `spinnaker-marketplace` project.
  * A nightly BOM and each release BOM is generated and stored in the `halconfig` bucket, along with copies of all microservice config files from their respective `halconfig/` directories in each repo.

* Halyard, spin, and sponnet releases are all orchested from the same Jenkins instance and are stored in their respective locations in `spinnaker-artifacts`

* `spinnakerbot` can be [built and deployed](https://github.com/spinnaker/spinnaker/blob/master/spinbot/Makefile#L11) by any developers with Google Container Builder (GCB) priviledges on the `spinnaker-marketplace` project.

_Why is this change being proposed?_

* Transparency in the process, remove Google as the sole owner/operator/bottleneck of the whole thing.

_Who have you identified as early customers of this change?_

* End-users and any new non-Google engineers that will be involved in the expanded nightly build-cop and release-manager duties.

## Timeline

_What is the proposed timeline for the implementation?_

ASAP

## Design

_What exactly are you doing?_


### `halconfig` bucket

GCS buckets exist in a global namespace, and cannot be directly transferred between projects.

Two possible approaches:
1. (Preferred): Copy all data from bucket `halconfig` in `spinnaker-marketplace` to `halconfig-temp` in `spinnaker-community`. Then delete the old bucket and recreate it in the new project.
2. Copy all data to `halconfig-v2` in the new project.
  * Update all Halyard config to use this new bucket
  * Update build and release config to start publishing here
  * Update any documentation on spinnaker.io

Approach 1 comes with some downtime as the data is transferred and buckets get renamed. Also comes with very slight risk of someone else grabbing the bucket name inbetween when the old bucket is deleted and the new one with the same name is created.

`spinnaker-artifacts` would most likely follow the same approach

### Microservice Docker registry

Each BOM references the default Docker registry to find each container (e.g. `artifactSources.dockerRegistry: gcr.io/spinnaker-marketplace`). Using approach #1 for the `halconfig` bucket would mean BOMs would be served from the `spinnaker-community` project but Docker/Kubernetes clusters would pull from GCR in `spinnaker-marketplace`. Google would have to maintain this project for approximately 6 months after the cutover (8 weeks * last 3 supported release = 24 weeks or about 6 months).

GCB builds would be cutover to `spinnaker-community` and all future BOMs would now have `artifactSources.dockerRegistry: gcr.io/spinnaker-community`

Because we probably don't want to lose 17+ releases, we could consider `docker pull` and `docker push`ing each container from one registry. This would then require updating all BOMs to use the new `artifactSources.dockerRegistry`. This whole thing isn't necessary to keep everything opertional, though.

### `spin` and `sponnet`

So long as the bucket name doesn't changed there shouldn't be any effect for these outside of some release script config (I assume this is config and not hardcoded, though I haven't checked).

### `spinnakerbot`

This one is different from all the others as it's a running service, but I'm still considering it an "artifact" that the Google team owns and maintains. Since it's already containerized, I'd be willing to experiment with running this on Cloud Run (like the stats service is currently slated to be run on) rather than have a GKE cluster solely for this purpose. I haven't done any more thinking about this though, so this is last because it's the weakest part of the proposal/update.

### Dependencies

Jenkins, Bintray, GCB, GCS, GCR, Halyard

## Drawbacks

Concentration of all-the-things into one project simplifies a lot of things, yet makes correct IAM permissions much more important. That being said, correct IAM permissions should always be very important, regardless of the workload, so in that light this change doesn't add any more additional risk.


## Known Unknowns

_What parts of the design do you expect to resolve through the RFC process?_

Since the whole point of this effort is to get more community involvement, I hope to get, ...wait for it.... more community involvement in this effort. After all, [the first rule of tautology club is the first rule of tautology club.](https://xkcd.com/703/)

_What related issues do you consider out of scope for this RFC that could be addressed in the future?_

I wrote up a doc on some idea for "what ideal looks like" [here](https://docs.google.com/document/d/1VR7n-6exwm_RspO18aDTZHZEMXgmL8vfz0CP4S3pUkM/edit) that could be explored after this cutover.

