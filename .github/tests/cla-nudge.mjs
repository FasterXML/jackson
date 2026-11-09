import assert from 'node:assert/strict';
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const here = path.dirname(fileURLToPath(import.meta.url));
const workflowPath = path.join(here, '../workflows/cla-nudge.yml');
const yaml = fs.readFileSync(workflowPath, 'utf8');
const scriptStart = yaml.indexOf('          script: |\n');
assert.notEqual(scriptStart, -1, 'reusable workflow must contain a script block');
const script = yaml.slice(scriptStart + '          script: |\n'.length)
  .split('\n')
  .map(line => line.startsWith('            ') ? line.slice(12) : '')
  .join('\n');

const DAY = 24 * 60 * 60 * 1000;
const REQUEST_MARK = '<!-- cla-nudge:request -->';
const REMINDER_MARK = '<!-- cla-nudge:reminder -->';
const LINK = 'https://github.com/FasterXML/jackson/blob/main/CONTRIBUTING.md#paperwork';
const REQUEST = `Thank you for contributing this! Before we can merge any PR, we'd need a CLA, see ${LINK} .\n\nLooking forward to reviewing this!\n\n${REQUEST_MARK}`;
const REMINDER = `Gentle ping on the CLA: we still need it before we can merge this, see ${LINK} . Thanks again for the PR!\n\n${REMINDER_MARK}`;

function pr(number, options = {}) {
  const created = options.created || '2026-11-01T00:00:00Z';
  return {
    number,
    state: 'open',
    user: { login: options.author || `author-${number}`, type: 'User' },
    created_at: created,
    pull_request: { url: `https://api.github.com/repos/owner/repo/pulls/${number}` },
    locked: options.locked || false,
    labels: (options.labels || ['cla-needed']).map(name => ({ name })),
  };
}

function comment(body, options = {}) {
  return {
    body,
    created_at: options.created || '2026-11-01T00:00:00Z',
    user: { login: options.login || 'reviewer', type: options.type || 'User' },
    author_association: options.authorAssociation || 'MEMBER',
  };
}

function makeHarness({ prs = [], comments = {}, reviews = {}, events = {}, currentPrs = {}, env = {}, failures = {} } = {}) {
  const calls = { list: 0, commentsFetched: [], eventsFetched: [], posts: [], failed: false, logs: [] };
  const github = {
    paginate: async (method, params) => (await method(params)).data,
    rest: { issues: {
      listForRepo: async params => {
        calls.list++;
        assert.deepEqual(params, { owner: 'FasterXML', repo: 'jackson-databind', state: 'open', labels: 'cla-needed', per_page: 100 });
        return { data: prs.filter(item => item.state !== 'closed' && item.labels.some(label => (typeof label === 'string' ? label : label.name) === 'cla-needed')) };
      },
      listComments: async params => {
        calls.commentsFetched.push(params.issue_number);
        if (failures.comments === params.issue_number) throw new Error(`comments failed for ${params.issue_number}`);
        return { data: comments[params.issue_number] || [] };
      },
      listEvents: async params => {
        calls.eventsFetched.push(params.issue_number);
        return { data: events[params.issue_number] || [] };
      },
      get: async ({ issue_number }) => {
        const listed = prs.find(pr => pr.number === issue_number);
        return { data: { ...listed, ...currentPrs[issue_number] } };
      },
      createComment: async params => { calls.posts.push(params); return { data: {} }; },
    }, pulls: {
      listReviews: async params => {
        assert.deepEqual(params, { owner: 'FasterXML', repo: 'jackson-databind', pull_number: params.pull_number, per_page: 100 });
        return { data: reviews[params.pull_number] || [] };
      },
    } },
  };
  const core = {
    info: msg => calls.logs.push(msg),
    warning: msg => calls.logs.push(`WARNING ${msg}`),
    setFailed: msg => { calls.failed = msg; },
    summary: {
      addHeading() { return this; }, addRaw() { return this; }, addTable() { return this; },
      async write() { calls.summaryWritten = true; return this; },
    },
  };
  const fakeProcess = { env: {
    DRY_RUN: 'false', SIMULATE_NOW: '', INTERVAL_DAYS: '3',
    MAX_REMINDERS: '0', SKIP_LABELED_BEFORE: '', ...env,
  } };
  let clock = '2026-11-07T00:00:00Z';
  class FixtureDate extends Date {
    constructor(...args) {
      super(...(args.length ? args : [clock]));
    }
  }
  const runScript = new Function('github', 'context', 'core', 'process', 'Date', `return (async () => {\n${script}\n})();`);
  return {
    calls,
    run: () => runScript(github, { repo: { owner: 'FasterXML', repo: 'jackson-databind' } }, core, fakeProcess, FixtureDate),
    setClock: value => { clock = value; },
    addComment: (number, c) => { (comments[number] ||= []).push(c); },
  };
}

const tests = [];
function test(name, fn) { tests.push([name, fn]); }

test('chooses request, wait, remind, completed skip, and locked skip correctly', async () => {
  const h = makeHarness({
    prs: [pr(1), pr(2), pr(3), pr(4, { labels: ['cla-needed', 'cla-received'] }), pr(5, { locked: true })],
    comments: {
      2: [comment('Please sign the CLA.', { created: '2026-11-06T12:00:00Z' })],
      3: [comment('Please sign the CLA.', { created: '2026-11-03T00:00:00Z' })],
    },
    events: { 1: [], 2: [], 3: [] },
  });
  await h.run();
  assert.deepEqual(h.calls.posts.map(p => p.issue_number), [1, 3]);
  assert(h.calls.logs.some(x => x.includes('wait') && x.includes('#2')));
  assert(h.calls.logs.some(x => x.includes('skip') && x.includes('#4')));
  assert(h.calls.logs.some(x => x.includes('skip') && x.includes('#5')));
});

test('posts exact request and reminder text to the calling repository', async () => {
  const h = makeHarness({ prs: [pr(11), pr(12)], comments: { 12: [comment('Please sign the CLA.', { created: '2026-11-03T00:00:00Z' })] } });
  await h.run();
  assert.equal(h.calls.posts[0].body, REQUEST);
  assert.equal(h.calls.posts[1].body, REMINDER);
  for (const post of h.calls.posts) {
    assert.equal(post.owner, 'FasterXML');
    assert.equal(post.repo, 'jackson-databind');
  }
});

test('does not fetch comments or events for completed or locked PRs', async () => {
  const h = makeHarness({ prs: [pr(20, { labels: ['cla-needed', 'cla-received'] }), pr(21, { locked: true })] });
  await h.run();
  assert.deepEqual(h.calls.commentsFetched, []);
  assert.deepEqual(h.calls.eventsFetched, []);
});

test('issues, closed PRs, and unlabeled PRs are ignored', async () => {
  const h = makeHarness({ prs: [
    { number: 30, labels: [{ name: 'cla-needed' }] },
    { ...pr(31), state: 'closed' },
    { ...pr(32), labels: [{ name: 'unrelated' }] },
  ] });
  await h.run();
  assert.deepEqual(h.calls.commentsFetched, []);
  assert.deepEqual(h.calls.posts, []);
});

test('dry run reports a request without posting', async () => {
  const h = makeHarness({ prs: [pr(40)], env: { DRY_RUN: 'true' } });
  await h.run();
  assert.deepEqual(h.calls.posts, []);
  assert(h.calls.logs.some(x => x.includes('request (dry run)')));
});

test('an immediate second run is idempotent', async () => {
  const h = makeHarness({ prs: [pr(50)], comments: {} });
  await h.run();
  h.addComment(50, comment(REQUEST, { type: 'Bot', login: 'github-actions[bot]', created: '2026-11-07T00:00:00Z' }));
  h.calls.posts.length = 0;
  await h.run();
  assert.equal(h.calls.posts.length, 0);
  assert(h.calls.logs.some(x => x.includes('wait') && x.includes('#50')));
});

test('max-reminders stops after the configured count', async () => {
  const h = makeHarness({
    prs: [pr(60)],
    comments: { 60: [comment(REQUEST, { type: 'Bot', login: 'github-actions[bot]' }), comment(REMINDER, { type: 'Bot', login: 'github-actions[bot]', created: '2026-11-03T00:00:00Z' })] },
    env: { MAX_REMINDERS: '1' },
  });
  await h.run();
  assert.equal(h.calls.posts.length, 0);
  assert(h.calls.logs.some(x => x.includes('done') && x.includes('reached max-reminders=1')));
});

test('a human-authored marked reminder counts toward max-reminders', async () => {
  const h = makeHarness({
    prs: [pr(61)],
    comments: { 61: [comment(REQUEST, { login: 'maintainer', authorAssociation: 'MEMBER' }), comment(REMINDER, { login: 'maintainer', authorAssociation: 'MEMBER', created: '2026-11-03T00:00:00Z' })] },
    env: { MAX_REMINDERS: '1' },
  });
  await h.run();
  assert.equal(h.calls.posts.length, 0);
  assert(h.calls.logs.some(x => x.includes('done') && x.includes('reached max-reminders=1')));
});

test('a contributor cannot spoof workflow markers', async () => {
  const h = makeHarness({ prs: [pr(611)], comments: {
    611: [comment(REQUEST, { login: 'author-611', authorAssociation: 'CONTRIBUTOR' })],
  } });
  await h.run();
  assert.equal(h.calls.posts.length, 1);
  assert.equal(h.calls.posts[0].body, REQUEST);
});

test('CLA matching handles negative statements, request forms, and the full name', async () => {
  const h = makeHarness({ prs: [pr(62), pr(63), pr(64), pr(65), pr(66), pr(67), pr(68)], comments: {
    62: [comment('CLA received, thanks!', { login: 'reviewer' })],
    63: [comment('Could you please sign the cla?', { login: 'reviewer', created: '2026-11-06T00:00:00Z' })],
    64: [comment('Please complete the Contributor License Agreement.', { login: 'reviewer', created: '2026-11-06T00:00:00Z' })],
    65: [comment('The CLA is required before merge.', { login: 'reviewer', created: '2026-11-06T00:00:00Z' })],
    66: [comment('I signed the CLA; please update the changelog.', { login: 'reviewer' })],
    67: [comment("I don't need a CLA.", { login: 'reviewer' })],
    68: [comment('No need for a CLA.', { login: 'reviewer' })],
  } });
  await h.run();
  assert.deepEqual(h.calls.posts.map(p => p.issue_number), [62, 66, 67, 68]);
  assert(h.calls.logs.some(x => x.includes('wait') && x.includes('#63')));
  assert(h.calls.logs.some(x => x.includes('wait') && x.includes('#64')));
  assert(h.calls.logs.some(x => x.includes('wait') && x.includes('#65')));
});

test('a request from before re-labeling does not suppress a fresh request', async () => {
  const h = makeHarness({ prs: [pr(65)], comments: {
    65: [comment('Please sign the CLA.', { login: 'reviewer', created: '2026-11-02T00:00:00Z' })],
  }, events: { 65: [{ event: 'labeled', label: { name: 'cla-needed' }, created_at: '2026-11-05T00:00:00Z' }] } });
  await h.run();
  assert.deepEqual(h.calls.posts.map(p => p.issue_number), [65]);
  assert.equal(h.calls.posts[0].body, REQUEST);
});

test('cutoff uses the first cla-needed label event after later re-labeling', async () => {
  const h = makeHarness({ prs: [pr(66)], events: { 66: [
    { event: 'labeled', label: { name: 'cla-needed' }, created_at: '2026-10-01T00:00:00Z' },
    { event: 'unlabeled', label: { name: 'cla-needed' }, created_at: '2026-10-10T00:00:00Z' },
    { event: 'labeled', label: { name: 'cla-needed' }, created_at: '2026-11-06T00:00:00Z' },
  ] }, env: { SKIP_LABELED_BEFORE: '2026-11-01' } });
  await h.run();
  assert.equal(h.calls.posts.length, 0);
  assert(h.calls.logs.some(x => x.includes('skip') && x.includes('first labeled 2026-10-01')));
});

test('invalid or too-small intervals fail before listing PRs', async () => {
  for (const interval of ['not-a-number', '0', '-1', '0.001']) {
    const h = makeHarness({ prs: [pr(67)], env: { INTERVAL_DAYS: interval } });
    await h.run();
    assert.match(h.calls.failed, /interval-days must be a finite number greater than 1 hour/);
    assert.equal(h.calls.list, 0);
    assert.equal(h.calls.posts.length, 0);
  }
});

test('invalid reminder limits fail before listing PRs', async () => {
  for (const limit of ['-1', '1.5', 'not-a-number']) {
    const h = makeHarness({ prs: [pr(671)], env: { MAX_REMINDERS: limit } });
    await h.run();
    assert.match(h.calls.failed, /max-reminders must be a non-negative integer/);
    assert.equal(h.calls.list, 0);
    assert.equal(h.calls.posts.length, 0);
  }
});

test('label events are fetched only when the cutoff or a CLA request needs them', async () => {
  const h = makeHarness({ prs: [pr(68), pr(69)], comments: {
    69: [comment('Please sign the CLA.', { created: '2026-11-06T00:00:00Z' })],
  } });
  await h.run();
  assert.deepEqual(h.calls.eventsFetched, [69]);
});

test('skip-labeled-before ignores older label events', async () => {
  const h = makeHarness({ prs: [pr(70)], events: { 70: [{ event: 'labeled', label: { name: 'cla-needed' }, created_at: '2026-11-06T23:59:59Z' }] }, env: { SKIP_LABELED_BEFORE: '2026-11-07' } });
  await h.run();
  assert.equal(h.calls.posts.length, 0);
  assert(h.calls.logs.some(x => x.includes('skip') && x.includes('before skip-labeled-before')));
});

test('invalid cutoff fails before posting', async () => {
  for (const cutoff of ['not-a-date', '2026-11-31', '2026-02-29', '2026-11-07T00:00:00Z', '2026-1-01']) {
    const h = makeHarness({ prs: [pr(80)], env: { SKIP_LABELED_BEFORE: cutoff } });
    await h.run();
    assert.match(h.calls.failed, /skip-labeled-before is not a valid date/, cutoff);
    assert.equal(h.calls.list, 0);
    assert.equal(h.calls.posts.length, 0);
  }
});

test('a valid leap day is accepted and the cutoff includes midnight UTC', async () => {
  const h = makeHarness({
    prs: [pr(81, { created: '2024-02-29T00:00:00Z' })],
    events: { 81: [{ event: 'labeled', label: { name: 'cla-needed' }, created_at: '2024-02-29T00:00:00Z' }] },
    env: { SKIP_LABELED_BEFORE: '2024-02-29' },
  });
  await h.run();
  assert.equal(h.calls.failed, false);
  assert.deepEqual(h.calls.posts.map(post => post.issue_number), [81]);
});

test('one failing PR does not stop processing other PRs', async () => {
  const h = makeHarness({ prs: [pr(90), pr(91)], failures: { comments: 90 } });
  await h.run();
  assert(h.calls.failed.includes('1 PR(s) failed'));
  assert.equal(h.calls.posts.length, 1);
  assert.equal(h.calls.posts[0].issue_number, 91);
});

test('invalid simulated time fails before listing PRs', async () => {
  const h = makeHarness({ prs: [pr(100)], env: { DRY_RUN: 'true', SIMULATE_NOW: 'bad time' } });
  await h.run();
  assert.match(h.calls.failed, /simulate-now is not a valid timestamp/);
  assert.equal(h.calls.list, 0);
});

test('14-day timeline sends one request and reminders at 3-day intervals', async () => {
  const comments = { 110: [] };
  const h = makeHarness({ prs: [pr(110, { created: '2026-11-01T00:00:00Z' })], comments });
  for (let day = 0; day <= 14; day++) {
    const timestamp = new Date(Date.parse('2026-11-01T00:00:00Z') + day * DAY).toISOString();
    h.setClock(timestamp);
    h.calls.posts.length = 0;
    await h.run();
    for (const item of h.calls.posts) {
      comments[110].push(comment(item.body, { type: 'Bot', login: 'github-actions[bot]', created: timestamp }));
    }
  }
  const markers = comments[110].map(c => c.body.includes(REMINDER_MARK) ? 'reminder' : 'request');
  assert.deepEqual(markers, ['request', 'reminder', 'reminder', 'reminder', 'reminder']);
});

test('PR authors, unrelated words and other bots do not count as asks', async () => {
  const h = makeHarness({ prs: [pr(120), pr(121), pr(122)], comments: {
    120: [comment('I will sign the CLA.', { login: 'author-120' })],
    121: [comment('CLASS is unrelated.', { login: 'reviewer' })],
    122: [comment('CLA bot ping', { type: 'Bot', login: 'other-bot' })],
  } });
  await h.run();
  assert.deepEqual(h.calls.posts.map(p => p.issue_number), [120, 121, 122]);
});

test('explicit maintainer requests count even alongside references to missing or future receipt', async () => {
  const examples = [
    'We cannot merge this without a CLA, please sign it.',
    'Please send the CLA; once the CLA is received I can merge.',
    'Before we can merge, we’d need a CLA.',
    'Please sign the [CLA](https://example.test/cla).',
  ];
  for (const body of examples) {
    for (const priorBotAsk of [false, true]) {
      const comments = [comment(body, { created: '2026-11-06T23:00:00Z' })];
      if (priorBotAsk) {
        comments.unshift(comment(REQUEST, { type: 'Bot', login: 'github-actions[bot]' }));
      }
      const h = makeHarness({ prs: [pr(130)], comments: { 130: comments } });
      await h.run();
      assert.equal(h.calls.posts.length, 0, body);
      assert(h.calls.logs.some(x => x.startsWith('wait') && x.includes('#130')));
    }
  }
});

test('outsider comments cannot suppress an initial request or delay a due reminder', async () => {
  const outsider = comment('You need to sign the CLA.', {
    authorAssociation: 'CONTRIBUTOR', created: '2026-11-06T23:00:00Z',
  });
  const ambiguous = comment('Do we need a CLA here? I need your feedback on the CLA.', {
    created: '2026-11-06T23:00:00Z',
  });
  const h = makeHarness({
    prs: [pr(131), pr(132), pr(133)],
    comments: {
      131: [outsider],
      132: [comment(REQUEST, { type: 'Bot', login: 'github-actions[bot]' }), outsider],
      133: [ambiguous],
    },
  });
  await h.run();
  assert.deepEqual(h.calls.posts.map(p => p.body), [REQUEST, REMINDER, REQUEST]);
});

test('the pre-label grace window only applies to manual asks in the current cycle', async () => {
  const labeled = { event: 'labeled', label: { name: 'cla-needed' }, created_at: '2026-11-06T23:00:00Z' };
  const earlierLabel = { ...labeled, created_at: '2026-11-06T19:00:00Z' };
  const removed = { ...labeled, event: 'unlabeled', created_at: '2026-11-06T22:55:00Z' };
  const ask = created => comment('Please sign the CLA.', { created });
  const h = makeHarness({
    prs: [pr(140), pr(141), pr(142), pr(143), pr(144)],
    comments: {
      140: [ask('2026-11-06T22:59:59Z')],
      141: [ask('2026-11-06T22:00:00Z')],
      142: [ask('2026-11-06T21:59:59Z')],
      143: [ask('2026-11-06T22:54:59Z')],
      144: [comment(REQUEST, { created: '2026-11-06T22:59:59Z' })],
    },
    events: {
      140: [labeled], 141: [labeled], 142: [labeled],
      143: [earlierLabel, removed, labeled], 144: [earlierLabel, removed, labeled],
    },
  });
  await h.run();
  assert.deepEqual(h.calls.posts.map(p => p.issue_number), [142, 143, 144]);
});

test('a missing PR author does not prevent later PRs or the summary from being processed', async () => {
  const h = makeHarness({ prs: [{ ...pr(150), user: null }, pr(151)] });
  await h.run();
  assert.deepEqual(h.calls.posts.map(p => p.issue_number), [150, 151]);
  assert(h.calls.logs.some(x => x.includes('#150 (unknown author)')));
  assert.equal(h.calls.failed, false);
  assert.equal(h.calls.summaryWritten, true);
});

test('simulated time cannot post and is still usable for a reminder preview', async () => {
  const env = { SIMULATE_NOW: '2026-12-01T00:00:00Z' };
  const live = makeHarness({ prs: [pr(160)], env });
  await live.run();
  assert.match(live.calls.failed, /simulate-now requires dry-run/);
  assert.equal(live.calls.list, 0);
  assert.deepEqual(live.calls.posts, []);

  const preview = makeHarness({
    prs: [pr(160)],
    comments: { 160: [comment(REQUEST, { type: 'Bot', login: 'github-actions[bot]' })] },
    env: { ...env, DRY_RUN: 'true' },
  });
  await preview.run();
  assert.equal(preview.calls.failed, false);
  assert.deepEqual(preview.calls.posts, []);
  assert(preview.calls.logs.some(x => x.startsWith('remind (dry run)')));
});

test('live posts are skipped when a PR becomes ineligible after listing', async () => {
  const changes = [
    { state: 'closed' },
    { labels: [] },
    { labels: [{ name: 'cla-needed' }, { name: 'cla-received' }] },
    { locked: true },
  ];
  for (const change of changes) {
    const h = makeHarness({
      prs: [pr(170), pr(171), pr(172)],
      currentPrs: { 170: change, 171: change },
      comments: { 171: [comment(REQUEST, { type: 'Bot', login: 'github-actions[bot]' })] },
    });
    await h.run();
    assert.deepEqual(h.calls.posts.map(post => post.issue_number), [172]);
    assert(h.calls.logs.some(line => line.startsWith('skip') && line.includes('#170')));
    assert(h.calls.logs.some(line => line.startsWith('skip') && line.includes('#171')));
    assert.equal(h.calls.failed, false);
  }
});

test('submitted maintainer review summaries prevent duplicate requests and delay reminders', async () => {
  const beforeLabel = '2026-11-06T22:45:00Z';
  const review = {
    ...comment('Please sign the CLA.'),
    created_at: '2026-11-01T00:00:00Z',
    submitted_at: beforeLabel,
    state: 'COMMENTED',
  };
  const h = makeHarness({
    prs: [pr(180), pr(181), pr(182), pr(183), pr(184), pr(185)],
    reviews: {
      180: [review],
      181: [review],
      182: [{ ...review, state: 'PENDING', submitted_at: undefined }],
      183: [{ ...review, author_association: 'CONTRIBUTOR' }],
      184: [{ ...review, user: { type: 'User', login: 'author-184' } }],
      185: [{ ...review, submitted_at: '2026-11-06T21:59:59Z' }],
    },
    events: Object.fromEntries([180, 185].map(number => [number, [{
      event: 'labeled', label: { name: 'cla-needed' }, created_at: '2026-11-06T23:00:00Z',
    }]])),
    comments: { 181: [comment(REQUEST, { type: 'Bot', login: 'github-actions[bot]' })] },
  });
  await h.run();
  assert.equal(h.calls.failed, false);
  assert.deepEqual(h.calls.posts.map(post => post.issue_number), [182, 183, 184, 185]);
  assert(h.calls.logs.some(line => line.startsWith('wait') && line.includes('#180')));
  assert(h.calls.logs.some(line => line.startsWith('wait') && line.includes('#181')));
});

let passed = 0;
for (const [name, fn] of tests) {
  try {
    await fn();
    passed++;
    console.log(`PASS ${name}`);
  } catch (err) {
    console.error(`FAIL ${name}\n${err.stack}`);
  }
}
console.log(`${passed}/${tests.length} passed`);
if (passed !== tests.length) process.exitCode = 1;
