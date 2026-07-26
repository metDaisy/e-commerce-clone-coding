import { Outlet } from "react-router-dom";

const AuthLayout: React.FC = () => {
  return (
    <div className="app" id="app">
      <main className="main" id="main-content">
        <Outlet />
      </main>
    </div>
  );
};

export default AuthLayout;