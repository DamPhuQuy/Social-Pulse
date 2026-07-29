export type ResetPasswordRequest = {
  email: string;
  otpCode: string;
  newPassword: string;
};
