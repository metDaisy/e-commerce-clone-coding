export const AUTH_CONSTANTS = {
  NAVER: {
    CLIENT_ID: import.meta.env.VITE_NAVER_CLIENT_ID,
    REDIRECT_URI: import.meta.env.VITE_NAVER_REDIRECT_URI || 'http://localhost:5173/naver/callback',
    AUTH_URL: 'https://nid.naver.com/oauth2.0/authorize',
  },
  KAKAO: {
    REDIRECT_URI: import.meta.env.VITE_KAKAO_REDIRECT_URI || 'http://localhost:5173/kakao/callback',
  },
  GOOGLE: {
    CLIENT_ID: import.meta.env.VITE_GOOGLE_CLIENT_ID,
    REDIRECT_URI: 'http://localhost:5173/signin',
  },
};

export const generateRandomState = (): string => {
  return Math.random().toString(36).substring(7);
};