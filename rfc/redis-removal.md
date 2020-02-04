# Redis Removal

| | |
|-|-|
| **Status**     | Proposed |
| **RFC #**      | [100](https://github.com/spinnaker/governance/pull/100) |
| **Author(s)**  | Rob Zienert ([`@robzienert`](https://github.com/robzienert)) |
| **SIG / WG**   | Platform SIG, Operations SIG |

## Overview

To help reduce the technical debt of the project and improve the operational
overhead of running Spinnaker, Redis needs to be deprecated and removed from
being a persistence option in favor of using SQL as the baseline storage
technology. This RFC documents a process and timeline for doing so.

### Goals and Non-Goals

Goals:

*   Provide an inventory of Spinnaker services that still need SQL support added
*   Define a process that all services can use for migrating datastores without
    downtime
*   Provide a timeline for the final switch to SQL, along with criteria to
    decide if the timeline should be delayed

Non-Goals:

*   Debating if SQL is the best replacement default persistence backend to
    standardize on
*   Documenting and solving all technical issues for each service to migrate;
    those will be handled separately whereas this document concerns itself
    primarily with the larger processes and timeline
*   Identification of all new processes for maintaining services on a SQL
    backend; these will be handled separately
*   Prescription of what SQL vendor to use: Current functionality has been built
    using MySQL; support for other vendors will be addressed separately

## Case Against Redis

Redis was introduced into the project as an easy way to get started. It's
phenomenally flexible, and a single, common data store simplifies the
operational story of the large operational footprint that Spinnaker has.
Further, it was useful in that its flexibility meant services could be rapidly
iterated on, as no schema was needed from the start.

Unfortunately, Redis is difficult to operationalize as a persistent datastore,
which many of the services treat Redis as. Community members are often surprised
when Redis restarts to find Spinnaker has lost in-flight work. Operating Redis
beyond a cache is not a skill that many organizations have. Worse yet, the level
of effort to support a highly-available, persistent story for Redis is very high
and littered with edge cases. For example, when tested at Netflix, both Redis
Cluster and Sentinel regularly encountered incorrect master failover behavior
that would lead to corrupted clusters or lost data.

Furthermore, developing applications on Redis is typically a foreign concept and
offers a confusing touch point for anyone looking to get involved in
contributing.

Having a singular datastore for Spinnaker is still necessary, as it eases the
burden of onboarding. Now that Spinnaker is no longer a young project, the need
for fast service iteration has largely dissapated. SQL was chosen as the
replacement because modern SQL is flexible and well understood.

## Service Inventory

Most of the larger services already have support for SQL.

* Full support: ✔️
* Partial support: ~
* No support: ✗

| | |
|-|-|
| Clouddriver | ✔️ |
| Echo | ✗ |
| Fiat | ✗ |
| Front50 | ✔️ |
| Gate | ~ |
| Igor | ✗ |
| Kayenta | ~ |
| Keel | ✔️ |
| Orca | ✔️ |
| Rosco | ✗ |
| Swabbie | ✗ |

## Service Migration Process

_Some words on how services should be written such that they can be migrated
without downtime._

Being able to migrate existing deployments from Redis to SQL for each particular
service will be crucial. Each service must provide these features and
characteristics before they can be said to having "full support":

* A service must support a dual-backend mode, where writes are sent to the SQL
  store while reads can still hit Redis
* A service must support an idempotent, background data migrator for all data
  that needs to be migrated**
* A service must provide a clear log signal of when migration is complete

_** Caches and short-lived data do not need to be migrated_

## Timeline

This migration is assuming a slow migration cadence for the rest of the
Spinnaker services.

### 1.20 (early May)

Complete technical tasks of migrating Igor, Echo.

### 1.21 (early July)

Complete technical tasks of migrating Gate, Fiat.

### 1.22 (early August)

Complete technical tasks of migrating Swabbie, Rosco, Kayenta.

### 1.23 (early October)

At this point, all services will have the technical work complete to deprecate
Redis from Spinnaker. Code will be written that applies to each service, warning
operators Redis will be removed in a couple releases.

### 1.24 (early December)

Redis is set to default as disabled. SQL is expected.

### 1.25+ (early February, 2021 and onwards)

Redis is removed from the codebase. The exact release window this would fall
under will be determined preferrably via the Community Stats, or via surveys.
I'm hoping community stats can be used to provide insight into when a critical
mass of operators have transitioned before removing Redis entirely.

## Known Unknowns

For the services that have already been migrated to SQL, a period of a month or
so is typical to work out any unknown regressions.

For development, each service needs to identify a schema that will work for its
myriad of configuration options. Reasoning about how a service will operate in a
dual-storage backend deployment for both short- and long-lived lifecycles can be
challenging.

For operations, we are assuming that existing deployments have access to a SQL
database and that the exact system requirements of the databases will not be
known initially. It is assumed that smaller deployments will still want to
colocate all services onto the same SQL server, despite it being ill-advised for
production workloads. Development focus, however, will remain on optimizing for
a database-per-service paradigm.

Database migrations can take significant manual effort. While we'll strive to
make the process as hands-off as possible, the operators will still need to
schedule time to provision new hardware and migrate. 

## Operations

Individual operations teams will need to migrate their services between now and
(tentatively)Spinnaker 1.25. My assumption is that a majority of operators will
not be inclined to migrate until the deprecation notice deadline is looming, so
we should make the migration process for any given service straight-forward and
as hands-off as possible.

## Future Possibilities

As SQL is onboarded, Netflix has already begun experimenting with the likes of
CockroachDB for various services' use cases. Other stores may become available
as alternatives to partial storage needs of a particular service.
