import "next-auth";
import "next-auth/jwt";

declare module "next-auth" {
  interface Session {
    backendJwt: string;
    user: {
      name?: string | null;
      email?: string | null;
      image?: string | null;
      githubUsername: string;
      studentId: string;
    };
  }
}

declare module "next-auth/jwt" {
  interface JWT {
    backendJwt: string;
    githubUsername: string;
    studentId: string;
  }
}