import { Outlet, useLocation } from "react-router-dom";
import Navbar from "../components/Navbar/Navbar";
import SubNav from "../components/SubNav";
import Footer from "../components/Footer";

const MainLayout: React.FC = () => {
  const location = useLocation();
  const isAuthPage =
    location.pathname === "/signin" || location.pathname === "/signup";

  return (
    <div className="app" id="app">
      {!isAuthPage && (
        <>
          <Navbar />
          <SubNav />
        </>
      )}

      <main className="main" id="main-content">
        <Outlet />
      </main>

      {!isAuthPage && <Footer />}
    </div>
  );
};

export default MainLayout;