import React, { useState } from 'react';
import { Input } from '../common/Input';
import { Button } from '../common/Button';
import { apiClient } from '../../api/client';
import { FolderPlus, AlertCircle, X } from 'lucide-react';

export const CreateProjectModal = ({ isOpen, onClose, onProjectCreated }) => {
  const [name, setName] = useState('');
  const [description, setDescription] = useState('');
  const [baseUrl, setBaseUrl] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  if (!isOpen) return null;

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);
    setError(null);
    try {
      const newProj = await apiClient.post('/projects', { name, description, baseUrl });
      setName('');
      setDescription('');
      setBaseUrl('');
      onProjectCreated?.(newProj);
      onClose();
    } catch (err) {
      setError(err.message || 'Failed to create project');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-zinc-950/80 backdrop-blur-sm">
      <div className="bg-zinc-900 border border-zinc-800 rounded-2xl w-full max-w-lg p-6 shadow-2xl relative animate-in fade-in zoom-in-95 duration-200">
        <button
          onClick={onClose}
          className="absolute top-4 right-4 text-zinc-500 hover:text-zinc-300 cursor-pointer"
        >
          <X className="h-5 w-5" />
        </button>

        <div className="flex items-center gap-3 mb-6">
          <div className="h-10 w-10 rounded-xl bg-gradient-to-tr from-indigo-600 to-violet-500 flex items-center justify-center shadow-lg shadow-indigo-500/20">
            <FolderPlus className="h-5 w-5 text-white" />
          </div>
          <div>
            <h2 className="text-xl font-bold text-white tracking-tight">Create Testing Project</h2>
            <p className="text-xs text-zinc-400">Configure a target application for UI and API tests</p>
          </div>
        </div>

        {error && (
          <div className="mb-4 p-3 rounded-lg bg-rose-500/10 border border-rose-500/20 flex items-start gap-2.5">
            <AlertCircle className="h-4 w-4 text-rose-400 mt-0.5 shrink-0" />
            <p className="text-xs text-rose-300 font-medium">{error}</p>
          </div>
        )}

        <form onSubmit={handleSubmit} className="space-y-4">
          <Input
            label="Project Name"
            placeholder="e.g. Production Storefront"
            value={name}
            onChange={(e) => setName(e.target.value)}
            required
          />

          <Input
            label="Base URL"
            placeholder="e.g. https://example.com or http://localhost:3000"
            value={baseUrl}
            onChange={(e) => setBaseUrl(e.target.value)}
            required
          />

          <div className="flex flex-col gap-1.5 w-full">
            <label className="text-xs font-medium text-zinc-300">Description</label>
            <textarea
              rows={3}
              placeholder="Brief description of this testing suite..."
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              className="w-full rounded-lg bg-zinc-900 border border-zinc-800 focus:border-indigo-500 focus:ring-1 focus:ring-indigo-500 px-3.5 py-2 text-sm text-zinc-100 placeholder-zinc-500 focus:outline-none transition-all"
            />
          </div>

          <div className="flex items-center justify-end gap-3 pt-3">
            <Button type="button" variant="ghost" onClick={onClose}>
              Cancel
            </Button>
            <Button type="submit" variant="primary" loading={loading}>
              Create Project
            </Button>
          </div>
        </form>
      </div>
    </div>
  );
};
