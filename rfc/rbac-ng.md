# RBAC for Kayenta and Spinnaker

| | |
|-|-|
| **Status**     | _**Proposed**, Accepted, Implemented, Obsolete_ |
| **RFC #**      | _Update after PR has been made_ |
| **Author(s)**  | Matt Sicker <msicker@apple.com> |
| **SIG / WG**   | sig-security |

## Overview

Kayenta is a canary analysis service used by Spinnaker and other projects.
It does not implement any form of access controls to its APIs beyond the authentication requirements of any reverse proxy in front of it such as Gate.
Normally, access controls would be enforced via Fiat, though this approach has not scaled well and duplicates permission storage with a polling mechanism.
Fiat is designed to collect resource metadata from other Spinnaker services and provide permission evaluation in return.
The primary information Fiat provides access to are external role information for users along with a cache of application permissions which are otherwise stored by Front50.

Kayenta is intended to work as a standalone application, however, and makes a good case study for an improved model for _role-based access controls_ (aka _RBAC_) across Spinnaker.
To do this, the concept of _role policies_ will be introduced as an abstraction to link together roles and permissions to define the security policy of a resource such as an account.
[Open Policy Agent](https://www.openpolicyagent.org) (OPA) is a flexible policy engine commonly used for handling authorization, compliance, and other policy-related concerns by evaluating expressions on documents (structured schemaless data).
Fiat provides a natural facade for externalizing security policy enforcement with an engine like OPA.
Starting with Kayenta, OPA needs to be informed of applications, metric store accounts, external roles, and role policies.
Metric store accounts and role policies will need backing storage to allow for runtime management.
Account credential management APIs should be added that work with these new access controls and allow permissions to be defined and managed on accounts while being configured.
This will make Kayenta more multi-tenant friendly and provide guidance on how to further enhance RBAC in Spinnaker.

### Goals and Non-Goals

Goals:
- Add support for role policies in Fiat with Gate APIs
- Add API for storing and managing account definitions in Kayenta to integrate with user secrets
- Add Open Policy Agent as a storage and permission evaluator backend for Fiat
- Add support for role policies to Kayenta metric store accounts
- Update Front50 to push application permission changes to Fiat

Non Goals:
- Provide APIs for managing security policies directly via Rego or other potential policy backends

## Motivation and Rationale

Kayenta APIs are all provided fairly openly assuming an admin has configured it with usable account credentials.
Account credentials can be configured for metric stores and object stores, the latter of which can also be used for configuration stores.
Object stores are used for storing and retrieving metric query results which are each assigned random UUIDs upon creation.
Configurations are encouraged to be shared and are viewable by all applications by default; as such, these are the only object stores that list objects from the store.
It is metric stores where lack of access controls causes issues in multi-tenant environments as sources for metrics are typically guarded by team-specific credentials.
All these accounts are defined as Spring beans through configuration files provided by the Spinnaker admin.

Supporting self-service configuration of metric store accounts requires some form of access controls as metric stores may contain sensitive data.
A self-service API should learn from the equivalent feature in Clouddriver how to integrate with user secrets and manage account definitions to provide a similar API for Kayenta.
Users will then be able to configure Kayenta accounts at runtime rather than requiring a Spinnaker admin to update Kayenta's configuration each time.
Multi-tenant usage of Kayenta will become feasible.

## Timeline

The initial Kayenta and dependent work is being developed during 2023Q1.
Further integration with other Spinnaker microservices, role policies, and permission change tracking would be targeted for afterwards in 2023.

## Design

This consists of a few related security elements: authentication, authorization, and credentials management.
Authentication is handled by Gate typically, so this will reuse the existing `X-Spinnaker-User` header for denoting the principal of the request.
User details are tracked by Fiat, so this data will need to be mirrored into OPA as a new backend.
Authorization is the more complex part as permission data is stored by the various microservices that own the resources.
These checks are typically handled through the `PermissionEvaluator` interface from Spring Security which takes three arguments: the authenticated user, the resource, and the permission.
Permission evaluation will be given the context of what roles the requesting user has, what permissions apply to applications, and what permissions apply to accounts.
As applications are checked by multiple services, these permissions will be pushed by Front50 into Fiat so that any service can use application permissions.
External role data can either be pushed from Gate on login (and when service accounts are saved) or included as part of the input data to Fiat for policy decisions depending on if it's locally available (like being included as JWT metadata or X.509 attributes).
The Fiat API will be updated to support permission checks on these new resources along with supporting role policies.

Account permissions are most applicable to metric store accounts where only the `READ` and `WRITE` permissions apply.
Granting read permission to a metric store account allows a user to fetch metrics by querying its `MetricsService` bean.
Granting write permission to a metric store account allows a sid to manage permissions of the account, update the account configuration, and delete the account.
Note that write permissions in this context are unrelated to writing metrics to a metrics store; metrics should be written to stores from the deployed applications being measured.
Access controls will be defined in terms of role policies which are intended to replace direct use of permissions maps.

Credentials management will be implemented through a SQL provider for managing metric store account definitions along with integration with user secrets.
When credentials definitions are parsed into account credentials, the parser can replace user secret URIs with their decrypted data if the calling user has permission to read them.
These credentials can be combined with existing configuration file provided accounts.
A REST API will be added for saving and deleting credentials with the former providing an API for managing the permissions of said credentials.

### Dependencies

Open Policy Agent would be a new dependency from outside the project.
OPA is commonly used with a lot of the technology that Spinnaker integrates with.
This is intended to replace most of what Fiat currently does by externalizing authorization decisions which can enable massive code simplifications to Fiat if fully embraced.
Fiat would still need a SQL database for storing role policies as OPA is not meant for persistent storage.

Account management would add a requirement on a SQL database such as PostgreSQL or MySQL for role policies as well as account storage.
Kork SQL would be an added internal dependency in Kayenta.
The updated Fiat API will also be a new dependency in Kayenta for optional RBAC integration when using Gate and Fiat.

## Drawbacks

This is added complexity as Kayenta doesn't already enforce much of anything to do with security.
Without using Spinnaker, there's still a lot of Spring Security things to configure to fully benefit from using access controls.

## Prior Art and Alternatives

One obvious approach was to directly integrate with Fiat as the rest of Spinnaker has by letting Fiat poll for resources and permissions.
While this is a fairly familiar way of adding new resource types, past experience has shown that this approach is not scalable.
Porting over some of what Fiat does into Kayenta could also work, though this would be another custom system to maintain.
Direct use of OPA was also considered, though the flexibility enabled through using Fiat as a facade outweighed the benefits from a more direct setup.

More general permission systems like SpiceDB could potentially be useful, though this would make more sense when applying permissions in a much more fine-grained fashion or to a world-wide continuous delivery service with millions of concurrent users.
The choice of keeping Fiat as a facade does enable the potential integration with SpiceDB or other policy engines in the future, though.

Clouddriver provides an API for account management.
However, this API is based on the classes in Clouddriver.
Reusing this API in Kayenta would require adding some form of generic account type in Clouddriver which could perform next to no validation.
This would also complicate user secret permission enforcement.

## Known Unknowns

There is a lot of flexibility in using Open Policy Agent and adopting this general architecture for access controls.
While metric store accounts have a fairly straightforward permission model, other Spinnaker resources are more complex.
The exact API for configuring accounts and permissions may evolve.
This should both simplify the common use case while enabling advanced use cases via access to OPA plumbing.

Access controls don't make much sense without authentication, and providing authentication configurations for Kayenta beyond Spinnaker integration is out of scope for this RFC, but this doesn't prevent the addition of that in the future.
Object store and configuration store accounts initially sounded like prime candidates for credentials management, but these could be replaced with a direct SQL backend for both objects and configurations; this is out of scope for this RFC as well.

## Security, Privacy, and Compliance

This feature introduces essential access controls around Kayenta metric store accounts and integration with Spinnaker authentication.
It also depends on changes to access control checks in Gate around applications referenced in calls to Kayenta.

As with Clouddriver account management, this feature works with user secrets to avoid directly storing credentials for external services.
User secrets have their own permissions attached during secret delivery that Spinnaker uses to perform access control checks upon use.

## Operations

Enabling security in Kayenta requires an additional SQL database.
If this is the first SQL database being introduced into a Spinnaker or Kayenta environment, this adds a new system to operate.

The introduction of Open Policy Agent support for a Fiat backend makes fairly large changes in how Fiat is operated today.
Instead of configuring Fiat with URLs to poll, the individual services that have permission data will be configured to push to Fiat through a common Fiat SDK.
This should significantly reduce the chattiness and space usage of Fiat's current synchronization and polling.

## Risks

OPA is a very powerful policy engine.
Misuse can lead to performance issues [as documented in their performance guide](https://www.openpolicyagent.org/docs/latest/policy-performance/), though these are less likely than the existing performance issues with Fiat as the number of users and resources grow.
OPA is a fairly mature project, though it still makes 0.x releases indicating that the exact API has not been stabilized.
Any advanced use cases that depend on custom OPA policies may need to deal with compatibility issues until a 1.0 API is established.
Spring Security is a complicated project to configure and work with.
While avoiding custom implementations of security code is great advice, this does make it more complicated to understand in practice as it's third party code.

## Future Possibilities

Other Spinnaker services can push resource metadata to Fiat and integrate with role policies.
This would help reduce the lag and chattiness of these services introduced by Fiat's polling along with simplifying access controls across the application.

The introduction of SQL storage in Kayenta presents a natural opportunity to migrate object stores and configuration stores to also use SQL.
Canary configurations and canary results could have permissions defined for standalone usage of Kayenta where there is no application RBAC.
