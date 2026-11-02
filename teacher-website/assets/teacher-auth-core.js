export const DEMO_SESSION_KEY = "smartattend_teacher_demo_session";

export class PortalAuthError extends Error {
  constructor(code, message, cause) {
    super(message, cause === undefined ? undefined : { cause });
    this.name = "PortalAuthError";
    this.code = code;
  }
}

function errorCode(error) {
  return error && typeof error === "object" && typeof error.code === "string"
    ? error.code
    : undefined;
}

export function firebaseErrorMessage(error) {
  switch (errorCode(error)) {
    case "auth/invalid-credential":
    case "auth/wrong-password":
    case "auth/user-not-found":
    case "auth/invalid-email":
      return "The email or password is incorrect.";
    case "auth/network-request-failed":
      return "Firebase could not be reached. Check your network and try again.";
    case "auth/too-many-requests":
      return "Too many sign-in attempts. Wait a moment and try again.";
    case "auth/user-disabled":
      return "This Firebase account has been disabled.";
    case "auth/operation-not-allowed":
      return "Email/Password sign-in is not enabled for this Firebase project.";
    case "auth/missing-email":
      return "Enter the faculty email address.";
    default:
      return "Firebase sign-in could not be completed. Please try again.";
  }
}

async function readError(response) {
  try {
    const body = await response.json();
    if (
      body &&
      typeof body === "object" &&
      body.error &&
      typeof body.error.code === "string" &&
      typeof body.error.message === "string"
    ) {
      return body.error;
    }
  } catch {
    // The safe fallback below deliberately avoids exposing response internals.
  }
  return {
    code: response.status === 401 ? "AUTHENTICATION_REQUIRED" : "BACKEND_ERROR",
    message:
      response.status === 401
        ? "Your Firebase session is no longer valid. Please sign in again."
        : "The SmartAttend teacher service could not complete the request.",
  };
}

export function createBackendClient(fetchImplementation) {
  async function request(user, path) {
    let token;
    try {
      token = await user.getIdToken();
    } catch (error) {
      throw new PortalAuthError(
        "TOKEN_UNAVAILABLE",
        "Your Firebase session token could not be loaded. Please sign in again.",
        error,
      );
    }

    let response;
    try {
      response = await fetchImplementation(path, {
        headers: { Authorization: `Bearer ${token}` },
      });
    } catch (error) {
      throw new PortalAuthError(
        "BACKEND_UNREACHABLE",
        "The SmartAttend teacher service is unavailable. Check that the backend is running.",
        error,
      );
    }

    if (!response.ok) {
      const safeError = await readError(response);
      throw new PortalAuthError(safeError.code, safeError.message);
    }
    return response.json();
  }

  return Object.freeze({
    async authorize(user) {
      const body = await request(user, "/api/me");
      if (!body || typeof body !== "object" || !body.teacher) {
        throw new PortalAuthError(
          "INVALID_BACKEND_RESPONSE",
          "The SmartAttend teacher service returned an invalid profile.",
        );
      }
      return body.teacher;
    },

    async assignments(user) {
      const body = await request(user, "/api/teacher/assignments");
      if (!body || !Array.isArray(body.assignments)) {
        throw new PortalAuthError(
          "INVALID_BACKEND_RESPONSE",
          "The SmartAttend teacher service returned invalid assignments.",
        );
      }
      return body.assignments;
    },
  });
}

export function createTeacherSessionController({
  storage,
  firebaseGateway,
  backendClient,
}) {
  async function authorize(user) {
    try {
      const teacher = await backendClient.authorize(user);
      return { mode: "real", user, teacher };
    } catch (error) {
      await firebaseGateway?.signOut();
      throw error;
    }
  }

  return Object.freeze({
    get firebaseConfigured() {
      return firebaseGateway !== undefined;
    },

    async enterDemo() {
      await firebaseGateway?.signOut();
      storage.setItem(DEMO_SESSION_KEY, "explicit");
      return { mode: "demo" };
    },

    async signIn(email, password) {
      storage.removeItem(DEMO_SESSION_KEY);
      if (firebaseGateway === undefined) {
        throw new PortalAuthError(
          "FIREBASE_NOT_CONFIGURED",
          "Teacher sign-in is not enabled on this site yet.",
        );
      }

      let user;
      try {
        user = await firebaseGateway.signIn(email, password);
      } catch (error) {
        throw new PortalAuthError(
          errorCode(error) ?? "FIREBASE_SIGN_IN_FAILED",
          firebaseErrorMessage(error),
          error,
        );
      }
      return authorize(user);
    },

    async restore() {
      if (storage.getItem(DEMO_SESSION_KEY) === "explicit") {
        return { mode: "demo" };
      }
      if (firebaseGateway === undefined) {
        return { mode: "none" };
      }
      const user = await firebaseGateway.currentUser();
      return user === null ? { mode: "none" } : authorize(user);
    },

    async loadAssignments(session) {
      if (session.mode !== "real") return [];
      return backendClient.assignments(session.user);
    },

    async sendPasswordReset(email) {
      if (firebaseGateway === undefined) {
        throw new PortalAuthError(
          "FIREBASE_NOT_CONFIGURED",
          "Teacher sign-in is not enabled on this site yet.",
        );
      }
      try {
        await firebaseGateway.sendPasswordReset(email);
      } catch (error) {
        throw new PortalAuthError(
          errorCode(error) ?? "PASSWORD_RESET_FAILED",
          firebaseErrorMessage(error),
          error,
        );
      }
    },

    async logout() {
      storage.removeItem(DEMO_SESSION_KEY);
      await firebaseGateway?.signOut();
    },
  });
}
