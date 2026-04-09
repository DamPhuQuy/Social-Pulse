import Header from "@/components/Header";
import Register from "@/components/Register";

function RegisterPage() {
  return (
    <div className="bg-surface font-body text-on-surface min-h-screen flex flex-col">
      <Header isHomePage={false} />

      <main className="flex-1 grid lg:grid-cols-2">
        {/* Left */}
        <div className="hidden lg:flex flex-col items-center justify-center p-12 bg-surface-container-low">
          <div className="max-w-md text-center space-y-6">
            {/* Slogan */}
            <h1 className="font-headline text-3xl font-bold tracking-tight text-on-surface">
              Your journey starts with a{" "}
              <span className="text-primary">single profile.</span>
            </h1>

            <p className="text-on-surface-variant text-lg">
              Join our community today and start socializing with like-minded
              individuals!
            </p>

            {/* Image */}
            <div className="pt-8">
              <img
                src="https://static.vecteezy.com/system/resources/previews/010/925/404/non_2x/registration-page-name-and-password-field-fill-in-form-menu-bar-corporate-website-create-account-user-information-flat-design-modern-illustration-vector.jpg"
                alt="Registration Illustration"
                className="w-full h-auto drop-shadow-xl rounded-2xl"
              />
            </div>
          </div>
        </div>
        {/* Right */}
        <div className="flex items-center justify-center p-8 bg-surface">
          <div className="w-full max-w-md">
            <Register />
          </div>
        </div>
      </main>
    </div>
  );
}

export default RegisterPage;
