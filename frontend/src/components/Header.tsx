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
export default function Header({ isHomePage = true }: HeaderProps) {
  return (
    <header className="fixed top-0 w-full z-50 bg-[rgba(0,0,0,0.8)] backdrop-blur-[20px] saturate-[180%]">
      <div className="flex justify-between items-center h-12 px-4 max-w-[980px] mx-auto w-full" style={{ fontFamily: '"SF Pro Text", "SF Pro Icons", "Helvetica Neue", Helvetica, Arial, sans-serif' }}>
        <div className="flex items-center gap-8">
          <Link
            to={PATHS.ONBOARDING}
            className="text-[14px] font-semibold text-[#f5f5f7] tracking-tight cursor-pointer flex items-center opacity-90 hover:opacity-100 transition-opacity"
            style={{ fontFamily: '"SF Pro Display", "SF Pro Icons", "Helvetica Neue", Helvetica, Arial, sans-serif' }}
          >
            Social Pulse
          </Link>
          
          <div className="hidden md:flex items-center gap-8 text-[12px] font-normal text-[#f5f5f7] opacity-80">
            <Link to={PATHS.LEARN_MORE} className="hover:opacity-100 transition-opacity">Tính năng</Link>
            <Link to={PATHS.LEARN_MORE} className="hover:opacity-100 transition-opacity">Cộng đồng</Link>
            <Link to={PATHS.LEARN_MORE} className="hover:opacity-100 transition-opacity">Bảng giá</Link>
          </div>
        </div>

        {isHomePage && (
          <div className="flex items-center gap-4">
            <Link
              to={PATHS.LOGIN}
              className="text-[12px] font-normal text-[#f5f5f7] opacity-80 hover:opacity-100 transition-opacity"
            >
              Đăng nhập
            </Link>
          </div>
        )}
      </div>
    </header>
  );
}
