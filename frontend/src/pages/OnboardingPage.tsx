import { PATHS } from "@/constants/paths";
import { useNavigate } from "react-router-dom";

export default function OnboardingPage() {
  const navigate = useNavigate();

  return (
    <div className="bg-surface font-body text-on-surface min-h-screen flex flex-col">
      {/* HEADER */}
      <header className="fixed top-0 w-full z-50 bg-slate-50/80 backdrop-blur-2xl">
        <div className="flex justify-between items-center px-8 py-6 max-w-screen-2xl mx-auto">
          <div className="text-2xl font-extrabold text-slate-800 tracking-tighter font-headline">
            Social Pulse
          </div>

          <div className="flex items-center gap-6">
            <span className="text-sm">Already a member?</span>
            <button
              onClick={() => navigate(PATHS.LOGIN)}
              className="px-6 py-2 rounded-full border hover:bg-gray-100 transition"
            >
              Sign In
            </button>
          </div>
        </div>
      </header>

      <main className="grow flex items-center justify-center pt-24 pb-12 px-6">
        <div className="max-w-6xl w-full grid grid-cols-1 lg:grid-cols-2 gap-16 items-center">
          <div className="flex flex-col space-y-10">
            <div>
              <h1 className="text-5xl font-bold">
                Join community <br />
                <span className="text-primary">and find your pulse</span>
              </h1>

              <p className="mt-4 text-gray-500">
                Experience a social platform designed for friendly and smart
                interaction.
              </p>
            </div>

            <div className="flex gap-4">
              <button
                onClick={() => navigate(PATHS.REGISTER)}
                className="bg-primary text-white px-6 py-3 rounded-full"
              >
                Get Started
              </button>

              <button className="px-6 py-3 rounded-full border">
                Learn more
              </button>
            </div>
          </div>

          <div>
            <img
              src="https://img.freepik.com/premium-vector/community-care-icons-team-help-illustration_911078-7846.jpg?semt=ais_hybrid&w=740&q=80"
              alt="community"
              className="rounded-3xl"
            />
          </div>
        </div>
      </main>

      <footer className="py-10 px-8">
        <div className="max-w-screen-2xl mx-auto flex justify-between items-center">
          <div className="flex gap-6 text-sm text-gray-500">
            <span>Privacy Policy</span>
            <span>Terms</span>
          </div>
          <div>
            <span className="text-sm text-gray-500">
              &copy; {new Date().getFullYear()} Social Pulse. All rights
              reserved by DUT students.
            </span>
          </div>
        </div>
      </footer>
    </div>
  );
}
