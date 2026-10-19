import React, { useState, useEffect, useRef } from 'react';
import { useAuth } from './features/auth/AuthContext';
import { apiClient } from './api/client';
import { Badge } from './components/common/Badge';
import { Button } from './components/common/Button';
import { AuthModal } from './components/common/AuthModal';
import { CreateProjectModal } from './components/projects/CreateProjectModal';
import { CreateTestCaseModal } from './components/testcases/CreateTestCaseModal';
import { Dashboard } from './components/dashboard/Dashboard';
import { ExecutionHistory } from './components/executions/ExecutionHistory';
import { TestRunDetails } from './components/executions/TestRunDetails';
import {
  Layers,
  FolderKanban,
  Plus,
  Trash2,
  LogIn,
  LogOut,
  User as UserIcon,
  RefreshCw,
  Globe,
  ArrowLeft,
  PlayCircle,
  FileCode,
  CheckCircle2
} from 'lucide-react';

export default function App() {
  const { user, logout, isAuthenticated } = useAuth();

  const [authModalOpen, setAuthModalOpen] = useState(false);
  const [authMode, setAuthMode] = useState('login');
  const [createProjectOpen, setCreateProjectOpen] = useState(false);
  const [createTestCaseOpen, setCreateTestCaseOpen] = useState(false);

  const [health, setHealth] = useState(null);
  const [projects, setProjects] = useState([]);
  const [selectedProject, setSelectedProject] = useState(null);
  const [testCases, setTestCases] = useState([]);
  const [dashboardMetrics, setDashboardMetrics] = useState(null);
  const [loadingDashboard, setLoadingDashboard] = useState(false);
  const [dashboardError, setDashboardError] = useState(null);
  const [showHistory, setShowHistory] = useState(false);
  const [historyPage, setHistoryPage] = useState(null);
  const [historyFilters, setHistoryFilters] = useState({ projectId: '', status: '', from: '', to: '' });
  const [loadingHistory, setLoadingHistory] = useState(false);
  const [historyError, setHistoryError] = useState(null);
  const [selectedTestRunId, setSelectedTestRunId] = useState(null);
  const [selectedTestRun, setSelectedTestRun] = useState(null);
  const [selectedTestResults, setSelectedTestResults] = useState([]);
  const [loadingTestRun, setLoadingTestRun] = useState(false);
  const [testRunError, setTestRunError] = useState(null);
  const [returnToHistory, setReturnToHistory] = useState(false);

  const [loadingProjects, setLoadingProjects] = useState(false);
  const [loadingTestCases, setLoadingTestCases] = useState(false);
  const [error, setError] = useState(null);
  const [runningTestCases, setRunningTestCases] = useState({});
  const [pendingTestRuns, setPendingTestRuns] = useState({});
  const [testResults, setTestResults] = useState({});
  const [testExecutionErrors, setTestExecutionErrors] = useState({});
  const activeTestRuns = useRef(new Set());
  const startedTestRuns = useRef(new Map());

  const fetchHealth = async () => {
    try {
      const data = await apiClient.get('/health');
      setHealth(data);
    } catch (err) {
      // Ignored for header pill
    }
  };

  const fetchProjects = async () => {
    if (!isAuthenticated) return;
    setLoadingProjects(true);
    setError(null);
    try {
      const data = await apiClient.get('/projects');
      setProjects(data.content || []);
    } catch (err) {
      setError(err.message || 'Failed to load projects');
    } finally {
      setLoadingProjects(false);
    }
  };

  const fetchTestCases = async (projectId) => {
    setLoadingTestCases(true);
    try {
      const data = await apiClient.get(`/projects/${projectId}/test-cases`);
      setTestCases(data.content || []);
    } catch (err) {
      setError(err.message || 'Failed to load test cases');
    } finally {
      setLoadingTestCases(false);
    }
  };

  const fetchDashboard = async () => {
    if (!isAuthenticated) return;
    setLoadingDashboard(true);
    setDashboardError(null);
    try {
      setDashboardMetrics(await apiClient.get('/dashboard/metrics'));
    } catch (err) {
      setDashboardError(err.message || 'Failed to load dashboard metrics');
    } finally {
      setLoadingDashboard(false);
    }
  };

  const fetchHistory = async (pageNumber = 0, filters = historyFilters) => {
    setLoadingHistory(true);
    setHistoryError(null);
    const params = new URLSearchParams({ page: String(pageNumber), size: '20' });
    if (filters.projectId) params.set('projectId', filters.projectId);
    if (filters.status) params.set('status', filters.status);
    if (filters.from) params.set('from', new Date(filters.from).toISOString());
    if (filters.to) params.set('to', new Date(filters.to).toISOString());
    try {
      setHistoryPage(await apiClient.get(`/test-runs?${params.toString()}`));
    } catch (err) {
      setHistoryError(err.message || 'Failed to load execution history');
    } finally {
      setLoadingHistory(false);
    }
  };

  const handleSelectRun = async (testRunId, fromHistory = showHistory) => {
    setSelectedTestRunId(testRunId);
    setReturnToHistory(fromHistory);
    setSelectedTestRun(null);
    setSelectedTestResults([]);
    setLoadingTestRun(true);
    setTestRunError(null);
    try {
      const [run, results] = await Promise.all([
        apiClient.get(`/test-runs/${testRunId}`),
        apiClient.get(`/test-runs/${testRunId}/results`),
      ]);
      setSelectedTestRun(run);
      setSelectedTestResults(results);
    } catch (err) {
      setTestRunError(err.message || 'Failed to load test run details');
    } finally {
      setLoadingTestRun(false);
    }
  };

  const handleRunTest = async (testCaseId) => {
    if (activeTestRuns.current.has(testCaseId)) return;

    activeTestRuns.current.add(testCaseId);
    setRunningTestCases((prev) => ({ ...prev, [testCaseId]: true }));
    setTestResults((prev) => ({ ...prev, [testCaseId]: null }));
    setTestExecutionErrors((prev) => ({ ...prev, [testCaseId]: null }));

    let run = startedTestRuns.current.get(testCaseId);
    const runAlreadyStarted = Boolean(run);
    try {
      if (!run) {
        run = await apiClient.post(`/test-cases/${testCaseId}/test-runs`);
        startedTestRuns.current.set(testCaseId, run);
        setPendingTestRuns((prev) => ({ ...prev, [testCaseId]: true }));
      }

      let result;
      while (!result || result.status === 'PENDING' || result.status === 'RUNNING') {
        await new Promise((resolve) => setTimeout(resolve, 1000));
        const results = await apiClient.get(`/test-runs/${run.id}/results`);
        result = results.find((item) => item.testCaseId === testCaseId);
        if (!result) {
          throw new Error('The test run was created, but its result is not available.');
        }
      }

      setTestResults((prev) => ({ ...prev, [testCaseId]: result }));
      startedTestRuns.current.delete(testCaseId);
      setPendingTestRuns((prev) => ({ ...prev, [testCaseId]: false }));
      fetchDashboard();
    } catch (err) {
      setTestExecutionErrors((prev) => ({
        ...prev,
        [testCaseId]: runAlreadyStarted || run
          ? `Test run started, but its result could not be loaded: ${err.message || 'Unknown error'}`
          : err.message || 'Failed to start test run',
      }));
    } finally {
      activeTestRuns.current.delete(testCaseId);
      setRunningTestCases((prev) => ({ ...prev, [testCaseId]: false }));
    }
  };

  const handleSelectProject = (project) => {
    setSelectedProject(project);
    fetchTestCases(project.id);
  };

  const handleDeleteTestCase = async (testCaseId) => {
    if (!window.confirm('Are you sure you want to delete this test case?')) return;
    try {
      await apiClient.delete(`/test-cases/${testCaseId}`);
      setTestCases((prev) => prev.filter((tc) => tc.id !== testCaseId));
      fetchDashboard();
    } catch (err) {
      alert(err.message || 'Failed to delete test case');
    }
  };

  const handleDeleteProject = async (projectId) => {
    if (!window.confirm('Are you sure you want to delete this project?')) return;
    try {
      await apiClient.delete(`/projects/${projectId}`);
      setProjects((prev) => prev.filter((p) => p.id !== projectId));
      fetchDashboard();
      if (selectedProject?.id === projectId) {
        setSelectedProject(null);
      }
    } catch (err) {
      alert(err.message || 'Failed to delete project');
    }
  };

  useEffect(() => {
    fetchHealth();
  }, []);

  useEffect(() => {
    if (isAuthenticated) {
      fetchProjects();
      fetchDashboard();
    } else {
      setProjects([]);
      setSelectedProject(null);
      setTestCases([]);
      setDashboardMetrics(null);
    }
  }, [isAuthenticated]);

  useEffect(() => {
    if (showHistory && !selectedTestRunId) fetchHistory(0);
  }, [showHistory]);

  return (
    <div className="min-h-screen bg-zinc-950 text-zinc-100 flex flex-col font-sans selection:bg-indigo-500 selection:text-white">
      {/* Top Header */}
      <header className="border-b border-zinc-800/80 bg-zinc-900/60 backdrop-blur sticky top-0 z-40">
        <div className="max-w-6xl mx-auto px-6 h-16 flex items-center justify-between">
          <div className="flex items-center gap-3">
            <div className="h-9 w-9 rounded-xl bg-gradient-to-tr from-indigo-600 to-violet-500 flex items-center justify-center shadow-lg shadow-indigo-500/20">
              <Layers className="h-5 w-5 text-white" />
            </div>
            <div>
              <span className="font-bold text-lg tracking-tight bg-gradient-to-r from-white via-zinc-200 to-zinc-400 bg-clip-text text-transparent">
                AutoTest AI
              </span>
              <span className="ml-2 text-xs font-mono px-2 py-0.5 rounded bg-indigo-500/10 text-indigo-400 border border-indigo-500/20">
                v1.0.0
              </span>
            </div>
          </div>

          <div className="flex items-center gap-3">
            {health && (
              <span className="hidden sm:inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-xs font-medium bg-emerald-500/10 text-emerald-400 border border-emerald-500/20">
                <span className="h-1.5 w-1.5 rounded-full bg-emerald-400 animate-pulse" />
                Backend UP (MySQL 8.0)
              </span>
            )}

            {isAuthenticated ? (
              <div className="flex items-center gap-3">
                <Button variant="ghost" onClick={() => {
                  setSelectedProject(null);
                  setSelectedTestRunId(null);
                  setShowHistory(true);
                }}>
                  Execution History
                </Button>
                <div className="flex items-center gap-2 px-3 py-1.5 rounded-lg bg-zinc-900 border border-zinc-800 text-xs">
                  <UserIcon className="h-3.5 w-3.5 text-indigo-400" />
                  <span className="font-medium text-zinc-200">{user.username}</span>
                </div>
                <Button variant="ghost" onClick={logout} title="Sign Out">
                  <LogOut className="h-4 w-4" />
                  <span className="hidden sm:inline">Logout</span>
                </Button>
              </div>
            ) : (
              <div className="flex items-center gap-2">
                <Button
                  variant="ghost"
                  onClick={() => {
                    setAuthMode('login');
                    setAuthModalOpen(true);
                  }}
                >
                  <LogIn className="h-4 w-4" />
                  Sign In
                </Button>
                <Button
                  variant="primary"
                  onClick={() => {
                    setAuthMode('register');
                    setAuthModalOpen(true);
                  }}
                >
                  Get Started
                </Button>
              </div>
            )}
          </div>
        </div>
      </header>

      {/* Main Container */}
      <main className="flex-1 max-w-6xl w-full mx-auto px-6 py-8">
        {!isAuthenticated ? (
          <div className="max-w-3xl mx-auto py-16 text-center">
            <Badge variant="info" className="mb-4">
              Automated UI &amp; API Testing Platform
            </Badge>
            <h1 className="text-4xl sm:text-5xl font-extrabold tracking-tight text-white mb-6">
              Production-Grade Testing Powered by{' '}
              <span className="bg-gradient-to-r from-indigo-400 to-violet-400 bg-clip-text text-transparent">
                Playwright &amp; Spring Boot
              </span>
            </h1>
            <p className="text-lg text-zinc-400 mb-8 leading-relaxed">
              Define, execute, and monitor automated Playwright UI actions and REST Assured API test suites.
              Gain deep visibility into test runs with live failure analytics and downloadable reports.
            </p>
            <div className="flex items-center justify-center gap-4">
              <Button
                variant="primary"
                onClick={() => {
                  setAuthMode('register');
                  setAuthModalOpen(true);
                }}
                className="px-6 py-3 text-base"
              >
                Create Free Account
              </Button>
              <Button
                variant="secondary"
                onClick={() => {
                  setAuthMode('login');
                  setAuthModalOpen(true);
                }}
                className="px-6 py-3 text-base"
              >
                Sign In
              </Button>
            </div>
          </div>
        ) : selectedTestRunId ? (
          <TestRunDetails
            run={selectedTestRun}
            results={selectedTestResults}
            loading={loadingTestRun}
            error={testRunError}
            onBack={() => {
              setSelectedTestRunId(null);
              setShowHistory(returnToHistory);
              if (returnToHistory) fetchHistory(historyPage?.number || 0);
              else fetchDashboard();
            }}
          />
        ) : showHistory ? (
          <ExecutionHistory
            page={historyPage}
            projects={projects}
            filters={historyFilters}
            loading={loadingHistory}
            error={historyError}
            onFiltersChange={setHistoryFilters}
            onApplyFilters={(event) => {
              event.preventDefault();
              fetchHistory(0, historyFilters);
            }}
            onPageChange={(page) => fetchHistory(page)}
            onSelectRun={handleSelectRun}
            onBack={() => setShowHistory(false)}
          />
        ) : selectedProject ? (
          /* Project Test Cases Detail View */
          <div className="space-y-6">
            <div className="flex items-center justify-between border-b border-zinc-800 pb-4">
              <div className="flex items-center gap-3">
                <Button variant="ghost" onClick={() => setSelectedProject(null)} className="p-2">
                  <ArrowLeft className="h-4 w-4" />
                </Button>
                <div>
                  <div className="flex items-center gap-2">
                    <h1 className="text-2xl font-bold text-white tracking-tight">{selectedProject.name}</h1>
                    <Badge variant="neutral">{selectedProject.baseUrl}</Badge>
                  </div>
                  <p className="text-xs text-zinc-400 mt-0.5">{selectedProject.description}</p>
                </div>
              </div>

              <div className="flex items-center gap-3">
                <Button variant="secondary" onClick={() => fetchTestCases(selectedProject.id)} loading={loadingTestCases}>
                  <RefreshCw className={`h-4 w-4 ${loadingTestCases ? 'animate-spin' : ''}`} />
                  Refresh
                </Button>
                <Button variant="primary" onClick={() => setCreateTestCaseOpen(true)}>
                  <Plus className="h-4 w-4" />
                  Add Test Case
                </Button>
              </div>
            </div>

            {/* Test Cases Table */}
            {testCases.length === 0 && !loadingTestCases ? (
              <div className="text-center py-16 bg-zinc-900/40 border border-zinc-800/80 rounded-2xl p-8">
                <PlayCircle className="h-12 w-12 text-zinc-600 mx-auto mb-4" />
                <h3 className="text-lg font-semibold text-white">No test cases configured</h3>
                <p className="text-sm text-zinc-400 mt-1 mb-6 max-w-sm mx-auto">
                  Add your first UI browser automation sequence or API endpoint assertion.
                </p>
                <Button variant="primary" onClick={() => setCreateTestCaseOpen(true)}>
                  <Plus className="h-4 w-4" />
                  Add First Test Case
                </Button>
              </div>
            ) : (
              <div className="space-y-3">
                {testCases.map((tc) => {
                  const config = JSON.parse(tc.configuration || '{}');
                  return (
                    <div
                      key={tc.id}
                      className="bg-zinc-900/70 border border-zinc-800 rounded-xl p-5 hover:border-zinc-700 transition flex flex-col md:flex-row md:items-center justify-between gap-4"
                    >
                      <div className="space-y-1.5">
                        <div className="flex items-center gap-2.5">
                          <span className="font-semibold text-white text-base">{tc.name}</span>
                          <Badge variant={tc.testType === 'UI_TEST' ? 'info' : 'success'}>
                            {tc.testType}
                          </Badge>
                        </div>
                        <p className="text-xs text-zinc-400">{tc.description || 'No description'}</p>

                        {/* Configuration Preview */}
                        <div className="flex items-center gap-2 pt-1 text-xs font-mono text-zinc-400">
                          {tc.testType === 'UI_TEST' ? (
                            <span>
                              Actions:{' '}
                              <span className="text-indigo-400 font-semibold">
                                {config.actions?.map((a) => a.type).join(' ➔ ') || 'None'}
                              </span>
                            </span>
                          ) : (
                            <span>
                              Endpoint:{' '}
                              <span className="text-emerald-400 font-semibold">
                                {config.method} {config.endpoint}
                              </span>{' '}
                              (Expects {config.expectedStatus})
                            </span>
                          )}
                        </div>
                        {testExecutionErrors[tc.id] && (
                          <p role="alert" className="text-xs text-rose-300 pt-2">
                            {testExecutionErrors[tc.id]}
                          </p>
                        )}
                        {testResults[tc.id] && (
                          <div
                            role="status"
                            aria-live="polite"
                            className="mt-3 rounded-lg border border-zinc-800 bg-zinc-950/70 p-3 space-y-2"
                          >
                            <div className="flex items-center gap-2">
                              <Badge
                                variant={
                                  testResults[tc.id].status === 'PASSED' ? 'success'
                                    : testResults[tc.id].status === 'FAILED' ? 'danger'
                                      : 'warning'
                                }
                              >
                                {testResults[tc.id].status === 'PASSED' ? 'PASS'
                                  : testResults[tc.id].status === 'FAILED' ? 'FAIL'
                                    : testResults[tc.id].status}
                              </Badge>
                              <span className="text-xs text-zinc-400">
                                {testResults[tc.id].durationMs} ms
                              </span>
                            </div>
                            {testResults[tc.id].errorMessage && (
                              <p className="text-xs text-rose-300">
                                {testResults[tc.id].errorMessage}
                              </p>
                            )}
                            {testResults[tc.id].executionLog && (
                              <details className="text-xs text-zinc-400">
                                <summary className="cursor-pointer hover:text-zinc-200">
                                  Execution details
                                </summary>
                                <pre className="mt-2 whitespace-pre-wrap break-words font-mono text-[11px]">
                                  {testResults[tc.id].executionLog}
                                </pre>
                              </details>
                            )}
                          </div>
                        )}
                      </div>

                      <div className="flex items-center gap-2 self-end md:self-center">
                        <Button
                          variant="secondary"
                          loading={Boolean(runningTestCases[tc.id])}
                          disabled={Boolean(runningTestCases[tc.id])}
                          onClick={() => handleRunTest(tc.id)}
                        >
                          {runningTestCases[tc.id] ? 'Running...'
                            : pendingTestRuns[tc.id] ? 'Check Result' : 'Run Test'}
                        </Button>
                        <button
                          onClick={() => handleDeleteTestCase(tc.id)}
                          className="text-zinc-600 hover:text-rose-400 transition cursor-pointer p-2"
                          title="Delete Test Case"
                        >
                          <Trash2 className="h-4 w-4" />
                        </button>
                      </div>
                    </div>
                  );
                })}
              </div>
            )}
          </div>
        ) : (
          /* Projects Overview Grid */
          <div className="space-y-6">
            <Dashboard
              metrics={dashboardMetrics}
              loading={loadingDashboard}
              error={dashboardError}
              onRefresh={fetchDashboard}
              onSelectRun={(id) => handleSelectRun(id, false)}
              onShowHistory={() => setShowHistory(true)}
            />
            <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4 border-b border-zinc-800 pb-6">
              <div>
                <h1 className="text-2xl font-bold tracking-tight text-white flex items-center gap-2">
                  <FolderKanban className="h-6 w-6 text-indigo-400" /> Testing Projects
                </h1>
                <p className="text-sm text-zinc-400 mt-1">
                  Select a project to inspect and create automated test cases.
                </p>
              </div>
              <div className="flex items-center gap-3">
                <Button variant="secondary" onClick={fetchProjects} loading={loadingProjects}>
                  <RefreshCw className={`h-4 w-4 ${loadingProjects ? 'animate-spin' : ''}`} />
                  Refresh
                </Button>
                <Button variant="primary" onClick={() => setCreateProjectOpen(true)}>
                  <Plus className="h-4 w-4" />
                  New Project
                </Button>
              </div>
            </div>

            {projects.length === 0 && !loadingProjects ? (
              <div className="text-center py-16 bg-zinc-900/40 border border-zinc-800/80 rounded-2xl p-8">
                <FolderKanban className="h-12 w-12 text-zinc-600 mx-auto mb-4" />
                <h3 className="text-lg font-semibold text-white">No projects found</h3>
                <p className="text-sm text-zinc-400 mt-1 mb-6 max-w-sm mx-auto">
                  Create your first testing project to configure UI and API automated tests.
                </p>
                <Button variant="primary" onClick={() => setCreateProjectOpen(true)}>
                  <Plus className="h-4 w-4" />
                  Create First Project
                </Button>
              </div>
            ) : (
              <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
                {projects.map((proj) => (
                  <div
                    key={proj.id}
                    onClick={() => handleSelectProject(proj)}
                    className="bg-zinc-900/60 border border-zinc-800 hover:border-indigo-500/50 rounded-xl p-5 transition flex flex-col justify-between group cursor-pointer"
                  >
                    <div>
                      <div className="flex items-start justify-between gap-3 mb-2">
                        <h3 className="font-semibold text-white text-base tracking-tight group-hover:text-indigo-400 transition">
                          {proj.name}
                        </h3>
                        <button
                          onClick={(e) => {
                            e.stopPropagation();
                            handleDeleteProject(proj.id);
                          }}
                          className="text-zinc-600 hover:text-rose-400 transition cursor-pointer p-1"
                          title="Delete Project"
                        >
                          <Trash2 className="h-4 w-4" />
                        </button>
                      </div>

                      <p className="text-xs text-zinc-400 line-clamp-2 mb-4">
                        {proj.description || 'No description provided.'}
                      </p>

                      <div className="flex items-center gap-1.5 text-xs font-mono text-zinc-300 bg-zinc-950/80 px-2.5 py-1.5 rounded-lg border border-zinc-800/80 truncate">
                        <Globe className="h-3.5 w-3.5 text-indigo-400 shrink-0" />
                        <span className="truncate">{proj.baseUrl}</span>
                      </div>
                    </div>

                    <div className="mt-5 pt-4 border-t border-zinc-800/80 flex items-center justify-between text-xs text-zinc-500">
                      <span className="text-indigo-400 font-medium">Click to manage tests ➔</span>
                      <span>{new Date(proj.createdAt).toLocaleDateString()}</span>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>
        )}
      </main>

      {/* Modals */}
      <AuthModal
        isOpen={authModalOpen}
        onClose={() => setAuthModalOpen(false)}
        initialMode={authMode}
        onSuccess={fetchProjects}
      />

      <CreateProjectModal
        isOpen={createProjectOpen}
        onClose={() => setCreateProjectOpen(false)}
        onProjectCreated={(newProj) => {
          setProjects((prev) => [newProj, ...prev]);
          fetchDashboard();
        }}
      />

      {selectedProject && (
        <CreateTestCaseModal
          isOpen={createTestCaseOpen}
          onClose={() => setCreateTestCaseOpen(false)}
          projectId={selectedProject.id}
          onTestCaseCreated={(newTc) => {
            setTestCases((prev) => [newTc, ...prev]);
            fetchDashboard();
          }}
        />
      )}

      {/* Footer */}
      <footer className="border-t border-zinc-900 py-6 text-center text-xs text-zinc-500">
        AutoTest AI — Web &amp; API Automation Testing Platform
      </footer>
    </div>
  );
}
