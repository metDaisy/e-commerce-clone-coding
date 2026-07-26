import React, { forwardRef } from 'react';

interface InputFieldProps extends React.InputHTMLAttributes<HTMLInputElement> {
  label: string;
  name: string;
  helpText?: React.ReactNode;
  error?: string;
}

const InputField = forwardRef<HTMLInputElement, InputFieldProps>(({
  label,
  name,
  helpText,
  error,
  required,
  ...props
}, ref) => {
  return (
    <div className="input-group">
      <label htmlFor={name}>
        {label}
        {required && ' *'}
      </label>
      <input
        id={name}
        name={name}
        ref={ref}
        required={required}
        {...props}
      />
      {error && <div className="error-text" style={{ color: '#c40000', fontSize: '12px', marginTop: '4px' }}>{error}</div>}
      {helpText && !error && <div className="help-text">{helpText}</div>}
    </div>
  );
});

InputField.displayName = 'InputField';

export default InputField;