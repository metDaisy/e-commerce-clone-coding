import { useEffect } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";

const Callback = () => {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();

  useEffect(() => {
    const code = searchParams.get("code");
    const state = searchParams.get("state");

    if (code && state) {
      // 여기서 서버로 code를 보내서 access_token을 받아야 합니다.
      // 예: POST /api/auth/naver/callback { code, state }
      console.log("네이버 로그인 code:", code);
      console.log("네이버 로그인 state:", state);

      // 로그인 성공 후 메인 페이지로 리디렉션
      navigate("/");
    } else {
      // 로그인 실패 또는 에러
      console.error("네이버 로그인 실패");
      navigate("/signin");
    }
  }, [searchParams, navigate]);

  return <div>네이버 로그인 처리 중...</div>;
};

export default Callback;
