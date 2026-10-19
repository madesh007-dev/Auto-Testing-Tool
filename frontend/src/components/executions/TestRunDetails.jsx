import React, { useEffect, useRef, useState } from 'react';
import { Badge } from '../common/Badge';
import { Button } from '../common/Button';
import { apiClient } from '../../api/client';
import { ReportDownloadButton } from './ReportDownloadButton';

const formatDate = (value) => value ? new Date(value).toLocaleString() : '—';
const statusVariant = (status) => status === 'PASSED' ? 'success' : status === 'FAILED' ? 'danger' : status === 'ERROR' ? 'warning' : 'neutral';

export const TestRunDetails = ({ run, results, loading, error, onBack }) => {
  const [screenshots, setScreenshots] = useState({});
  const [screenshotError, setScreenshotError] = useState({});
  const [loadingScreenshots, setLoadingScreenshots] = useState({});
  const screenshotUrls = useRef(new Set());

  useEffect(() => () => screenshotUrls.current.forEach((url) => URL.revokeObjectURL(url)), []);

  const loadScreenshot = async (result) => {
    if (loadingScreenshots[result.id]) return;
    setLoadingScreenshots((previous) => ({ ...previous, [result.id]: true }));
    setScreenshotError((previous) => ({ ...previous, [result.id]: '' }));
    try {
      const image = await apiClient.getBlob(`/test-results/${result.id}/screenshot`);
      const url = URL.createObjectURL(image);
      setScreenshots((previous) => {
        if (previous[result.id]) {
          URL.revokeObjectURL(previous[result.id]);
          screenshotUrls.current.delete(previous[result.id]);
        }
        screenshotUrls.current.add(url);
        return { ...previous, [result.id]: url };
      });
    } catch (err) {
      setScreenshotError((previous) => ({ ...previous, [result.id]: err.message || 'Screenshot unavailable.' }));
    } finally {
      setLoadingScreenshots((previous) => ({ ...previous, [result.id]: false }));
    }
  };

  if (loading) return <p role="status" className="text-sm text-zinc-400">Loading test run…</p>;
  if (error || !run) return <section><Button variant="ghost" onClick={onBack}>← Back</Button><p role="alert" className="mt-4 text-sm text-rose-300">{error || 'Test run not found.'}</p></section>;

  const duration = run.durationMs > 0 ? `${run.durationMs} ms` : '—';
  const completed = run.passedTests + run.failedTests + run.errorTests;
  const passRate = completed ? run.passedTests * 100 / completed : 0;

  return (
    <section className="space-y-5" aria-labelledby="run-details-heading">
      <div className="flex flex-wrap items-center justify-between gap-3 border-b border-zinc-800 pb-5">
        <div className="flex items-center gap-3">
          <Button variant="ghost" className="p-2" onClick={onBack}>←</Button>
          <div>
            <h1 id="run-details-heading" className="text-2xl font-bold text-white">Test Run #{run.id}</h1>
            <p className="mt-1 text-sm text-zinc-400">{run.projectName}</p>
          </div>
        </div>
        <ReportDownloadButton run={run} />
      </div>

      <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
        {[
          ['Status', <Badge key="run-status" variant={statusVariant(run.status)}>{run.status}</Badge>],
          ['Started', formatDate(run.startedAt)],
          ['Completed', formatDate(run.completedAt)],
          ['Duration', duration],
          ['Passed', run.passedTests],
          ['Failed', run.failedTests],
          ['Errors', run.errorTests],
          ['Skipped', run.skippedTests],
          ['Pass rate', `${passRate.toFixed(1)}%`],
          ['Triggered by', run.triggeredByUsername || '—'],
        ].map(([label, value]) => (
          <div key={label} className="rounded-xl border border-zinc-800 bg-zinc-900/60 p-4">
            <p className="text-xs text-zinc-500">{label}</p>
            <div className="mt-2 text-sm font-medium text-zinc-100">{value}</div>
          </div>
        ))}
      </div>

      <div className="space-y-3">
        <h2 className="text-lg font-semibold text-white">Test results</h2>
        {!results.length && <p className="rounded-xl border border-zinc-800 p-6 text-center text-sm text-zinc-500">No test results are stored for this run.</p>}
        {results.map((result) => (
          <article key={result.id} className="space-y-3 rounded-xl border border-zinc-800 bg-zinc-900/60 p-5">
            <div className="flex flex-wrap items-center justify-between gap-2">
              <div>
                <h3 className="font-medium text-white">{result.testCaseName}</h3>
                <p className="mt-1 text-xs text-zinc-500">{result.testType} · {result.durationMs} ms</p>
              </div>
              <Badge variant={statusVariant(result.status)}>{result.status}</Badge>
            </div>
            <div className="grid gap-2 text-xs text-zinc-400 sm:grid-cols-2">
              <p>Started: {formatDate(result.startedAt)}</p>
              <p>Completed: {formatDate(result.completedAt)}</p>
            </div>
            {result.errorMessage && <p className="rounded-lg border border-rose-500/20 bg-rose-500/10 p-3 text-sm text-rose-300">{result.errorMessage}</p>}
            {result.executionLog && (
              <details>
                <summary className="cursor-pointer text-xs text-indigo-300">Execution log</summary>
                <pre className="mt-2 max-h-80 overflow-auto whitespace-pre-wrap break-words rounded-lg bg-zinc-950 p-3 text-xs text-zinc-300">{result.executionLog}</pre>
              </details>
            )}
            {result.screenshotUrl && (
              <div className="space-y-2">
                {!screenshots[result.id] && <Button variant="secondary" loading={loadingScreenshots[result.id]} onClick={() => loadScreenshot(result)}>View failure screenshot</Button>}
                {screenshotError[result.id] && <p role="alert" className="text-xs text-rose-300">{screenshotError[result.id]}</p>}
                {screenshots[result.id] && <img src={screenshots[result.id]} alt={`Screenshot for ${result.testCaseName}`} className="max-h-[32rem] rounded-lg border border-zinc-700 object-contain" />}
              </div>
            )}
          </article>
        ))}
      </div>
    </section>
  );
};
