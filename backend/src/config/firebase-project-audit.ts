import type { FirebaseRuntimeConfig } from "../firebase/config.js";

export interface AndroidFirebaseMetadata {
  readonly projectId: string;
  readonly projectNumber: string;
  readonly packages: readonly string[];
}

export interface FirebaseProjectAuditInput {
  readonly android: AndroidFirebaseMetadata;
  readonly firebase: FirebaseRuntimeConfig;
  readonly webProjectId?: string;
  readonly webAppId?: string;
  readonly messagingSenderId?: string;
}

export interface FirebaseProjectAuditResult {
  readonly status: "disabled" | "emulator" | "ready";
  readonly liveProjectConfigReady: boolean;
  readonly androidProjectId: string;
  readonly androidProjectNumber: string;
  readonly androidPackages: readonly string[];
  readonly backendProjectId?: string;
  readonly webProjectId?: string;
}

function requiredTrimmed(value: string, message: string): string {
  const trimmed = value.trim();
  if (trimmed === "") {
    throw new Error(message);
  }
  return trimmed;
}

export function auditFirebaseProject(
  input: FirebaseProjectAuditInput,
): FirebaseProjectAuditResult {
  const androidProjectId = requiredTrimmed(
    input.android.projectId,
    "Android Firebase project ID is missing from app/google-services.json.",
  );
  const androidProjectNumber = requiredTrimmed(
    input.android.projectNumber,
    "Android Firebase project number is missing from app/google-services.json.",
  );
  const androidPackages = input.android.packages
    .map((packageName) => packageName.trim())
    .filter((packageName) => packageName !== "");

  if (input.firebase.mode === "disabled") {
    return {
      status: "disabled",
      liveProjectConfigReady: false,
      androidProjectId,
      androidProjectNumber,
      androidPackages,
    };
  }

  const backendProjectId = requiredTrimmed(
    input.firebase.projectId,
    "FIREBASE_PROJECT_ID is missing.",
  );
  const webProjectId = input.webProjectId?.trim();

  if (input.firebase.mode === "emulator") {
    return {
      status: "emulator",
      liveProjectConfigReady: false,
      androidProjectId,
      androidProjectNumber,
      androidPackages,
      backendProjectId,
      ...(webProjectId === undefined || webProjectId === "" ? {} : { webProjectId }),
    };
  }

  if (backendProjectId !== androidProjectId) {
    throw new Error(
      `FIREBASE_PROJECT_ID must match Android project ${androidProjectId}; received ${backendProjectId}.`,
    );
  }
  if (webProjectId === undefined || webProjectId === "") {
    throw new Error("FIREBASE_WEB_PROJECT_ID is missing for live ADC mode.");
  }
  if (webProjectId !== androidProjectId) {
    throw new Error(
      `FIREBASE_WEB_PROJECT_ID must match Android project ${androidProjectId}; received ${webProjectId}.`,
    );
  }

  const webAppId = input.webAppId?.trim();
  const webAppParts = webAppId?.split(":") ?? [];
  if (
    webAppId === undefined ||
    webAppId === "" ||
    webAppParts.length !== 4 ||
    webAppParts[0] !== "1" ||
    webAppParts[1] !== androidProjectNumber ||
    webAppParts[2] !== "web" ||
    webAppParts[3] === ""
  ) {
    throw new Error(
      `FIREBASE_WEB_APP_ID must belong to Firebase project number ${androidProjectNumber}.`,
    );
  }

  const messagingSenderId = input.messagingSenderId?.trim();
  if (
    messagingSenderId !== undefined &&
    messagingSenderId !== "" &&
    messagingSenderId !== androidProjectNumber
  ) {
    throw new Error(
      `FIREBASE_WEB_MESSAGING_SENDER_ID must match Firebase project number ${androidProjectNumber}.`,
    );
  }

  return {
    status: "ready",
    liveProjectConfigReady: true,
    androidProjectId,
    androidProjectNumber,
    androidPackages,
    backendProjectId,
    webProjectId,
  };
}

export function formatFirebaseProjectAudit(
  result: FirebaseProjectAuditResult,
): string {
  const lines = [
    "SmartAttend Firebase project audit",
    `Android project: ${result.androidProjectId}`,
    `Android project number: ${result.androidProjectNumber}`,
    `Android packages: ${result.androidPackages.join(", ") || "NONE"}`,
    `Backend mode: ${result.status}`,
  ];

  if (result.backendProjectId !== undefined) {
    lines.push(`Backend project: ${result.backendProjectId}`);
  }
  if (result.webProjectId !== undefined) {
    lines.push(`Web project: ${result.webProjectId}`);
  }
  lines.push(
    `LIVE PROJECT CONFIG READY: ${result.liveProjectConfigReady ? "YES" : "NO"}`,
  );

  return lines.join("\n");
}
