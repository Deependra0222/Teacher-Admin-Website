import {
  applicationDefault,
  deleteApp,
  initializeApp,
  type App,
} from "firebase-admin/app";
import { getAuth } from "firebase-admin/auth";
import { getFirestore } from "firebase-admin/firestore";

import {
  TeacherAuthorizationService,
  type RawTeacherAssignment,
  type TeacherDataStore,
  type TokenVerifier,
  type VerifiedIdentity,
} from "../auth/teacher-authorization.js";
import type { FirebaseRuntimeConfig } from "./config.js";

interface DecodedTokenLike {
  readonly uid: string;
  readonly email?: unknown;
}

export interface AdminAuthLike {
  verifyIdToken(idToken: string, checkRevoked?: boolean): Promise<DecodedTokenLike>;
}

interface DocumentSnapshotLike {
  readonly exists: boolean;
  data(): Record<string, unknown> | undefined;
}

interface QueryDocumentSnapshotLike {
  readonly id: string;
  data(): Record<string, unknown>;
}

interface QuerySnapshotLike {
  readonly docs: readonly QueryDocumentSnapshotLike[];
}

interface QueryLike {
  where(fieldPath: string, operation: "==", value: unknown): QueryLike;
  get(): Promise<QuerySnapshotLike>;
}

interface CollectionLike extends QueryLike {
  doc(id: string): { get(): Promise<DocumentSnapshotLike> };
}

export interface AdminFirestoreLike {
  collection(name: string): CollectionLike;
}

export class FirebaseAdminTokenVerifier implements TokenVerifier {
  constructor(private readonly auth: AdminAuthLike) {}

  async verifyIdToken(
    idToken: string,
    checkRevoked: boolean,
  ): Promise<VerifiedIdentity> {
    const decoded = await this.auth.verifyIdToken(idToken, checkRevoked);
    return typeof decoded.email === "string"
      ? { uid: decoded.uid, email: decoded.email }
      : { uid: decoded.uid };
  }
}

export class FirebaseAdminTeacherDataStore implements TeacherDataStore {
  constructor(private readonly firestore: AdminFirestoreLike) {}

  async getUserProfile(uid: string): Promise<Record<string, unknown> | null> {
    const snapshot = await this.firestore.collection("users").doc(uid).get();
    return snapshot.exists ? (snapshot.data() ?? null) : null;
  }

  async getActiveAssignments(
    uid: string,
  ): Promise<readonly RawTeacherAssignment[]> {
    const snapshot = await this.firestore
      .collection("teacherAssignments")
      .where("teacherUid", "==", uid)
      .where("active", "==", true)
      .get();

    return snapshot.docs.map((document) => ({
      id: document.id,
      data: document.data(),
    }));
  }
}

export interface FirebaseAdminRuntime {
  readonly teacherAuthorization: TeacherAuthorizationService;
  close(): Promise<void>;
}

function setEmulatorHosts(config: Extract<FirebaseRuntimeConfig, { mode: "emulator" }>): void {
  process.env.FIREBASE_AUTH_EMULATOR_HOST = config.authEmulatorHost;
  process.env.FIRESTORE_EMULATOR_HOST = config.firestoreEmulatorHost;
}

export function createFirebaseAdminRuntime(
  config: FirebaseRuntimeConfig,
): FirebaseAdminRuntime | undefined {
  if (config.mode === "disabled") {
    return undefined;
  }

  let app: App;
  if (config.mode === "emulator") {
    setEmulatorHosts(config);
    app = initializeApp(
      { projectId: config.projectId },
      `smartattend-teacher-emulator-${process.pid}`,
    );
  } else {
    app = initializeApp(
      {
        credential: applicationDefault(),
        projectId: config.projectId,
      },
      `smartattend-teacher-adc-${process.pid}`,
    );
  }

  const verifier = new FirebaseAdminTokenVerifier(getAuth(app));
  const dataStore = new FirebaseAdminTeacherDataStore(getFirestore(app));

  return {
    teacherAuthorization: new TeacherAuthorizationService(verifier, dataStore),
    async close() {
      await deleteApp(app);
    },
  };
}

