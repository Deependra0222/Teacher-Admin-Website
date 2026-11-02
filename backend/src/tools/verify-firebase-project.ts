import "dotenv/config";

import { readFile } from "node:fs/promises";
import { fileURLToPath } from "node:url";

import {
  auditFirebaseProject,
  formatFirebaseProjectAudit,
  type AndroidFirebaseMetadata,
} from "../config/firebase-project-audit.js";
import { loadEnvironment } from "../config/environment.js";

interface GoogleServicesJson {
  readonly project_info?: {
    readonly project_id?: unknown;
    readonly project_number?: unknown;
  };
  readonly client?: readonly {
    readonly client_info?: {
      readonly android_client_info?: {
        readonly package_name?: unknown;
      };
    };
  }[];
}

function parseAndroidMetadata(document: GoogleServicesJson): AndroidFirebaseMetadata {
  const rawProjectId = document.project_info?.project_id;
  const rawProjectNumber = document.project_info?.project_number;
  const projectId = typeof rawProjectId === "string" ? rawProjectId : "";
  const projectNumber =
    typeof rawProjectNumber === "string" ? rawProjectNumber : "";
  const packages = (document.client ?? []).flatMap((client) => {
    const packageName = client.client_info?.android_client_info?.package_name;
    return typeof packageName === "string" ? [packageName] : [];
  });
  return { projectId, projectNumber, packages };
}

async function main(): Promise<void> {
  const googleServicesPath = fileURLToPath(
    new URL("../../../app/google-services.json", import.meta.url),
  );
  const document = JSON.parse(
    await readFile(googleServicesPath, "utf8"),
  ) as GoogleServicesJson;
  const android = parseAndroidMetadata(document);
  const config = loadEnvironment(process.env);
  const result = auditFirebaseProject({
    android,
    firebase: config.firebase,
    ...(config.firebaseWebConfig === undefined
      ? {}
      : {
          webProjectId: config.firebaseWebConfig.projectId,
          webAppId: config.firebaseWebConfig.appId,
          ...(config.firebaseWebConfig.messagingSenderId === undefined
            ? {}
            : {
                messagingSenderId:
                  config.firebaseWebConfig.messagingSenderId,
              }),
        }),
  });

  console.log(formatFirebaseProjectAudit(result));
}

void main().catch((error: unknown) => {
  const message = error instanceof Error ? error.message : "Unknown audit error";
  console.error(`Firebase project audit failed: ${message}`);
  process.exitCode = 1;
});
