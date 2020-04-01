## SIG Bug Triage Process

SIGs should be triaging the issues with their matching label during every SIG
meeting. You can view
[a list of recently filed issues](https://github.com/spinnaker/spinnaker/labels/sig%2Fplatform)
on GitHub to click through the ones filed since your last meeting. (That link
shows issues for the Platform SIG, but you can easily edit the filter from
there.)

Each issue should have one of three outcomes:

1.  **Assigned to a user or added to an actively-maintained GitHub project.** An
    actively-maintained project has a team of people who are using it to plan
    future work.

1.  **Marked with the label "contributions welcome".** This is an indicator that
    we don't know anyone who will be working on the issue, but we would be
    interested in discussing it further if a contributor could be found. Please
    also add a message to the bug describing the situation:

    > Thanks for filing the issue. Unfortunately, no one is available to work on
    > this at the moment, but we welcome contributions from our community.
    > Before starting work on it, we'd strongly encourage you to discuss your
    > plans here or on the SIG's \[slack channel](https://join.spinnaker.io/).

    Add the `no-lifecycle` tag if the issue represents a bug that shouldn't be
    automatically closed just because no one is available to fix it right now.
    Add the tag `beginner friendly` if it looks like an issue that a new
    contributor could help with, which will also prevent it from being
    automatically closed.

1.  **Closed.** There are a few reasons why you might close a bug.

    If it appears to be a request for support and not a genuine bug, please
    close it with a message like this:

    > Thanks for filing this issue. It looks like this might be a request for
    > help rather than a bug. In that case, you're probably better off looking
    > for answers \[from our Slack community](https://join.spinnaker.io/), since
    > there is a larger pool of users there. Use the search functionality to
    > find users who have a similar experience, or post a message in \`#general`
    > or another related channel.

    Alternately, this might be a bug with no reproduction steps and not enough
    details to warrant a proper investigation. In that case, consider a response
    like the following:

    > Thanks for filing the issue. We aren't able to determine what the cause of
    > the issue is, and there aren't enough steps to reproduce for us to
    > recreate it ourselves. Unfortunately, this means we have to close the
    > issue. Please reopen it with some steps to reproduce if you are able.

    There will undoubtedly be other reasons to close an issue, but please leave
    a message when you do. Closing a bug with a message is a much friendlier
    experience than having it automatically closed by Spinnakerbot months later.
