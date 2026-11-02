import path from "node:path";
import { fileURLToPath } from "node:url";

import type { AppConfig } from "../app.js";
import type {
  FirebaseRuntimeConfig,
  FirebaseWebConfig,
} from "../firebase/config.js";

export interface ServerConfig extends AppConfig {
  readonly port: number;
  readonly firebase: FirebaseRuntimeConfig;
}

const backendDirectory = fileURLToPath(new URL("../../", import.meta.url));
const defaultStaticDirectory = fileURLToPath(
  new URL("../../../teacher-website/", import.meta.url),
);

function parsePort(rawPort: string | undefined): number {
  const port = Number(rawPort?.trim() || "3000");

  if (!Number.isInteger(port) || port < 1 || port > 65_535) {
    throw new Error("PORT must be an integer from 1 to 65535.");
  }

  return port;
}

function parseAllowedOrigins(rawOrigins: string | undefined): readonly string[] {
  if (rawOrigins === undefined || rawOrigins.trim() === "") {
    return ["http://localhost:3000"];
  }

  return rawOrigins
    .split(",")
    .map((origin) => origin.trim())
    .filter((origin) => origin.length > 0);
}

function optionalValue(value: string | undefined): string | undefined {
  const trimmed = value?.trim();
  return trimmed ? trimmed : undefined;
}

function requiredValue(value: string | undefined, name: string): string {
  const parsed = optionalValue(value);
  if (parsed === undefined) {
    throw new Error(`${name} is required for the selected FIREBASE_MODE.`);
  }
  return parsed;
}

function parseFirebase(env: NodeJS.ProcessEnv): {
  readonly runtime: FirebaseRuntimeConfig;
  readonly web?: FirebaseWebConfig;
} {
  const mode = optionalValue(env.FIREBASE_MODE) ?? "disabled";
  if (mode !== "disabled" && mode !== "emulator" && mode !== "adc") {
    throw new Error("FIREBASE_MODE must be disabled, emulator, or adc.");
  }
  if (mode === "disabled") {
    return { runtime: { mode: "disabled" } };
  }

  const projectId = requiredValue(env.FIREBASE_PROJECT_ID, "FIREBASE_PROJECT_ID");
  if (mode === "emulator") {
    const authEmulatorHost =
      optionalValue(env.FIREBASE_AUTH_EMULATOR_HOST) ?? "127.0.0.1:9099";
    const firestoreEmulatorHost =
      optionalValue(env.FIRESTORE_EMULATOR_HOST) ?? "127.0.0.1:8080";
    return {
      runtime: {
        mode,
        projectId,
        authEmulatorHost,
        firestoreEmulatorHost,
      },
      web: {
        apiKey: optionalValue(env.FIREBASE_WEB_API_KEY) ?? `${projectId}-local-only`,
        authDomain:
          optionalValue(env.FIREBASE_WEB_AUTH_DOMAIN) ?? `${projectId}.firebaseapp.com`,
        projectId,
        appId: optionalValue(env.FIREBASE_WEB_APP_ID) ?? `${projectId}-local-only`,
        authEmulatorUrl: `http://${authEmulatorHost}`,
      },
    };
  }

  const apiKey = optionalValue(env.FIREBASE_WEB_API_KEY);
  const authDomain = optionalValue(env.FIREBASE_WEB_AUTH_DOMAIN);
  const webProjectId = optionalValue(env.FIREBASE_WEB_PROJECT_ID);
  const appId = optionalValue(env.FIREBASE_WEB_APP_ID);
  if (
    apiKey === undefined ||
    authDomain === undefined ||
    webProjectId === undefined ||
    appId === undefined
  ) {
    throw new Error(
      "Firebase Web configuration is incomplete. Set FIREBASE_WEB_API_KEY, FIREBASE_WEB_AUTH_DOMAIN, FIREBASE_WEB_PROJECT_ID, and FIREBASE_WEB_APP_ID.",
    );
  }
  if (webProjectId !== projectId) {
    throw new Error("FIREBASE_WEB_PROJECT_ID must match FIREBASE_PROJECT_ID.");
  }

  const storageBucket = optionalValue(env.FIREBASE_WEB_STORAGE_BUCKET);
  const messagingSenderId = optionalValue(env.FIREBASE_WEB_MESSAGING_SENDER_ID);
  const baseWeb: FirebaseWebConfig = {
    apiKey,
    authDomain,
    projectId: webProjectId,
    appId,
  };
  const web = {
    ...baseWeb,
    ...(storageBucket === undefined ? {} : { storageBucket }),
    ...(messagingSenderId === undefined ? {} : { messagingSenderId }),
  };
  return { runtime: { mode, projectId }, web };
}

export function loadEnvironment(env: NodeJS.ProcessEnv): ServerConfig {
  const staticDirectory = env.STATIC_DIR?.trim()
    ? path.resolve(backendDirectory, env.STATIC_DIR.trim())
    : defaultStaticDirectory;

  const firebase = parseFirebase(env);
  return {
    port: parsePort(env.PORT),
    allowedOrigins: parseAllowedOrigins(env.ALLOWED_ORIGINS),
    jsonLimit: env.JSON_LIMIT?.trim() || "100kb",
    staticDirectory,
    firebase: firebase.runtime,
    ...(firebase.web === undefined ? {} : { firebaseWebConfig: firebase.web }),
  };
}
