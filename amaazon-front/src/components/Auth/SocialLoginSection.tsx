import React, { useEffect, useRef } from 'react';
import kakaoLoginImage from '@/assets/kakao-login.png';
import naverLoginImage from '@/assets/naver-login.png';

declare global {
  interface Window {
    google?: any;
    Kakao?: any;
  }
}

interface SocialLoginSectionProps {
  onNaverLogin: () => void;
  onKakaoLogin: () => void;
  onGithubLogin: () => void;
}

const SocialButton: React.FC<{
  label: string;
  bgColor: string;
  color: string;
  icon?: string;
  onClick?: () => void;
}> = ({ label, bgColor, color, icon, onClick }) => (
  <button
    type='button'
    className='social-btn'
    style={{ background: bgColor, borderColor: bgColor, color: color }}
    onClick={onClick}
  >
    {icon && <span className='social-icon'>{icon}</span>}
    {label}
  </button>
);

const SocialLoginSection: React.FC<SocialLoginSectionProps> = ({
  onNaverLogin,
  onKakaoLogin,
  onGithubLogin,
}) => {
  const googleButtonRef = useRef<HTMLDivElement>(null);
  const isGoogleRendered = useRef(false);

  useEffect(() => {
    if (window.google && window.google.accounts && googleButtonRef.current && !isGoogleRendered.current) {
      isGoogleRendered.current = true;
      window.google.accounts.id.initialize({
        client_id: import.meta.env.VITE_GOOGLE_CLIENT_ID,
        context: 'signin',
        ux_mode: 'redirect',
        login_uri: 'http://localhost:5173/signin',
        auto_prompt: false,
      });
      window.google.accounts.id.renderButton(googleButtonRef.current, {
        theme: 'filled_black',
        size: 'large',
        type: 'standard',
        shape: 'pill',
        text: 'signin_with',
        locale: 'en-US',
        logo_alignment: 'left',
      });
    }
  }, []);

  return (
    <div className='social-login-section'>
      <div className='social-title'>Or sign in with</div>
      <div className='google-login-section' style={{ display: 'flex', justifyContent: 'center' }}>
        <div ref={googleButtonRef}></div>
      </div>

      <div
        className='naver-image-btn'
        onClick={onNaverLogin}
        style={{ cursor: 'pointer', marginTop: '10px', display: 'flex', justifyContent: 'center' }}
      >
        <img
          src={naverLoginImage}
          alt='Naver Login'
          style={{ width: '100%', height: '40px', objectFit: 'contain', borderRadius: '4px' }}
        />
      </div>

      <div
        className='kakao-image-btn'
        onClick={onKakaoLogin}
        style={{ cursor: 'pointer', marginTop: '10px', display: 'flex', justifyContent: 'center' }}
      >
        <img
          src={kakaoLoginImage}
          alt='Kakao Login'
          style={{ width: '100%', height: '40px', objectFit: 'contain', borderRadius: '4px' }}
        />
      </div>

      <div className='social-buttons-grid' style={{ marginTop: '10px', display: 'grid', gridTemplateColumns: '1fr', gap: '10px' }}>
        <SocialButton
          label='GitHub'
          bgColor='#24292E'
          color='#ffffff'
          icon='GH'
          onClick={onGithubLogin}
        />
      </div>
    </div>
  );
};

export default SocialLoginSection;