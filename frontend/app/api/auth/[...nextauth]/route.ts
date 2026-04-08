import NextAuth from "next-auth";
import GithubProvider from "next-auth/providers/github";

const handler = NextAuth({
  providers: [
    GithubProvider({
      clientId: process.env.GITHUB_ID!,
      clientSecret: process.env.GITHUB_SECRET!,
    }),
  ],

  callbacks: {
    // Notify the backend when it successfully returns from GitHub, get the JWT
    async jwt({ token, account, profile }) {
      // First login — account and profile will be pre-filled
      if (account && profile) {
        try {
          const res = await fetch(
            `${process.env.NEXT_PUBLIC_API_URL}/auth/github/callback?code=${account.access_token}`,
            {
              method: "GET",
              headers: { "Content-Type": "application/json" },
            }
          );

          if (res.ok) {
            const data = await res.json();
            // Add JWT from the backend to the token
            token.backendJwt = data.token;
            token.githubUsername = data.githubUsername;
            token.studentId = data.studentId;
          }
        } catch (error) {
          console.error("Backend JWT fetch failed:", error);
        }
      }
      return token;
    },

    // Session object that the client will see
    async session({ session, token }) {
      session.backendJwt = token.backendJwt as string;
      session.user.githubUsername = token.githubUsername as string;
      session.user.studentId = token.studentId as string;
      return session;
    },
  },

  pages: {
    signIn: "/auth/login", // our private login page
  },
});

export { handler as GET, handler as POST };