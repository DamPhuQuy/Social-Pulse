/**
 * Decodes the payload of a JWT without verifying the signature.
 * Safe to use on the client side for reading claims (role, userId, etc.)
 * because the actual authorization enforcement happens on the server.
 */
export function decodeJwtPayload(token: string): Record<string, unknown> | null {
  try {
    const base64Url = token.split(".")[1];
    if (!base64Url) return null;
    const base64 = base64Url.replace(/-/g, "+").replace(/_/g, "/");
    const jsonPayload = decodeURIComponent(
      atob(base64)
        .split("")
        .map((c) => "%" + ("00" + c.charCodeAt(0).toString(16)).slice(-2))
        .join("")
    );
    return JSON.parse(jsonPayload);
  } catch {
    return null;
  }
}

/**
 * Returns true if the JWT contains the ADMIN role.
 */
export function isAdminToken(token: string | null): boolean {
  if (!token) return false;
  const payload = decodeJwtPayload(token);
  if (!payload) return false;
  const roles = payload["roles"];
  if (Array.isArray(roles)) {
    return roles.includes("ADMIN");
  }
  return false;
}
