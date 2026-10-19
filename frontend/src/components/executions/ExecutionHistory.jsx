import React from 'react';
import { Badge } from '../common/Badge';
import { Button } from '../common/Button';
import { ReportDownloadButton } from './ReportDownloadButton';

const formatDate = (value) => value ? new Date(value).toLocaleString() : '—';

export const ExecutionHistory = ({
  page,
  projects,
  filters,
  loading,
  error,
  onFiltersChange,
  onApplyFilters,
  onPageChange,
  onSelectRun,
  onBack,
}) => (
  <section className="space-y-5" aria-labelledby="history-heading">
    <div className="flex items-center gap-3 border-b border-zinc-800 pb-5">
      <Button variant="ghost" className="p-2" onClick={onBack}>←</Button>
      <div>
        <h1 id="history-heading" className="text-2xl font-bold text-white">Execution History</h1>
        <p className="mt-1 text-sm text-zinc-400">Persisted test runs for your projects.</p>
      </div>
    </div>

    <form className="grid gap-3 rounded-xl border border-zinc-800 bg-zinc-900/60 p-4 sm:grid-cols-2 lg:grid-cols-5" onSubmit={onApplyFilters}>
      <select aria-label="Filter by project" value={filters.projectId} onChange={(event) => onFiltersChange({ ...filters, projectId: event.target.value })} className="rounded-lg border border-zinc-800 bg-zinc-950 px-3 py-2 text-sm text-zinc-200">
        <option value="">All projects</option>
        {projects.map((project) => <option key={project.id} value={project.id}>{project.name}</option>)}
      </select>
      <select aria-label="Filter by status" value={filters.status} onChange={(event) => onFiltersChange({ ...filters, status: event.target.value })} className="rounded-lg border border-zinc-800 bg-zinc-950 px-3 py-2 text-sm text-zinc-200">
        <option value="">All statuses</option>
        {['PENDING', 'RUNNING', 'PASSED', 'FAILED', 'ERROR', 'SKIPPED'].map((status) => <option key={status}>{status}</option>)}
      </select>
      <input aria-label="Started after" type="datetime-local" value={filters.from} onChange={(event) => onFiltersChange({ ...filters, from: event.target.value })} className="rounded-lg border border-zinc-800 bg-zinc-950 px-3 py-2 text-sm text-zinc-200" />
      <input aria-label="Started before" type="datetime-local" value={filters.to} onChange={(event) => onFiltersChange({ ...filters, to: event.target.value })} className="rounded-lg border border-zinc-800 bg-zinc-950 px-3 py-2 text-sm text-zinc-200" />
      <Button type="submit" variant="secondary" loading={loading}>Apply filters</Button>
    </form>

    {error && <p role="alert" className="rounded-lg border border-rose-500/20 bg-rose-500/10 p-3 text-sm text-rose-300">{error}</p>}
    {loading && <p role="status" className="text-sm text-zinc-400">Loading executions…</p>}
    {!loading && page?.content?.length === 0 && <p className="rounded-xl border border-zinc-800 p-8 text-center text-sm text-zinc-500">No executions match these filters.</p>}

    {!!page?.content?.length && (
      <>
        <div className="overflow-x-auto rounded-xl border border-zinc-800">
          <table className="w-full min-w-[980px] text-left text-sm">
            <thead className="bg-zinc-900 text-xs uppercase text-zinc-500">
              <tr>{['Run', 'Report', 'Project', 'Started', 'Completed', 'Status', 'Tests', 'Passed', 'Failed', 'Errors', 'Duration', 'Triggered by'].map((label) => <th key={label} className="px-4 py-3">{label}</th>)}</tr>
            </thead>
            <tbody className="divide-y divide-zinc-800 bg-zinc-950/40">
              {page.content.map((run) => {
                const duration = run.durationMs > 0 ? `${run.durationMs} ms` : '—';
                return (
                  <tr key={run.id} className="text-zinc-300">
                    <td className="px-4 py-3"><button className="text-indigo-400 hover:text-indigo-300" onClick={() => onSelectRun(run.id)}>#{run.id}</button></td>
                    <td className="px-4 py-3"><ReportDownloadButton run={run} compact /></td>
                    <td className="px-4 py-3">{run.projectName}</td>
                    <td className="px-4 py-3">{formatDate(run.startedAt)}</td>
                    <td className="px-4 py-3">{formatDate(run.completedAt)}</td>
                    <td className="px-4 py-3"><Badge variant={run.status === 'PASSED' ? 'success' : run.status === 'FAILED' ? 'danger' : 'warning'}>{run.status}</Badge></td>
                    <td className="px-4 py-3">{run.totalTests}</td>
                    <td className="px-4 py-3">{run.passedTests}</td>
                    <td className="px-4 py-3">{run.failedTests}</td>
                    <td className="px-4 py-3">{run.errorTests}</td>
                    <td className="px-4 py-3">{duration}</td>
                    <td className="px-4 py-3">{run.triggeredByUsername || '—'}</td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
        <div className="flex items-center justify-between text-sm text-zinc-400">
          <span>{page.totalElements} runs · Page {page.number + 1} of {Math.max(page.totalPages, 1)}</span>
          <div className="flex gap-2">
            <Button variant="secondary" disabled={page.first} onClick={() => onPageChange(page.number - 1)}>Previous</Button>
            <Button variant="secondary" disabled={page.last} onClick={() => onPageChange(page.number + 1)}>Next</Button>
          </div>
        </div>
      </>
    )}
  </section>
);
