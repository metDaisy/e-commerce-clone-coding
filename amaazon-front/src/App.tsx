import { RouterProvider, createBrowserRouter } from "react-router-dom";
import MainLayout from "./layouts/MainLayout";
import AuthLayout from "./layouts/AuthLayout";
import Home from "./pages/Home/Home";
import SignIn from "./pages/SignIn/SignIn";
import SignUp from "./pages/SignUp/SignUp";
import Callback from "./pages/Callback/Callback";

const router = createBrowserRouter([
  {
    path: "/",
    element: <MainLayout />,
    children: [
      { index: true, element: <Home /> },
      {
        path: "/signin",
        element: <AuthLayout />,
        children: [{ index: true, element: <SignIn /> }],
      },
      {
        path: "/signup",
        element: <AuthLayout />,
        children: [{ index: true, element: <SignUp /> }],
      },
      { path: "/naver/callback", element: <Callback /> },
      { path: "/kakao/callback", element: <Callback /> },
    ],
  },
]);

function App() {
  return <RouterProvider router={router} />;
}

export default App;