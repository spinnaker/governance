#!/usr/bin/env kotlin

/*
To run this tool, first open an incognito window and log in as admin@spinnaker.io. Then run the
script and open the authorization link it presents in that window:

./create-sig-groups.main.kts -- --sig-name kubernetes \
                                --sig-leads ethan.rogers@armory.io,ezimanyi@google.com \
                                --dry-run

This is a tool to create the two SIG mailing lists for a new SIG (sig-foo@spinnaker.io and
sig-foo-leads@spinnaker.io). It will also attempt to update any existing groups to apply some
standard settings.

In particular, it will ensure that:
* Both groups are owned by sig@spinnaker.io and have no other owners
* The sig-foo@ group has sig-foo-leads@ as a manager (existing memberships remain)
* The sig-foo-leads@ group has the SIG leads as managers and no other memberships
* The sig-foo@ group has the settings listed below in `sigGroupSettings` and sig-foo-leads@ has the
  settings listed in `sigLeadsGroupSettings`
*/

@file:CompilerOptions("-jvm-target", "11")
@file:DependsOn("com.github.ajalt:clikt:2.6.0")
@file:DependsOn("com.google.apis:google-api-services-admin-directory:directory_v1-rev20190806-1.30.3")
@file:DependsOn("com.google.apis:google-api-services-groupssettings:v1-rev20190725-1.30.9")
@file:DependsOn("com.google.oauth-client:google-oauth-client-java6:1.30.5")
@file:DependsOn("com.google.oauth-client:google-oauth-client-jetty:1.30.5")

import Create_sig_groups_main.Membership
import Create_sig_groups_main.MembershipBuilder
import com.github.ajalt.clikt.core.NoOpCliktCommand
import com.github.ajalt.clikt.parameters.options.convert
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.options.validate
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow
import com.google.api.client.googleapis.json.GoogleJsonResponseException
import com.google.api.client.http.HttpRequestInitializer
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.services.directory.Directory
import com.google.api.services.directory.DirectoryScopes
import com.google.api.services.directory.model.Group
import com.google.api.services.directory.model.Member
import com.google.api.services.directory.model.Members
import com.google.api.services.groupssettings.Groupssettings
import com.google.api.services.groupssettings.GroupssettingsScopes
import com.google.api.services.groupssettings.model.Groups
import com.google.common.collect.ImmutableMap

// Reference for these settings is here:
// https://developers.google.com/admin-sdk/groups-settings/v1/reference/groups
val sigGroupSettings: Groups = Groups()
  .setWhoCanJoin("ANYONE_CAN_JOIN")
  .setWhoCanViewMembership("ALL_MEMBERS_CAN_VIEW")
  .setWhoCanViewGroup("ANYONE_CAN_VIEW") // who can read the archives
  .setAllowExternalMembers("true") // allows users outside @spinnaker.io
  .setWhoCanPostMessage("ANYONE_CAN_POST")
  .setMessageModerationLevel("MODERATE_NON_MEMBERS")
  .setAllowWebPosting("true")
  .setPrimaryLanguage("en")
  .setIsArchived("true") // keep the message archive
  .setWhoCanContactOwner("ANYONE_CAN_CONTACT")
  .setWhoCanDiscoverGroup("ANYONE_CAN_DISCOVER")

val sigLeadsGroupSettings: Groups = Groups()
  .setWhoCanJoin("INVITED_CAN_JOIN")
  .setWhoCanViewMembership("ALL_MEMBERS_CAN_VIEW")
  .setWhoCanViewGroup("ALL_MEMBERS_CAN_VIEW") // who can read the archives
  .setAllowExternalMembers("true") // allows users outside @spinnaker.io
  .setWhoCanPostMessage("ANYONE_CAN_POST")
  .setMessageModerationLevel("MODERATE_NON_MEMBERS")
  .setAllowWebPosting("true")
  .setPrimaryLanguage("en")
  .setIsArchived("true") // keep the message archive
  .setWhoCanContactOwner("ANYONE_CAN_CONTACT")
  .setWhoCanDiscoverGroup("ANYONE_CAN_DISCOVER")

val options = Args()
options.main(args)

val scopes = if (options.dryRun) {
  // There is no readonly option for the groups settings unfortunately.
  setOf(DirectoryScopes.ADMIN_DIRECTORY_GROUP_READONLY, GroupssettingsScopes.APPS_GROUPS_SETTINGS)
} else {
  setOf(DirectoryScopes.ADMIN_DIRECTORY_GROUP, GroupssettingsScopes.APPS_GROUPS_SETTINGS)
}

val (directoryService, groupsSettingsService) = createServices()

println("Creating SIG ${options.sigName} with leads ${options.sigLeads}")

val sigGroupAddress = "sig-${options.sigName}@spinnaker.io"
val sigLeadsGroupAddress = "sig-${options.sigName}-leads@spinnaker.io"
val sigLeadsGroupMembership = MembershipBuilder()
  .put("sig@spinnaker.io", Role.OWNER)
  .putAll(options.sigLeads.map { it to Role.MANAGER }.toMap())
  .build()
val sigGroupMembership = MembershipBuilder()
  .put("sig@spinnaker.io", Role.OWNER)
  .put(sigLeadsGroupAddress, Role.MANAGER)
  .build()

configureGroup(sigLeadsGroupAddress, sigLeadsGroupSettings, sigLeadsGroupMembership, allowAdditionalMembers = false)
configureGroup(sigGroupAddress, sigGroupSettings, sigGroupMembership, allowAdditionalMembers = true)

class Args : NoOpCliktCommand(name = "create-sig-groups.main.kts") {

  val sigName by option(help = "the name of the SIG (without the 'sig-' prefix, e.g. 'platform')").required()

  val sigLeads by option(help = "comma separated list of SIG lead email addresses")
    .convert { it.split(',').toSet() }
    .required()
    .validate { if (it.size < 2) fail("You must specify at least 2 SIG leads, separated by commas") }

  val dryRun by option("--dry-run", help = "don't make any changes").flag("--nodry-run", default = false)
}

enum class Role {
  MANAGER, MEMBER, OWNER
}

typealias Membership = ImmutableMap<String, Role>
typealias MembershipBuilder = ImmutableMap.Builder<String, Role>

data class Services(val directoryService: Directory, val groupsSettingsService: Groupssettings)

fun createServices(): Services {
  val authFlow = GoogleAuthorizationCodeFlow.Builder(
    NetHttpTransport(),
    JacksonFactory.getDefaultInstance(),
    "244977121457-3ou7a63v9qffottjadng4bk25imsnvp6.apps.googleusercontent.com",
    // This is an "other" type client_secret, so it isn't really treated as a secret and is safe to
    // commit to git.
    "1-JIesQQ4W--O8ZsliPoVFYS",
    scopes).build()
  val credential = AuthorizationCodeInstalledApp(authFlow, LocalServerReceiver()).authorize("user")
  val directoryService = createDirectoryService(credential)
  val groupsSettingsService = createGroupsSettingsService(credential)
  return Services(directoryService, groupsSettingsService)
}

fun createDirectoryService(credentials: HttpRequestInitializer): Directory {
  return Directory.Builder(NetHttpTransport(), JacksonFactory.getDefaultInstance(), credentials)
    .setApplicationName("create-sig-groups.main.kts")
    .build()
}

fun createGroupsSettingsService(credentials: HttpRequestInitializer): Groupssettings {
  return Groupssettings.Builder(NetHttpTransport(), JacksonFactory.getDefaultInstance(), credentials)
    .setApplicationName("create-sig-groups.main.kts")
    .build()
}

fun configureGroup(groupAddress: String, newSettings: Groups, expectedMembership: Membership, allowAdditionalMembers: Boolean) {
  val (existingMembership, existingSettings) = createGroupIfMissing(groupAddress)
  patchSettings(groupAddress, existingSettings, newSettings)
  updateMembership(groupAddress, existingMembership, expectedMembership, allowAdditionalMembers = allowAdditionalMembers)
}

data class GroupData(val members: Membership, val settings: Groups)

fun createGroupIfMissing(groupAddress: String): GroupData {
  val group = getNullFor404 { directoryService.Groups().get(groupAddress).execute() }

  if (group == null) {
    println("Creating group $groupAddress")
    if (!options.dryRun) {
      directoryService.groups().insert(Group().setEmail(groupAddress))
    } else {
      // Since we didn't create the group, we can't query its data. Just return fake empty data.
      return GroupData(MembershipBuilder().build(), Groups())
    }
  }

  return GroupData(getMembers(groupAddress), getExistingSettings(groupAddress))
}

fun getMembers(groupAddress: String): Membership {
  val members = MembershipBuilder()
  var pageToken: String? = null
  do {
    val response: Members
    response = directoryService.members().list(groupAddress).setPageToken(pageToken).execute()
    pageToken = response.nextPageToken
    response.members.forEach { member -> members.put(member.email, Role.valueOf(member.role)) }
  } while (pageToken != null)
  return members.build()
}

fun getExistingSettings(groupAddress: String): Groups {
  return groupsSettingsService.groups().get(groupAddress).execute()
}

fun <T> getNullFor404(function: () -> T): T? {
  try {
    return function()
  } catch (e: GoogleJsonResponseException) {
    return if (e.statusCode == 404) null else throw e;
  }
}

fun patchSettings(groupAddress: String, existingSettings: Groups, newSettings: Groups) {
  newSettings.entries.forEach { (key, value) ->
    val existingSetting = existingSettings.get(key)
    if (existingSettings.get(key) != value) {
      println("$groupAddress: Setting $key to $value (from $existingSetting)")
    }
  }
  if (!options.dryRun) {
    groupsSettingsService.groups().patch(groupAddress, newSettings)
  }
}

fun updateMembership(
  groupAddress: String,
  existingMembers: Membership,
  expectedMembers: Membership,
  allowAdditionalMembers: Boolean) {

  val missingMembers = expectedMembers.keys - existingMembers.keys
  missingMembers.forEach { member ->
    val role = expectedMembers[member]!!
    println("Adding $member to group $groupAddress with role $role")
    if (!options.dryRun) {
      directoryService.members().insert(groupAddress, Member().setEmail(member).setRole(role.name))
    }
  }

  val additionalMembers = existingMembers.keys - expectedMembers.keys
  additionalMembers.forEach { member ->
    val role = existingMembers[member]!!
    if (!allowAdditionalMembers) {
      println("Removing $member from group $groupAddress")
      if (!options.dryRun) {
        directoryService.members().delete(groupAddress, member)
      }
    } else if (role != Role.MEMBER) {
      println("Changing $member to role MEMBER in group $groupAddress (from $role)")
      if (!options.dryRun) {
        directoryService.members().patch(groupAddress, member, Member().setRole(Role.MEMBER.name))
      }
    }
  }

  val checkRoles = existingMembers.keys.intersect(expectedMembers.keys)
  checkRoles.forEach { member ->
    val existingRole = existingMembers[member]!!
    val expectedRole = expectedMembers[member]!!
    if (existingRole != expectedRole) {
      println("Setting $member to role $expectedRole in group $groupAddress (was $existingRole)")
      if (!options.dryRun) {
        directoryService.members().patch(groupAddress, member, Member().setRole(expectedRole.name))
      }
    }
  }
}
