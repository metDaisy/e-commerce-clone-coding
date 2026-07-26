import amazonLogo from "@/assets/amazon-logo.jpg";
import kakaoLoginImage from "@/assets/kakao-login.png";
import naverLoginImage from "@/assets/naver-login.png";
import React, { useState } from "react";
import { Link } from "react-router-dom";
import SocialButton from "@/components/SocialButton";
import { useSocialLogin } from "@/hooks/useSocialLogin";
import "./SignIn.css";

const SignIn = () => {
  const [formData, setFormData] = useState({
    email: "",
    password: "",
  });

  const { handleNaverLogin, handleKakaoLogin, handleGithubLogin } = useSocialLogin();

  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const { name, value } = e.target;
    setFormData((prevData) => ({
      ...prevData,
      [name]: value,
    }));
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    console.log("Login submitted:", formData);
    // API 호출 로직 추가
  };

  return (
    <div className="signin-wrapper">
      <div className="signin-container">
        <div className="logo">
          <Link to="/">
            <img src={amazonLogo} alt="Amazon Logo" className="logo-img" />
          </Link>
        </div>

        <div className="form-box">
          <form onSubmit={handleSubmit}>
            <div className="input-group">
              <label htmlFor="email">Email</label>
              <input
                type="text"
                id="email"
                name="email"
                value={formData.email}
                onChange={handleChange}
                placeholder="Enter your email or phone number"
              />
            </div>

            <div className="input-group">
              <label htmlFor="password">Password</label>
              <input
                type="password"
                id="password"
                name="password"
                value={formData.password}
                onChange={handleChange}
                placeholder="Enter your password"
              />
              <div className="help-text">
                <a href="#">Forgot your password?</a>
              </div>
            </div>

            <button type="submit" className="signin-btn">
              Sign in
            </button>
          </form>

          <div className="legal-text">
            By continuing, you agree to Amazon&apos;s{" "}
            <a href="#">Conditions of Use</a> and <a href="#">Privacy Notice</a>
            .
          </div>

          <div className="new-to-amazon">
            <span className="line"></span>
            <span className="text">New to Amazon?</span>
            <span className="line"></span>
          </div>

          <Link to="/signup" className="create-account-btn-link">
            Create your Amazon account
          </Link>

          <div className="divider"></div>

          {/* 소셜 로그인 섹션 */}
          <div className="social-login-section">
            <div className="social-title">Or sign in with</div>
            <div className="google-login-section">
              <div
                id="g_id_onload"
                data-client_id={import.meta.env.VITE_GOOGLE_CLIENT_ID}
                data-context="signin"
                data-ux_mode="redirect"
                data-login_uri="http://localhost:5173/signin"
                data-auto_prompt="false"
              ></div>

              <div
                className="g_id_signin"
                data-type="standard"
                data-shape="pill"
                data-theme="filled_black"
                data-text="signin_with"
                data-size="large"
                data-locale="en-US"
                data-logo_alignment="left"
              ></div>
            </div>
            
            <div className="naver-image-btn" onClick={handleNaverLogin}>
              <img 
                src={naverLoginImage} 
                alt="Naver Login" 
              />
            </div>

            <div className="kakao-image-btn" onClick={handleKakaoLogin}>
              <img 
                src={kakaoLoginImage} 
                alt="Kakao Login" 
              />
            </div>

            <div className="social-buttons-grid">
              <SocialButton
                label="GitHub"
                bgColor="#24292E"
                color="#ffffff"
                icon="GH"
                onClick={handleGithubLogin}
              />
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default SignIn;