# To improve the spinnaker sql storage performance by using bulk save api

| | |
|-|-|
| **Status**     | _**Proposed**, Accepted, Implemented, Obsolete_ |
| **RFC #**      | |
| **Author(s)**  | Sanjeev Thatiparthi (@sanopsmx) (https://github.com/sanopsmx)
| **SIG / WG**   | sig-security

## Overview

https://github.com/spinnaker/spinnaker/issues/6147

Spinnaker API currently supports saving one pipeline at a time. In order to save multiple pipelines,
the external system needs to iteratively call the save pipeline method. In a good deployment,
the pipeline save goes through multiple hops and can take 2 to 5s and can be slower in other deployments.
The overall time to update all the pipelines can run into hours.

### Goals and Non-Goals

To improve the performance of gate, orca and front50 microservices by using bulk insert query.
We need to write a bulk save api which accepts an list of pipelines in a single rest api call
and save them in front50 using bulk insert query.

## Motivation and Rationale

In many deployments, the pipelines are managed with templates and pipeline json structures are generated
from these templates based on enterprise requirements and then saved to Spinnaker using the API.
This operation occurs frequently enough that there is a requirement for efficiency in saving bulk pipelines in Spinnaker.
The changes to pipelines can occur in a Spinnaker deployments because of

1. Changes in policies associated with pipelines
2. Changes to metadata in the pipelines for better analytics of executed pipelines
3. Synchronizing pipelines in Spinnaker with Git spanning multiple applications

Applying the changes to pipelines with one pipeline at a time with currently available API takes hours to complete.
Since the operation of bulk updates is performed often and multiple organizations,
it makes sense to have Spinnaker support batch updates of pipelines.

## Timeline

Will be implemented in 1-2 weeks time from the approval of this governance PR.

## Design

We propose a solution of updating multiple pipelines in one API call.

We propose to use the batch update strategy of saving multiple pipelines in a single rest call.
The API will accept a list of pipelines and save it to a data store using bulk insert queries (SQL backend)
if supported else will fallback to existing one pipeline at a time.
The batch update API will also require service account configurations in the pipeline and
will not support dynamic service accounts for the pipelines. This allows for performance
improvement by the Gate API calling the front50 service without having to go through Orca.

We create a new rest api.This rest api will accept a list of pipelines as an array and pass the json to front50.

We use sql bulk insert to save the all the pipelines list to the database.

*Assumptions:*
This solution works only for RDBMS database store.
Save pipelines run in the context of an application, irrespective of the content of the pipeline.
In the case of bulk save, we will use the application name as “bulk save”.
The implication is that the tasks for bulk save will not show up under tasks in their respective applications.

*Performance Statistics*

These are the few performance stats collected using bulk insert queries( sql storage).

| Pipeline size | No. of pipelines | Total size | Round trip time in secs |        Configuration changes          | Comments |
|:-------------:|------------------|------------|-------------------------|---------------------------------------|----------|
|    ~7.1 kb    |       1000       |   ~7.1 MB  |         23.260          |  okhttp3 default timeout of 10 secs.  |          |
|    ~7.1 kb    |       1450       |   ~10.3 MB |         26.878          |  okhttp3 default timeout of 10 secs.  |          |
|    ~7.1 kb    |       1757       |   ~12.5 MB |         39.478          |  okhttp3 default timeout of 10 secs.  |          |
|    ~7.1 kb    |       3513       |   ~25 MB   |         80.371          | Increased okhttp3 timeout to 60 secs. |          |

