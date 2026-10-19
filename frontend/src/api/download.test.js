import assert from 'node:assert/strict';
import { test } from 'node:test';
import { downloadBlob } from './download.js';

test('downloads the report blob with the requested filename and revokes its URL later', () => {
  const originalCreateObjectURL = URL.createObjectURL;
  const originalRevokeObjectURL = URL.revokeObjectURL;
  const originalDocument = globalThis.document;
  const originalWindow = globalThis.window;
  const calls = [];
  let scheduledCleanup;
  const anchor = {
    style: {},
    click: () => calls.push('click'),
    remove: () => calls.push('remove'),
  };

  URL.createObjectURL = (blob) => {
    calls.push(['create', blob.type]);
    return 'blob:report';
  };
  URL.revokeObjectURL = (url) => calls.push(['revoke', url]);
  globalThis.document = {
    body: {
      appendChild: (element) => calls.push(['append', element]),
    },
    createElement: (tag) => {
      calls.push(['element', tag]);
      return anchor;
    },
  };
  globalThis.window = {
    setTimeout: (callback, delay) => {
      scheduledCleanup = callback;
      calls.push(['timeout', delay]);
    },
  };

  try {
    const reportBlob = new Blob(['<html>report</html>'], { type: 'text/html' });
    downloadBlob(reportBlob, 'autotest-report-run-12.html');

    assert.equal(anchor.href, 'blob:report');
    assert.equal(anchor.download, 'autotest-report-run-12.html');
    assert.deepEqual(calls.map((call) => Array.isArray(call) ? call[0] : call), [
      'create',
      'element',
      'append',
      'click',
      'remove',
      'timeout',
    ]);
    assert.equal(calls.find((call) => Array.isArray(call) && call[0] === 'timeout')[1], 1000);

    scheduledCleanup();
    assert.deepEqual(calls.at(-1), ['revoke', 'blob:report']);
  } finally {
    URL.createObjectURL = originalCreateObjectURL;
    URL.revokeObjectURL = originalRevokeObjectURL;
    globalThis.document = originalDocument;
    globalThis.window = originalWindow;
  }
});
