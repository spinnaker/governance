# User Secrets

| | |
|-|-|
| **Status**     | **Proposed**_ |
| **RFC #**      | _Update after PR has been made_ |
| **Author(s)**  | Matt Sicker (@jvz) |
| **SIG / WG**   | _Applicable SIG(s) or Working Group(s)_ |

## Overview

This proposal extends the Kork `SecretEngine` SPI to allow users to import and use secrets from an external secrets store while controlling authorization to these secrets inside Spinnaker.
User secrets are defined in a particular encoding format along with a secret type that determines how the secret data are parsed.
These secrets provide authorization metadata for integration with Fiat so that multiple tenants can share the same Spinnaker environment.

### Goals and Non-Goals

* Goals
  - Enable Spinnaker to use external secrets managers through a common Spinnaker identity which secrets owners can grant read access.
  - Enable Spinnaker to enforce access controls to these secrets based on authorization data delivered with the secret.
  - Enable Clouddriver account credentials to use authorized user secrets.
  - Enable SpEL expressions to use user secrets.
  - Enable other Spinnaker services to use user secrets.
* Non-Goals
  - Direct storage of secrets (best done through external secrets managers).
  - Modification of user secrets contents.
  - Management of user secrets.

## Motivation and Rationale

Clouddriver has an alpha feature for managing accounts backed by a SQL database.
Using the existing Kork `SecretEngine` API, account definitions can reference secrets through `encrypted:...` and `encryptedFile:...` URIs which are resolved at load time.
These engines rely on the Spinnaker runtime identity to make API calls to their respective services to retrieve the referenced secrets.
This functionality was originally used in Halyard for resolving secrets at deploy time where there is only one relevant identity to authorize.
Extending this secrets manager integration to enable multiple tenants requires additional authorization details as the access controls of the secrets manager are used for granting read access to Spinnaker itself.
Without the authorization details, any user could use one another's secrets if the same encrypted URI is used which is not secure in a multi-tenant setting.
These authorization details are included with other metadata and the secret data in the secret itself, so an encoding format is defined.

Early stakeholders for this effort are:

* Apple

## Timeline

The alpha version of this feature is targeted for 2022Q2 and is being contributed by Apple.
Development began in Q1 coinciding with the related Clouddriver Account Management API feature which initially integrated with the Kubernetes V2 provider.
User secrets will be integrated into this API along with updates to existing Kork `SecretEngine` modules (AWS Secrets Manager, S3, and GCS).
SpEL functions for using secrets in pipelines will be proposed.
Further integrations may be developed beyond Q2 before a beta version is released for broader use.

## Design

User secrets implement `UserSecret` which provides some common operations around extracting secret data and checking permissions.
Implementations provide a `type` property which determines the structure of the secret data.
These secrets can be referenced through a `secret://` (or `secretFile://`) URI using the `UserSecretReference` class to parse.
Each URI is structured as `secret://secret-engine-id?param1=value1&param2=value2&param3=value3` where _secret-engine-id_ is the `SecretEngine` identifier and the parameter key/value pairs are specific to each engine.
`SecretEngine` is updated with additional `decrypt()` and `validate()` methods for `UserSecretReference` values.
Each engine is configured to make requests for secrets using a single Spinnaker identity, typically the workload identity of Spinnaker itself for ease of integration with cloud-specific APIs.
To demonstrate, suppose we have a secret named `my-spinnaker-secret` in AWS Secrets Manager in the `us-west-2` region under the AWS account id `222211110000` while Spinnaker is running in EKS in the `us-east-1` region under the account id `111122223333`.
This secret would have an [ARN](https://docs.aws.amazon.com/general/latest/gr/aws-arns-and-namespaces.html) of `arn:aws:secretsmanager:us-west-2:222211110000:secret:my-spinnaker-secret`.
Then this secret can be made available to Spinnaker by attaching an IAM policy to this secret such as:

```json
{
  "Version": "2012-10-17",
  "Statement": [{
    "Effect": "Allow",
    "Action": ["secretsmanager:GetSecretValue"],
    "Resource": ["arn:aws:secretsmanager:us-west-2:222211110000:secret:my-spinnaker-secret"]
  }]
}
```

Then the URI `secret://secrets-manager?r=us-east-1&s=arn:aws:secretsmanager:us-west-2:222211110000:secret:my-spinnaker-secret&k=secret-key` will form the basis for user secret references to this secret.
Granting access in this direction allows Spinnaker to read the secret, but this is insufficient for providing access controls within Spinnaker around who is allowed to use this secret.
These access controls must instead be provided via the secret engine, typically by embedding the Spinnaker roles that are allowed to use the secret.
The `UserSecretMapper` class provides serialization and deserialization functionality for different types of user secrets.
At minimum, a `UserSecret` is a tree-like structure where the root object has a `type` field for indicating the type of secret.
This type corresponds to different structures for user secrets along with some way to check that a collection of roles are all allowed to use its data.

The initial type introduced is the `opaque` type which has the following fields to define.

* `roles`: a list of Fiat roles that are allowed to use the data in this secret.
Permission checks involving this list of roles must check that the set of target resource roles are all contained in this list.
* `data`: a map of keys to base64-encoded (basic variant) binary data.
* `stringData`: a map of keys to utf8-encoded string data.

For example, the following is a snippet of a secret named `eks-sa` in `us-east-1` in the AWS account id `123455432100` encoded as JSON:

```json
{
  "type": "opaque",
  "roles": ["admin", "sre"],
  "data": {
    "kubeconfig": "YXBpVmVyc2lvbjogdjEKY2x1c3RlcnM6..."
  }
}
```

Then using this secret in a new Clouddriver account:

```json
{
  "type": "kubernetes",
  "name": "eks",
  "permissions": {
    "READ": ["admin", "sre", "dev"],
    "WRITE": ["admin", "sre"]
  },
  "context": "eks",
  "namespaces": ["default", "sandbox"],
  "kubeconfigContents": "secret://secrets-manager?r=us-east-1&s=arn:aws:secretsmanager:us-east-1:123455432100:secret:eks-sa&e=json&k=kubeconfig"
}
```

Note that secrets can be referred to by just the secret name and not the full ARN when the secret is stored in the same Spinnaker workload AWS account.
However, it is expected that in a multi-tenant environment, different tenants may have different AWS accounts, thus the tenants are responsible for exporting their own secrets to Spinnaker through IAM policies.
Similar authorization strategies should be used in other external secrets managers.

For integration with SpEL, the following functions are introduced:

* `#secret(args)`: takes a map literal of the parameters provided to a secret engine.
A specific secret engine may be specified by the map key `engine` which may have a default value configured through a Spinnaker configuration property.
The value returned is a proxy object that allows for reading the user secret data but attempts to prevent accidental serialization of the secret to an insecure storage space.
* `#secretJson(args)`: equivalent to `#secret({e: 'json', ...args})`
* `#secretYaml(args)`: equivalent to `#secret({e: 'yaml', ...args})`
* `#secretCbor(args)`: equivalent to `#secret({e: 'cbor', ...args})`

For example, the following SpEL expressions are equivalent (assuming `secrets-manager` is the default engine):

* `#secretYaml({r: 'us-west-2', s: 'arn:aws:secretsmanager:us-west-2:333444455555:secret:spinnaker-deploy-bot'}).kubeconfig`
* `#secret({r: 'us-west-2', s: 'arn:aws:secretsmanager:us-west-2:333444455555:secret:spinnaker-deploy-bot', e: 'yaml'}).kubeconfig`
* `#secret({r: 'us-west-2', s: 'arn:aws:secretsmanager:us-west-2:333444455555:secret:spinnaker-deploy-bot', e: 'yaml', k: 'kubeconfig'})`
* `#secret({r: 'us-west-2', s: 'arn:aws:secretsmanager:us-west-2:333444455555:secret:spinnaker-deploy-bot', e: 'yaml', k: 'kubeconfig', engine: 'secrets-manager'})`

### Dependencies

This feature updates `kork-secrets`, `kork-secrets-aws`, and `kork-secrets-gcp`.
Existing Jackson Databind dependencies are used for JSON, YAML, and CBOR support.

## Drawbacks

User secrets requires two levels of authorization: authorization for the Spinnaker service(s) to read secrets from a secrets manager service, and authorization for users inside the Spinnaker service(s) to use these secrets.
Existing audit logs of secrets access in the external secrets manager may lose context on how secrets are used without aggregating audit logs from Spinnaker.
Secrets rotation scripts need to be updated to respect the format in use.

## Prior Art and Alternatives

Kork provides the `EncryptedSecret` class and corresponding URI formats for injecting secrets via Halyard and some limited support in Spinnaker services to use the same API.
This API is designed for single tenants and provides no access controls.

Dedicated secrets managers such as [Vault](https://www.vaultproject.io) provide much more sophisticated secrets management and delivery methods, though it is a complex system to manage in addition to Spinnaker and the cloud infrastructure where it runs.
While Vault could become another `SecretEngine` provider in Kork, requiring Vault when some users may already be using cloud-specific secrets managers or other secrets manager software would be too inflexible.

It may be possible to store the Spinnaker authorization data for secrets in secrets metadata APIs, though the implementation details on how each secrets manager exposes metadata varies quite a bit.
Since these access controls are not particularly sensitive, they don't _need_ to be encrypted with the secret, though this is the most direct way to deliver said access controls such that Fiat can understand them.

This `UserSecretManager` API would replace the tentative use of `SecretManager` in the Clouddriver Account Management API.
This API extends from the existing `SecretEngine` API, so it will integrate with existing engines fairly easily.

Some other approaches considered here explored the possibility for using more sophisticated identity and access management integration in Spinnaker using a framework such as [SPIFFE](https://spiffe.io) which could potentially be used to identify subjects inside Spinnaker and for generating appropriate certificates and tokens.
This approach was considered as the ultimate goal of using secrets in Spinnaker in the first place, though such an integration project would be much more expansive than a user secrets API.
While the SPIFFE approach is a better idea in theory, it would still require all the relevant integration points with Spinnaker to also support the same framework.
Integrating with external secrets managers provides more flexibility here as the secrets manager can be used for generating and rotating secrets for far more systems than are currently supported by SPIFFE.
A similar approach was considered specific to Kubernetes using JWTs that both Kubernetes and Fiat could understand and exchange, though this is similarly limited by the number of integration points that support OpenID-Connect or other JWT-based auth systems.

## Known Unknowns

Some aspects of this feature still require more research.

* Configuration strategy for how long to cache fetched secrets.
This needs to be designed so that secrets can be rotated while also avoiding unnecessary load on the relevant secrets manager APIs.
* How to configure multiple authentication identities for use with fetching secrets.
This currently relies on having a single Spinnaker identity to route secrets requests through, though in a more dynamic environment, there may be multiple workload identities in Spinnaker.
* How this would work in other cloud providers.
Kork only includes `SecretEngine` implementations for AWS and GCP; implementing equivalent engines for other cloud providers is out of scope for this initial RFC.

## Security, Privacy, and Compliance

This feature is a security feature and thus must be carefully reviewed.
The proposed architecture involves various Spinnaker services being granted read access to secrets along with those services keeping cached secrets from multiple tenants.
Use of secrets must be sufficiently guarded by Fiat; this can get complicated in scenarios where no human is involved in making a Spinnaker request (e.g., cron-triggered events).
Integration with Spinnaker service accounts should be carefully done.

## Operations

This adds a new human process that wasn't previously supported: the ability for one cloud account to share a secret with another cloud account.
This process relies on specifics to each secrets manager implementation.
Users should only grant read access to secrets that are used in Spinnaker, _not_ to all secrets in that user's account.

Metrics are defined for the time it takes to load a secret, the number of successful secret fetches, the number of failed fetches, the number of authorized secret uses, and the number of unauthorized secret use attempts.
Audit logs are defined for the same events.

## Risks

Security flaws in Spinnaker may allow an attacker to gain access to more unauthorized data if Spinnaker is configured with the ability to access said data.
This is not that different from the risks of operating a continuous delivery system in general as the system must enforce authentication and authorization to the resources it controls.
Most Spinnaker environments run as a single workload identity (or one per microservice), thus the amount of confidential data Spinnaker may have access to is nearly unlimited.

## Future Possibilities

User secrets may be key to delegating more admin-controlled actions to regular Spinnaker users.
This may be beneficial for supporting authenticated webhooks, authenticated artifact providers, and other authenticated APIs invoked by Spinnaker.
The Clouddriver Account Management API may be extended for use in other Spinnaker services when combined with user secrets.
