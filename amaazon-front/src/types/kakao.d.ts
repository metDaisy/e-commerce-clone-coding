interface KakaoAuth {
  authorize(options: {
    redirectUri: string;
  }): void;
}

interface KakaoSDK {
  Auth: KakaoAuth;
}

interface Window {
  Kakao?: KakaoSDK;
}