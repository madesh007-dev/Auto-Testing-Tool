import React, { useState } from 'react';
import { apiClient } from '../../api/client';
import { downloadBlob } from '../../api/download';
import { Button } from '../common/Button';

export const ReportDownloadButton = ({ run, compact = false }) => {
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const completed = !['PENDING', 'RUNNING'].includes(run.status);

  const downloadReport = async () => {
    setError('');
    setLoading(true);
    try {
      const report = await apiClient.getBlob(`/test-runs/${run.id}/report`, 'text/html, application/json');
      downloadBlob(report, `autotest-report-run-${run.id}.html`);
    } catch (err) {
      setError(err.message || 'Failed to download the HTML report.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="space-y-1">
      <Button
        variant="secondary"
        loading={loading}
        disabled={!completed}
        onClick={downloadReport}
      >
        {compact ? 'Download report' : 'Download HTML report'}
      </Button>
      {error && <p role="alert" className="max-w-48 text-xs text-rose-300">{error}</p>}
    </div>
  );
};
