import React from 'react';
import { Badge } from '../common/Badge';
import { Button } from '../common/Button';

const metricCards = [
  ['Projects', 'totalProjects'],
  ['Test cases', 'totalTestCases'],
  ['Test runs', 'totalTestRuns'],
  ['Executed tests', 'totalExecutedTests'],
  ['Passed', 'passedTests'],
  ['Failed', 'failedTests'],
  ['Errors', 'errorTests'],
  ['Pass rate', 'passRate', true],
];

const formatDate = (value) => value ? new Date(value).toLocaleString() : '—';

export const Dashboard = ({ metrics, loading, error, onRefresh, onSelectRun, onShowHistory }) => (
  <section className="space-y-6" aria-labelledby="dashboard-heading">
    <div className="flex flex-col sm:flex-row justify-between gap-3 border-b border-zinc-800 pb-5">
      <div>
        <h1 id="dashboard-heading" className="text-2xl font-bold text-white">Dashboard</h1>
        <p className="mt-1 text-sm text-zinc-400">Metrics from your persisted test executions.</p>
      </div>
      <div className="flex gap-2">
        <Button variant="secondary" onClick={onShowHistory}>Execution History</Button>
        <Button variant="secondary" loading={loading} onClick={onRefresh}>Refresh metrics</Button>
      </div>
    </div>

    {error && <p role="alert" className="rounded-lg border border-rose-500/20 bg-rose-500/10 p-3 text-sm text-rose-300">{error}</p>}
    {loading && !metrics && <p role="status" className="text-sm text-zinc-400">Loading dashboard metrics…</p>}

    {metrics && (
      <>
        <div className="grid grid-cols-2 gap-3 sm:grid-cols-4">
          {metricCards.map(([label, key, percent]) => (
            <div key={key} className="rounded-xl border border-zinc-800 bg-zinc-900/70 p-4">
              <p className="text-xs text-zinc-400">{label}</p>
              <p className="mt-2 text-2xl font-semibold text-white">
                {percent ? `${metrics[key].toFixed(1)}%` : metrics[key]}
              </p>
            </div>
          ))}
        </div>

        <div className="grid gap-6 lg:grid-cols-2">
          <section className="rounded-xl border border-zinc-800 bg-zinc-900/60 p-5">
            <h2 className="mb-4 text-base font-semibold text-white">Execution trend · 14 days</h2>
            {metrics.executionTrend.some((day) => day.executedTests > 0) ? (
              <div className="flex h-44 items-end gap-1" aria-label="Daily executed tests">
                {metrics.executionTrend.map((day) => {
                  const max = Math.max(...metrics.executionTrend.map((item) => item.executedTests), 1);
                  return (
                    <div key={day.date} className="flex h-full min-w-0 flex-1 flex-col justify-end" title={`${day.date}: ${day.executedTests} executed`}>
                      <div
                        className="w-full rounded-t bg-indigo-500"
                        style={{ height: `${Math.max(day.executedTests / max * 100, day.executedTests ? 6 : 0)}%` }}
                      />
                      <span className="mt-1 truncate text-center text-[9px] text-zinc-500">
                        {new Date(`${day.date}T00:00:00`).toLocaleDateString(undefined, { day: 'numeric' })}
                      </span>
                    </div>
                  );
                })}
              </div>
            ) : (
              <p className="py-12 text-center text-sm text-zinc-500">No completed test executions in the last 14 days.</p>
            )}
          </section>

          <section className="rounded-xl border border-zinc-800 bg-zinc-900/60 p-5">
            <div className="mb-4 flex items-center justify-between gap-2">
              <h2 className="text-base font-semibold text-white">Recent executions</h2>
              <button onClick={onShowHistory} className="text-xs text-indigo-400 hover:text-indigo-300">View all</button>
            </div>
            {metrics.recentExecutions.length ? (
              <div className="space-y-2">
                {metrics.recentExecutions.map((run) => (
                  <button
                    key={run.id}
                    onClick={() => onSelectRun(run.id)}
                    className="flex w-full items-center justify-between gap-3 rounded-lg border border-zinc-800 p-3 text-left hover:border-indigo-500/50"
                  >
                    <span className="min-w-0">
                      <span className="block truncate text-sm font-medium text-white">{run.projectName}</span>
                      <span className="text-xs text-zinc-500">Run #{run.id} · {formatDate(run.startedAt)}</span>
                    </span>
                    <Badge variant={run.status === 'PASSED' ? 'success' : run.status === 'FAILED' ? 'danger' : 'warning'}>
                      {run.status}
                    </Badge>
                  </button>
                ))}
              </div>
            ) : (
              <p className="py-12 text-center text-sm text-zinc-500">No test runs yet.</p>
            )}
          </section>
        </div>
      </>
    )}
  </section>
);
