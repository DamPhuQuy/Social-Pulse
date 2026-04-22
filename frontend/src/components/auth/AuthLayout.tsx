import Header from "@/components/Header";
import type { ReactNode } from "react";

type AuthLayoutProps = {
  /** The right-hand hero panel heading */
  heroTitle: ReactNode;
  /** The right-hand hero panel body text */
  heroBody: string;
  /** Image URL shown in the hero panel */
  heroImageSrc: string;
  /** Alt text for the hero image */
  heroImageAlt: string;
  /** The form card rendered on the left column */
  children: ReactNode;
};

/**
 * Two-column layout shared by Login and Register pages.
 *
 * Left column  → form card (always visible)
 * Right column → hero branding panel (hidden on mobile, visible lg+)
 *
 * Usage:
 * ```tsx
 * <AuthLayout
 *   heroTitle={<>Welcome back to <span className="text-primary">Social Pulse.</span></>}
 *   heroBody="Sign in to reconnect with your communities."
 *   heroImageSrc="/illustrations/login.svg"
 *   heroImageAlt="Login illustration"
 * >
 *   <LoginForm />
 * </AuthLayout>
 * ```
 */
export function AuthLayout({
  heroTitle,
  heroBody,
  heroImageSrc,
  heroImageAlt,
  children,
}: AuthLayoutProps) {
  return (
    <div className="bg-surface font-body text-on-surface min-h-screen flex flex-col">
      <Header isHomePage={false} />

      <main className="flex-1 grid lg:grid-cols-2">
        {/* Form column */}
        <div className="flex items-center justify-center p-8 bg-surface">
          <div className="w-full max-w-md">{children}</div>
        </div>

        {/* Hero column – hidden on small screens */}
        <div className="hidden lg:flex flex-col items-center justify-center p-12 bg-surface-container-low">
          <div className="max-w-md text-center space-y-6">
            <h2 className="font-headline text-3xl font-bold tracking-tight text-on-surface">
              {heroTitle}
            </h2>

            <p className="text-on-surface-variant text-lg">{heroBody}</p>

            <div className="pt-8">
              <img
                src={heroImageSrc}
                alt={heroImageAlt}
                className="w-full h-auto drop-shadow-xl rounded-2xl"
              />
            </div>
          </div>
        </div>
      </main>
    </div>
  );
}
