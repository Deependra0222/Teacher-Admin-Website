import {
  PortalAuthError,
  createBackendClient,
  createTeacherSessionController,
} from "./teacher-auth-core.js";

const FIREBASE_SDK_VERSION = "13.1.0";
const FIREBASE_APP_URL =
  `https://www.gstatic.com/firebasejs/${FIREBASE_SDK_VERSION}/firebase-app.js`;
const FIREBASE_AUTH_URL =
  `https://www.gstatic.com/firebasejs/${FIREBASE_SDK_VERSION}/firebase-auth.js`;

async function createFirebaseGateway(configuration) {
  const [appSdk, authSdk] = await Promise.all([
    import(FIREBASE_APP_URL),
    import(FIREBASE_AUTH_URL),
  ]);

  const app = appSdk.initializeApp(configuration.firebaseOptions);
  const auth = authSdk.getAuth(app);
  if (configuration.authEmulatorUrl) {
    authSdk.connectAuthEmulator(auth, configuration.authEmulatorUrl, {
      disableWarnings: true,
    });
  }
  await authSdk.setPersistence(auth, authSdk.browserSessionPersistence);

  const initialUser = new Promise((resolve, reject) => {
    const unsubscribe = authSdk.onAuthStateChanged(
      auth,
      (user) => {
        unsubscribe();
        resolve(user);
      },
      (error) => {
        unsubscribe();
        reject(error);
      },
    );
  });

  return Object.freeze({
    async signIn(email, password) {
      const credential = await authSdk.signInWithEmailAndPassword(
        auth,
        email,
        password,
      );
      return credential.user;
    },
    async currentUser() {
      await initialUser;
      return auth.currentUser;
    },
    async signOut() {
      await authSdk.signOut(auth);
    },
    async sendPasswordReset(email) {
      await authSdk.sendPasswordResetEmail(auth, email);
    },
  });
}

function safeConfiguration() {
  const value = window.__SMARTATTEND_FIREBASE__;
  if (
    !value ||
    value.enabled !== true ||
    !value.firebaseOptions ||
    typeof value.firebaseOptions !== "object"
  ) {
    return { enabled: false };
  }
  return value;
}

export async function initializeTeacherAuth() {
  const configuration = safeConfiguration();
  let firebaseGateway;
  let initializationError;
  if (configuration.enabled) {
    try {
      firebaseGateway = await createFirebaseGateway(configuration);
    } catch (error) {
      initializationError = new PortalAuthError(
        "FIREBASE_SDK_UNAVAILABLE",
        "Firebase sign-in could not be initialized. Check the network and Web App configuration.",
        error,
      );
    }
  }

  const controller = createTeacherSessionController({
    storage: window.sessionStorage,
    firebaseGateway,
    backendClient: createBackendClient(window.fetch.bind(window)),
  });

  return Object.freeze({
    get firebaseConfigured() {
      return configuration.enabled === true && initializationError === undefined;
    },
    get initializationError() {
      return initializationError;
    },
    enterDemo: () => controller.enterDemo(),
    signIn: (email, password) => controller.signIn(email, password),
    restore: () => controller.restore(),
    loadAssignments: (session) => controller.loadAssignments(session),
    sendPasswordReset: (email) => controller.sendPasswordReset(email),
    logout: () => controller.logout(),
  });
}
