import { PATHS } from "@/constants/paths";
import { Link } from "react-router-dom";

function Header({ isHomePage = true }: { readonly isHomePage?: boolean }) {
  return (
    <header className="fixed top-0 w-full z-50 bg-slate-50/80 backdrop-blur-2xl">
      <div className="flex items-center h-22.5 px-8 max-w-screen-2xl mx-auto">
        <Link
          to={PATHS.ONBOARDING}
          className="text-2xl font-extrabold text-slate-800 tracking-tighter font-headline cursor-pointer"
        >
          Social Pulse
        </Link>

        {isHomePage ? (
          <div className="ml-auto flex items-center gap-3">
            <span className="hidden md:inline text-sm">Already a member?</span>

            <Link to={PATHS.LOGIN}>
              <button className="px-4 md:px-6 py-2 rounded-full border hover:bg-gray-100 transition">
                <span className="md:hidden">Sign In</span>
                <span className="hidden md:inline">Sign In</span>
              </button>
            </Link>
          </div>
        ) : (
          <div />
        )}
      </div>
    </header>
  );
}

export default Header;
