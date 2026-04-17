import { PATHS } from "@/constants/paths";
import { Link } from "react-router-dom";

type HeaderProps = {
  /** When true, shows the "Sign In" action button on the right */
  readonly isHomePage?: boolean;
};

/**
 * App-wide top navigation bar.
 * Fixed at the top with a frosted-glass backdrop.
 */
function Header({ isHomePage = true }: HeaderProps) {
  return (
    <header className="fixed top-0 w-full z-50 bg-surface/80 backdrop-blur-2xl border-b border-outline-variant/40">
      <div className="flex items-center h-16 px-8 max-w-screen-2xl mx-auto">
        <Link
          to={PATHS.ONBOARDING}
          className="text-2xl font-extrabold text-on-surface tracking-tighter font-headline cursor-pointer"
        >
          Social Pulse
        </Link>

        {isHomePage && (
          <div className="ml-auto flex items-center gap-3">
            <span className="hidden md:inline text-sm text-on-surface-variant">
              Already a member?
            </span>

            <Link
              to={PATHS.LOGIN}
              className="px-5 py-2 rounded-full border border-outline-variant text-sm font-medium text-on-surface hover:bg-surface-container transition-colors"
            >
              Sign In
            </Link>
          </div>
        )}
      </div>
    </header>
  );
}

export default Header;
