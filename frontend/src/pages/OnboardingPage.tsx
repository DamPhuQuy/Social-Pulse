import Header from "@/components/Header";
import { PATHS } from "@/constants/paths";
import { Link } from "react-router-dom";

export default function OnboardingPage() {
  return (
    <div className="bg-surface font-body text-on-surface min-h-screen flex flex-col">
      <Header isHomePage={true} />

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
              <Link
                to={PATHS.REGISTER}
                className="bg-primary text-white px-6 py-3 rounded-full hover:bg-primary-dark transition"
              >
                Get Started
              </Link>

              <Link
                to={PATHS.LEARN_MORE}
                className="px-6 py-3 rounded-full border hover:bg-gray-100 transition"
              >
                Learn more
              </Link>
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
