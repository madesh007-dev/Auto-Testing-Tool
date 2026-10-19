import React from 'react';

export const Input = ({
  label,
  error,
  type = 'text',
  placeholder,
  value,
  onChange,
  required = false,
  className = '',
  ...props
}) => {
  return (
    <div className="flex flex-col gap-1.5 w-full">
      {label && (
        <label className="text-xs font-medium text-zinc-300">
          {label} {required && <span className="text-rose-400">*</span>}
        </label>
      )}
      <input
        type={type}
        placeholder={placeholder}
        value={value}
        onChange={onChange}
        required={required}
        className={`w-full rounded-lg bg-zinc-900 border ${
          error ? 'border-rose-500 focus:ring-rose-500' : 'border-zinc-800 focus:border-indigo-500 focus:ring-indigo-500'
        } px-3.5 py-2 text-sm text-zinc-100 placeholder-zinc-500 focus:outline-none focus:ring-1 transition-all ${className}`}
        {...props}
      />
      {error && <span className="text-xs text-rose-400">{error}</span>}
    </div>
  );
};
