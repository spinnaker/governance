# Account Registration Plugin

| | |
|-|-|
| **Status**     | **Proposed** |
| **RFC #**      | _Update after PR has been made_ |
| **Author(s)**  | Nima Kaviani (@nimakaviani), Gaurav Dhamija (@dhamijag), Manabu McCloskey (@nabuskey) |
| **SIG / WG**   | Plugins |

## Overview

The accounts registration plugin implements the [Spinnaker Account Management RFC](https://github.com/spinnaker/governance/blob/master/rfc/account-management.md)
to allow Spinnaker accounts to be added dynamically from an external source (wired over HTTP) without having to restart Clouddriver. The plugin
serves as a first implementation of the RFC and can be used both to load external accounts into Clouddriver and also as
an example for others interested in interfacing with the Clouddriver and supplying account data dynamically. While the implementation
can technically be modified to accommodate any type of account, the current implementation only supports EC2, ECS, and Lambda accounts.

### Goals and Non-Goals

Goals for the plugin are to:
 - Provide a plugin that can pull from an external account registry service
 - Enable dynamic addition or removal of EC2, ECS, and Lambda accounts to Clouddriver

Non-Goals are to:
 - Implement an exhaustive list of options other than a third party service to provide account data
 - Enable registration of other Spinnaker account types

## Motivation and Rationale

Details on the need for dynamic addition of Spinnaker accounts to Clouddriver are described in the [Spinnaker Account Management RFC](https://github.com/spinnaker/governance/blob/master/rfc/account-management.md).
The plugin provides an implementation based on the RFC.

By using this plugin, users of the plugin can achieve the following
- Supply EC2, ECS, and Lambada accounts dynamically to the Clouddriver via an external service
- Add or remove those accounts based on supplied data and without having to restart Clouddriver
- Control the frequency of pulling account data from the third party registry service along with other configuration options

Autodesk is the first customer aiming to deploy this plugin in production.

## Timeline

The first version of the plugin is implemented and lives under [awslabs/aws-account-registration-plugin-spinnaker](https://github.com/awslabs/aws-account-registration-plugin-spinnaker).

The final implementation which is planned to be fully compatible with the [Spinnaker Account Management RFC](https://github.com/spinnaker/governance/blob/master/rfc/account-management.md) is expected by
the end of October, early November 2020.

## Design

### Workflow

Following section outlines list of high level steps which are elaborated in a subsequent sequence diagram for the proposed
solution:

   1. On initial startup of CloudDriver, the plugin will reach out to the external service to get list of available accounts.
   1. On a scheduled basis, CloudDriver reloads incremental account details from the external service.
   1. During account lookup request with CloudDriver, if it is missing locally, then CloudDriver will reach out to the external service to
get account details.
   1. When a pipeline tries to use an account:
      - If the account has been synchronized with CloudDriver, it will proceed as usual
      - If the account is unknown, CloudDriver will get that specific account from the external service
   1. Deletions of accounts will happen on the scheduled poll within CloudDriver.

### Pros
- Simple and resilient workflow .
- Single source of truth which will be the external service.
- No explicit pub/sub mechanism required to synchronize CloudDriver instances.
- Reasonable performance as it does not require to refresh the entire Spring context.

### Cons
- This solution relies on periodic polling for external data source from each CloudDriver
 instance. This can lead to increased load on external dependency.

### Resilience

- In case of a 403 response from the external service, the plugin retries as the
  error code be due to expired AWS credentials.
- Other failures do not result on retries and it is left to the next sync loop
  for the plugin to try the external service again.

### Dependencies

- Existence of an external service the providers accound data and acts as the source of truth
- External service to implement the schema understandable by the plugin

## Drawbacks

- Only supports EC2, ECS, and Lambda accounts at this stage.
- The schema for the provided accounts can be more generic depending on use case.

## Prior Art and Alternatives

- SpringBoot config server can be used as an alternative but it doesnt solve the dynamic loading of accounts
and still results in delays during startup time.

## Security, Privacy, and Compliance

- The current implementation of the plugin resolves authentication through AWS roles and does not provide
any other authentication mechanism to connect to the external service.

- There is no authorization mechanism in place to filter out account data. It is primarily the responsibility
of the external service and the way authentication is resolved in different derivations of the plugin.

## Operations

For the plugin to be enabled, Clouddriver configuration needs to be updated with the plugin information as
instructed on the [GitHub page for the plugin](https://github.com/awslabs/aws-account-registration-plugin-spinnaker).

## Future Possibilities

- support other types of accounts
- implement other authorization and authentication mechanisms
