// API-only CLA decisions. Dependencies are injectable for the offline harness.
module.exports = async function claNudge({ github, context, core, env = process.env, now = new Date() }) {
  const LABEL = 'cla-needed';
  const DONE_LABEL = 'cla-received';
  const LINK = 'https://github.com/FasterXML/jackson/blob/main/CONTRIBUTING.md#paperwork';
  const REQUEST_MARK = '<!-- cla-nudge:request -->';
  const REMINDER_MARK = '<!-- cla-nudge:reminder -->';
  const REQUEST =
    `Thank you for contributing this! Before we can merge any PR, we'd need a CLA, see ${LINK} .\n\n` +
    `Looking forward to reviewing this!\n\n${REQUEST_MARK}`;
  const REMINDER =
    `Gentle ping on the CLA: we still need it before we can merge this, see ${LINK} . ` +
    `Thanks again for the PR!\n\n${REMINDER_MARK}`;

  const HOUR = 3600 * 1000;
  const DAY = 24 * HOUR;
  const SLACK = HOUR;
  const PRE_LABEL_GRACE = HOUR;
  const dryRun = env.DRY_RUN === 'true';
  const intervalDays = Number(env.INTERVAL_DAYS);
  if (!Number.isFinite(intervalDays) || intervalDays * DAY <= SLACK) {
    core.setFailed('interval-days must be a finite number greater than 1 hour');
    return;
  }
  const interval = intervalDays * DAY;
  const maxReminders = Number(env.MAX_REMINDERS || 0);
  if (!Number.isFinite(maxReminders) || maxReminders < 0 || !Number.isInteger(maxReminders)) {
    core.setFailed('max-reminders must be a non-negative integer');
    return;
  }
  const simulatedNow = env.SIMULATE_NOW;
  if (simulatedNow && !dryRun) {
    core.setFailed('simulate-now requires dry-run: true');
    return;
  }
  const currentTime = simulatedNow ? new Date(simulatedNow) : now;

  if (Number.isNaN(currentTime.getTime())) {
    core.setFailed(`simulate-now is not a valid timestamp: ${env.SIMULATE_NOW}`);
    return;
  }

  const cutoffInput = env.SKIP_LABELED_BEFORE;
  const cutoff = cutoffInput ? new Date(cutoffInput + 'T00:00:00Z') : null;
  if (cutoff && (
    !/^\d{4}-\d{2}-\d{2}$/.test(cutoffInput) ||
    Number.isNaN(cutoff.getTime()) ||
    cutoff.toISOString().slice(0, 10) !== cutoffInput
  )) {
    core.setFailed(
      `skip-labeled-before is not a valid date (expected YYYY-MM-DD): ${cutoffInput}`
    );
    return;
  }

  const { owner, repo } = context.repo;
  const days = ms => (ms / DAY).toFixed(1);
  const latest = dates =>
    new Date(Math.max(...dates.map(d => new Date(d).getTime())));

  const skipReason = pr => {
    const labels = pr.labels.map(label =>
      typeof label === 'string' ? label : label.name
    );
    if (pr.state !== 'open') return 'PR is no longer open';
    if (pr.locked) return 'conversation is locked';
    if (labels.includes(DONE_LABEL)) return 'also labeled ' + DONE_LABEL;
    if (!labels.includes(LABEL)) return 'no longer labeled ' + LABEL;
    return null;
  };

  const isMaintainer = c =>
    c.user?.type === 'User' &&
    ['OWNER', 'MEMBER', 'COLLABORATOR'].includes(c.author_association);
  const isOurs = (c, mark) =>
    (c.body || '').includes(mark) &&
    ((c.user?.type === 'Bot' && c.user.login === 'github-actions[bot]') ||
      isMaintainer(c));

  const cla = String.raw`(?:CLA|Contributor License Agreement)\b`;
  const agreement = String.raw`(?:(?:a|an|the|your)\s+)?(?:signed\s+)?(?:individual\s+|corporate\s+)?\[?${cla}`;
  const requestPatterns = [
    String.raw`^(?:(?:could|can|would|will)\s+you\s+)?(?:(?:please|kindly)\s+)?(?:sign|submit|complete|provide|send|execute|return|fill out)\s+${agreement}`,
    String.raw`^(?:(?:we|you)\s+|we['’](?:d|ll)\s+)?(?:still\s+)?(?:need|require)\s+${agreement}`,
    String.raw`^(?:the\s+)?${cla}\s+(?:is\s+)?(?:still\s+)?(?:required|needed|missing|outstanding)\b`,
    String.raw`^we\s+(?:cannot|can['’]t)\s+merge\b[^.!?;]{0,50}\bwithout\s+${agreement}`,
  ].map(pattern => new RegExp(pattern, 'i'));

  // Positive clauses count on their own. A separate acknowledgment or
  // a condition about future receipt must not cancel an explicit ask.
  const maintainerAsked = (c, author) =>
    isMaintainer(c) && c.user.login !== author &&
    (c.body || '').split(/[.!?;,\n]/).some(clause =>
      requestPatterns.some(pattern => pattern.test(clause.trim()))
    );

  const issues = await github.paginate(
    github.rest.issues.listForRepo,
    {
      owner,
      repo,
      state: 'open',
      labels: LABEL,
      per_page: 100,
    }
  );

  const prs = issues.filter(i => i.pull_request);

  core.info(
    `${prs.length} open PR(s) labeled ${LABEL} in ${owner}/${repo}` +
    `${dryRun ? ' [dry run]' : ''}; now=${currentTime.toISOString()}`
  );

  const rows = [];
  let failures = 0;

  for (const pr of prs) {
    const author = pr.user?.login;

    let action;
    let why;

    try {
      why = skipReason(pr);
      if (why) {
        action = 'skip';
      } else {
        const comments = await github.paginate(
          github.rest.issues.listComments,
          {
            owner,
            repo,
            issue_number: pr.number,
            per_page: 100,
          }
        );

        const reminders = comments.filter(c => isOurs(c, REMINDER_MARK));
        const markedAsks = [
          ...comments.filter(c => isOurs(c, REQUEST_MARK)),
          ...reminders,
        ];
        const manualAsks = comments.filter(c =>
          !isOurs(c, REQUEST_MARK) && !isOurs(c, REMINDER_MARK) &&
          maintainerAsked(c, author)
        ).map(c => c.created_at);
        const reviews = await github.paginate(github.rest.pulls.listReviews, {
          owner,
          repo,
          pull_number: pr.number,
          per_page: 100,
        });
        for (const review of reviews) {
          if (
            review.state !== 'PENDING' &&
            Number.isFinite(Date.parse(review.submitted_at)) &&
            maintainerAsked(review, author)
          ) {
            manualAsks.push(review.submitted_at);
          }
        }
        let firstLabeledAt = new Date(pr.created_at);
        let labeledAt = firstLabeledAt;
        let lastUnlabeledAt = -Infinity;

        // Events are only needed to enforce the rollout cutoff or
        // to time reminders after a CLA request has been made.
        if (cutoff || markedAsks.length || manualAsks.length) {
          const events = await github.paginate(
            github.rest.issues.listEvents,
            {
              owner,
              repo,
              issue_number: pr.number,
              per_page: 100,
            }
          );
          const labelEvents = events.filter(e => e.label?.name === LABEL);
          const labelDates = labelEvents
            .filter(e => e.event === 'labeled')
            .map(e => Date.parse(e.created_at));
          if (labelDates.length > 0) {
            firstLabeledAt = new Date(Math.min(...labelDates));
            labeledAt = new Date(Math.max(...labelDates));
          }
          for (const event of labelEvents) {
            if (event.event === 'unlabeled') {
              lastUnlabeledAt = Math.max(lastUnlabeledAt, Date.parse(event.created_at));
            }
          }
        }

        const inCurrentCycle = c => Date.parse(c.created_at) >= labeledAt;
        const currentAsks = [
          ...markedAsks.filter(inCurrentCycle).map(c => c.created_at),
          ...manualAsks.filter(timestamp => {
            const time = Date.parse(timestamp);
            return time >= labeledAt ||
              (time >= labeledAt - PRE_LABEL_GRACE && time > lastUnlabeledAt);
          }),
        ];

        if (cutoff && firstLabeledAt < cutoff) {
          action = 'skip';
          why =
            `first labeled ${firstLabeledAt.toISOString().slice(0, 10)}, ` +
            `before skip-labeled-before`;
        } else if (currentAsks.length === 0) {
          action = 'request';
          why = 'no CLA request for the current label cycle';
        } else {
          const lastAsk = latest(currentAsks);

          const reminderCount = reminders.filter(inCurrentCycle).length;

          const labelAge = currentTime - labeledAt;
          const sinceAsk = currentTime - lastAsk;

          why =
            `label on ${days(labelAge)}d, ` +
            `last CLA comment ${days(sinceAsk)}d ago, ` +
            `${reminderCount} reminder(s) so far`;

          if (
            maxReminders > 0 &&
            reminderCount >= maxReminders
          ) {
            action = 'done';
            why += `; reached max-reminders=${maxReminders}`;
          } else if (
            labelAge >= interval - SLACK &&
            sinceAsk >= interval - SLACK
          ) {
            action = 'remind';
          } else {
            action = 'wait';
          }
        }

        if (
          !dryRun &&
          (action === 'request' || action === 'remind')
        ) {
          // Labels or PR state can change while the scan reads history.
          const issue = { owner, repo, issue_number: pr.number };
          const { data: current } = await github.rest.issues.get(issue);
          const reason = skipReason(current);
          if (reason) {
            action = 'skip';
            why = reason;
          } else {
            await github.rest.issues.createComment({
              ...issue,
              body: action === 'request' ? REQUEST : REMINDER,
            });
          }
        }
      }
    } catch (err) {
      failures++;
      action = 'error';
      why = err.message;
      core.warning(`#${pr.number}: ${err.message}`);
    }

    const verb =
      dryRun &&
      (action === 'request' || action === 'remind')
        ? `${action} (dry run)`
        : action;

    core.info(
      `${verb.padEnd(18)} #${pr.number} ${author ? `@${author}` : '(unknown author)'}: ${why}`
    );

    rows.push([
      `#${pr.number}`,
      author ? `@${author}` : '(unknown author)',
      verb,
      why,
    ]);
  }

  await core.summary
    .addHeading(
      `CLA nudge: ${owner}/${repo}${dryRun ? ' (dry run)' : ''}`,
      3
    )
    .addRaw(
      `${prs.length} open PR(s) labeled \`${LABEL}\`, now=${currentTime.toISOString()}`,
      true
    )
    .addTable([
      [
        { data: 'PR', header: true },
        { data: 'Author', header: true },
        { data: 'Action', header: true },
        { data: 'Why', header: true },
      ],
      ...rows,
    ])
    .write();

  if (failures > 0) {
    core.setFailed(
      `${failures} PR(s) failed; see warnings above`
    );
  }
};
