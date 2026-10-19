import React, { useState } from 'react';
import { Input } from '../common/Input';
import { Button } from '../common/Button';
import { apiClient } from '../../api/client';
import { Plus, Trash2, X, AlertCircle, Code, Globe, PlayCircle } from 'lucide-react';

export const CreateTestCaseModal = ({ isOpen, onClose, projectId, onTestCaseCreated }) => {
  const [name, setName] = useState('');
  const [description, setDescription] = useState('');
  const [testType, setTestType] = useState('UI_TEST');

  // UI Test actions state
  const [uiActions, setUiActions] = useState([
    { type: 'NAVIGATE', value: '/login', selector: '' },
    { type: 'ASSERT_VISIBLE', value: '', selector: "[data-testid='login-form']" },
  ]);

  // API Test state
  const [apiMethod, setApiMethod] = useState('GET');
  const [apiEndpoint, setApiEndpoint] = useState('/api/v1/health');
  const [apiBaseUrl, setApiBaseUrl] = useState('');
  const [apiExpectedStatus, setApiExpectedStatus] = useState(200);
  const [apiRequestBody, setApiRequestBody] = useState('');

  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  if (!isOpen) return null;

  const handleAddUiAction = () => {
    setUiActions((prev) => [...prev, { type: 'CLICK', selector: '', value: '' }]);
  };

  const handleUpdateUiAction = (index, field, val) => {
    setUiActions((prev) => {
      const copy = [...prev];
      copy[index] = { ...copy[index], [field]: val };
      return copy;
    });
  };

  const handleRemoveUiAction = (index) => {
    setUiActions((prev) => prev.filter((_, i) => i !== index));
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);
    setError(null);

    let configJson = '';
    if (testType === 'UI_TEST') {
      configJson = JSON.stringify({
        actions: uiActions.map((a) => ({
          type: a.type,
          selector: a.selector || undefined,
          value: a.value || undefined,
        })),
      });
    } else {
      configJson = JSON.stringify({
        method: apiMethod,
        endpoint: apiEndpoint,
        apiBaseUrl: apiBaseUrl || undefined,
        expectedStatus: Number(apiExpectedStatus),
        requestBody: apiRequestBody ? apiRequestBody : undefined,
      });
    }

    try {
      const newTestCase = await apiClient.post(`/projects/${projectId}/test-cases`, {
        name,
        description,
        testType,
        configuration: configJson,
      });

      onTestCaseCreated?.(newTestCase);
      onClose();
    } catch (err) {
      setError(err.message || 'Failed to create test case');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-zinc-950/80 backdrop-blur-sm">
      <div className="bg-zinc-900 border border-zinc-800 rounded-2xl w-full max-w-2xl p-6 shadow-2xl relative max-h-[90vh] overflow-y-auto animate-in fade-in zoom-in-95 duration-200">
        <button
          onClick={onClose}
          className="absolute top-4 right-4 text-zinc-500 hover:text-zinc-300 cursor-pointer"
        >
          <X className="h-5 w-5" />
        </button>

        <div className="flex items-center gap-3 mb-6">
          <div className="h-10 w-10 rounded-xl bg-gradient-to-tr from-indigo-600 to-violet-500 flex items-center justify-center shadow-lg shadow-indigo-500/20">
            <PlayCircle className="h-5 w-5 text-white" />
          </div>
          <div>
            <h2 className="text-xl font-bold text-white tracking-tight">Create Test Case</h2>
            <p className="text-xs text-zinc-400">Configure automated UI browser actions or API request assertions</p>
          </div>
        </div>

        {error && (
          <div className="mb-4 p-3 rounded-lg bg-rose-500/10 border border-rose-500/20 flex items-start gap-2.5">
            <AlertCircle className="h-4 w-4 text-rose-400 mt-0.5 shrink-0" />
            <p className="text-xs text-rose-300 font-medium">{error}</p>
          </div>
        )}

        <form onSubmit={handleSubmit} className="space-y-4">
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <Input
              label="Test Case Name"
              placeholder="e.g. Verify Login Flow"
              value={name}
              onChange={(e) => setName(e.target.value)}
              required
            />

            <div className="flex flex-col gap-1.5">
              <label className="text-xs font-medium text-zinc-300">Test Type</label>
              <select
                value={testType}
                onChange={(e) => setTestType(e.target.value)}
                className="w-full rounded-lg bg-zinc-900 border border-zinc-800 px-3.5 py-2 text-sm text-zinc-100 focus:outline-none focus:border-indigo-500"
              >
                <option value="UI_TEST">UI Test (Playwright Browser)</option>
                <option value="API_TEST">API Test (REST Assured)</option>
              </select>
            </div>
          </div>

          <Input
            label="Description"
            placeholder="e.g. Automates user login and checks that the dashboard displays properly"
            value={description}
            onChange={(e) => setDescription(e.target.value)}
          />

          {/* UI Test Builder */}
          {testType === 'UI_TEST' && (
            <div className="border border-zinc-800 rounded-xl p-4 bg-zinc-950/50 space-y-3">
              <div className="flex items-center justify-between">
                <span className="text-xs font-semibold uppercase tracking-wider text-zinc-400">
                  Playwright Action Sequence
                </span>
                <Button type="button" variant="secondary" onClick={handleAddUiAction} className="text-xs py-1 px-2.5">
                  <Plus className="h-3.5 w-3.5" />
                  Add Action
                </Button>
              </div>

              {uiActions.map((action, idx) => (
                <div key={idx} className="flex items-center gap-2 bg-zinc-900 p-2.5 rounded-lg border border-zinc-800 text-xs">
                  <span className="font-mono text-zinc-500 w-5 text-center shrink-0">{idx + 1}</span>

                  <select
                    value={action.type}
                    onChange={(e) => handleUpdateUiAction(idx, 'type', e.target.value)}
                    className="bg-zinc-800 text-zinc-200 rounded px-2 py-1 border border-zinc-700"
                  >
                    <option value="NAVIGATE">NAVIGATE</option>
                    <option value="CLICK">CLICK</option>
                    <option value="FILL">FILL</option>
                    <option value="ASSERT_VISIBLE">ASSERT_VISIBLE</option>
                    <option value="ASSERT_TEXT">ASSERT_TEXT</option>
                    <option value="ASSERT_URL">ASSERT_URL</option>
                    <option value="SCREENSHOT">SCREENSHOT</option>
                  </select>

                  {['CLICK', 'FILL', 'ASSERT_VISIBLE', 'ASSERT_TEXT'].includes(action.type) && (
                    <input
                      type="text"
                      placeholder="Selector (e.g. [data-testid='btn'])"
                      value={action.selector}
                      onChange={(e) => handleUpdateUiAction(idx, 'selector', e.target.value)}
                      className="bg-zinc-800 text-zinc-100 rounded px-2 py-1 flex-1 border border-zinc-700 font-mono"
                    />
                  )}

                  {['NAVIGATE', 'FILL', 'ASSERT_TEXT', 'ASSERT_URL'].includes(action.type) && (
                    <input
                      type="text"
                      placeholder={action.type === 'NAVIGATE' ? 'Path (e.g. /login)' : 'Value'}
                      value={action.value}
                      onChange={(e) => handleUpdateUiAction(idx, 'value', e.target.value)}
                      className="bg-zinc-800 text-zinc-100 rounded px-2 py-1 flex-1 border border-zinc-700 font-mono"
                    />
                  )}

                  <button
                    type="button"
                    onClick={() => handleRemoveUiAction(idx)}
                    className="text-zinc-500 hover:text-rose-400 p-1 cursor-pointer"
                  >
                    <Trash2 className="h-4 w-4" />
                  </button>
                </div>
              ))}
            </div>
          )}

          {/* API Test Builder */}
          {testType === 'API_TEST' && (
            <div className="border border-zinc-800 rounded-xl p-4 bg-zinc-950/50 space-y-3">
              <span className="text-xs font-semibold uppercase tracking-wider text-zinc-400">
                REST Assured Configuration
              </span>

              <div>
                <label className="text-xs text-zinc-400 mb-1 block">API Base URL (for relative endpoints)</label>
                <input
                  type="url"
                  value={apiBaseUrl}
                  onChange={(e) => setApiBaseUrl(e.target.value)}
                  placeholder="http://localhost:8081"
                  className="w-full rounded bg-zinc-900 border border-zinc-800 px-3 py-1.5 text-xs font-mono text-zinc-200"
                />
              </div>

              <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
                <div>
                  <label className="text-xs text-zinc-400 mb-1 block">HTTP Method</label>
                  <select
                    value={apiMethod}
                    onChange={(e) => setApiMethod(e.target.value)}
                    className="w-full rounded bg-zinc-900 border border-zinc-800 px-3 py-1.5 text-xs text-zinc-200"
                  >
                    <option value="GET">GET</option>
                    <option value="POST">POST</option>
                    <option value="PUT">PUT</option>
                    <option value="DELETE">DELETE</option>
                    <option value="PATCH">PATCH</option>
                  </select>
                </div>

                <div className="sm:col-span-2">
                  <label className="text-xs text-zinc-400 mb-1 block">Endpoint Path</label>
                  <input
                    type="text"
                    value={apiEndpoint}
                    onChange={(e) => setApiEndpoint(e.target.value)}
                    placeholder="/api/v1/resource"
                    className="w-full rounded bg-zinc-900 border border-zinc-800 px-3 py-1.5 text-xs font-mono text-zinc-200"
                    required
                  />
                </div>
              </div>

              <div>
                <label className="text-xs text-zinc-400 mb-1 block">Expected Status Code</label>
                <input
                  type="number"
                  value={apiExpectedStatus}
                  onChange={(e) => setApiExpectedStatus(e.target.value)}
                  className="w-32 rounded bg-zinc-900 border border-zinc-800 px-3 py-1.5 text-xs font-mono text-zinc-200"
                  required
                />
              </div>

              {['POST', 'PUT', 'PATCH'].includes(apiMethod) && (
                <div>
                  <label className="text-xs text-zinc-400 mb-1 block">Request Body (JSON)</label>
                  <textarea
                    rows={3}
                    value={apiRequestBody}
                    onChange={(e) => setApiRequestBody(e.target.value)}
                    placeholder='{"key": "value"}'
                    className="w-full rounded bg-zinc-900 border border-zinc-800 px-3 py-2 text-xs font-mono text-zinc-200"
                  />
                </div>
              )}
            </div>
          )}

          <div className="flex items-center justify-end gap-3 pt-3">
            <Button type="button" variant="ghost" onClick={onClose}>
              Cancel
            </Button>
            <Button type="submit" variant="primary" loading={loading}>
              Save Test Case
            </Button>
          </div>
        </form>
      </div>
    </div>
  );
};
