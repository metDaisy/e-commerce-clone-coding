import amazonLogo from '@/assets/amazon-logo.jpg';
import React from 'react';
import { Link } from 'react-router-dom';
import './SignIn.css';
import { useSignInForm } from './useSignInForm';
import InputField from '../../components/common/InputField';
import { AUTH_CONSTANTS, generateRandomState } from '../../constants/auth';
import SocialLoginSection from '../../components/Auth/SocialLoginSection';

const NAVER_STATE = generateRandomState();

const SignIn = () => {
  const { form, onSubmit } = useSignInForm();
  const { register, formState: { errors } } = form;

  const handleNaverLogin = () => {
    const naverAuthUrl = `${AUTH_CONSTANTS.NAVER.AUTH_URL}?response_type=code&client_id=${AUTH_CONSTANTS.NAVER.CLIENT_ID}&state=${NAVER_STATE}&redirect_uri=${AUTH_CONSTANTS.NAVER.REDIRECT_URI}`;
    window.location.href = naverAuthUrl;
  };

  const handleKakaoLogin = () => {
    try {
      if (window.Kakao && window.Kakao.Auth) {
        window.Kakao.Auth.authorize({
          redirectUri: AUTH_CONSTANTS.KAKAO.REDIRECT_URI,
        });
      } else {
        console.error('카카오 SDK가 로드되지 않았습니다.');
      }
    } catch (error) {
      console.error('카카오 로그인 실패:', error);
    }
  };

  const handleGithubLogin = async () => {
    try {
      console.log('깃허브 로그인 클릭');
    } catch (error) {
      console.error('깃허브 로그인 실패:', error);
    }
  };

  return (
    <div className='signin-wrapper'>
      <div className='signin-container'>
        <div className='logo'>
          <Link to='/'>
            <img src={amazonLogo} alt='Amazon Logo' className='logo-img' />
          </Link>
        </div>

        <div className='form-box'>
          <form onSubmit={onSubmit}>
            <InputField
              label='Email'
              type='text'
              placeholder='Enter your email or phone number'
              error={errors.email?.message}
              {...register('email')}
            />

            <InputField
              label='Password'
              type='password'
              placeholder='Enter your password'
              helpText={<a href='#'>Forgot your password?</a>}
              error={errors.password?.message}
              {...register('password')}
            />

            <button type='submit' className='signin-btn'>
              Sign in
            </button>
          </form>

          <div className='legal-text'>
            By continuing, you agree to Amazon's{' '}
            <a href='#'>Conditions of Use</a> and <a href='#'>Privacy Notice</a>
            .
          </div>

          <div className='new-to-amazon'>
            <span className='line'></span>
            <span className='text'>New to Amazon?</span>
            <span className='line'></span>
          </div>

          <Link to='/signup' className='create-account-btn-link'>
            Create your Amazon account
          </Link>

          <div className='divider'></div>

          {/* 소셜 로그인 섹션 */}
          <SocialLoginSection
            onNaverLogin={handleNaverLogin}
            onKakaoLogin={handleKakaoLogin}
            onGithubLogin={handleGithubLogin}
          />
        </div>
      </div>
    </div>
  );
};

export default SignIn;