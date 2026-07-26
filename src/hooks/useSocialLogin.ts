import { useCallback } from "react";

const NAVER_CLIENT_ID = import.meta.env.VITE_NAVER_CLIENT_ID;
const NAVER_REDIRECT_URI = "http://localhost:5173/naver/callback";
const NAVER_STATE = Math.random().toString(36).substring(7);

const KAKAO_REDIRECT_URI = "http://localhost:5173/kakao/callback";

export const useSocialLogin = () => {
  const handleNaverLogin = useCallback(() => {
    if (!NAVER_CLIENT_ID) {
      console.error("네이버 클라이언트 ID가 설정되지 않았습니다.");
      return;
    }
    const naverAuthUrl = `https://nid.naver.com/oauth2.0/authorize?response_type=code&client_id=${NAVER_CLIENT_ID}&state=${NAVER_STATE}&redirect_uri=${NAVER_REDIRECT_URI}`;
    window.location.href = naverAuthUrl;
  }, []);

  const handleKakaoLogin = useCallback(() => {
    try {
      const kakao = (window as any).Kakao;
      if (kakao && !kakao.isInitialized()) {
        kakao.init(import.meta.env.VITE_KAKAO_JAVASCRIPT_KEY);
      }
      
      if (kakao && kakao.Auth) {
        kakao.Auth.authorize({
          redirectUri: KAKAO_REDIRECT_URI,
        });
      } else {
        console.error("카카오 SDK가 로드되지 않았습니다.");
      }
    } catch (error) {
      console.error("카카오 로그인 실패:", error);
    }
  }, []);

  const handleGithubLogin = useCallback(() => {
    // TODO: GitHub OAuth 로직 구현
    console.log("GitHub 로그인 클릭");
  }, []);

  return {
    handleNaverLogin,
    handleKakaoLogin,
    handleGithubLogin,
  };
};