import amazonLogo from '@/assets/amazon-logo.jpg';
import React from 'react';
import { Link } from 'react-router-dom';
import './SignUp.css';
import { useSignUpForm } from './useSignUpForm';
import InputField from '../../components/common/InputField';

const SignUp = () => {
  const { form, onSubmit } = useSignUpForm();
  const { register, formState: { errors } } = form;

  return (
    <div className='signup-wrapper'>
      <div className='signup-container'>
        <div className='logo'>
          <Link to='/'>
            <img src={amazonLogo} alt='Amazon Logo' className='logo-img' />
          </Link>
        </div>

        <form onSubmit={onSubmit}>
          <InputField
            label='Your name'
            type='text'
            placeholder='First and last name'
            error={errors.name?.message}
            {...register('name')}
          />

          <InputField
            label='Email address'
            type='email'
            placeholder='Enter your email'
            helpText='We&apos;ll send you a verification code'
            error={errors.email?.message}
            {...register('email')}
          />

          <InputField
            label='Password'
            type='password'
            placeholder='At least 6 characters'
            helpText='Passwords must be at least 6 characters.'
            error={errors.password?.message}
            {...register('password')}
          />

          <InputField
            label='Re-enter password'
            type='password'
            placeholder='Re-enter password'
            error={errors.confirmPassword?.message}
            {...register('confirmPassword')}
          />

          <button type='submit' className='signup-btn'>
            Verify email
          </button>
        </form>

        <div className='divider'></div>

        <div className='conditions'>
          By creating an account, you agree to Amazon&apos;s{' '}
          <a href='#'>Conditions of Use</a> and <a href='#'>Privacy Notice</a>.
        </div>

        <div className='signin-link'>
          <a href='/signin'>Already have an account? Sign in</a>
        </div>
      </div>
    </div>
  );
};

export default SignUp;