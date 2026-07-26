import React from "react";

interface SocialButtonProps {
  label: string;
  bgColor: string;
  color: string;
  icon?: string;
  onClick?: () => void;
}

const SocialButton: React.FC<SocialButtonProps> = ({
  label,
  bgColor,
  color,
  icon,
  onClick,
}) => (
  <button
    type="button"
    className="social-btn"
    style={{ background: bgColor, borderColor: bgColor, color: color }}
    onClick={onClick}
  >
    {icon && <span className="social-icon">{icon}</span>}
    {label}
  </button>
);

export default SocialButton;