export interface VerifiedIdentity {
  readonly uid: string;
  readonly email?: string;
}

export interface TokenVerifier {
  verifyIdToken(idToken: string, checkRevoked: boolean): Promise<VerifiedIdentity>;
}

export interface RawTeacherAssignment {
  readonly id: string;
  readonly data: Record<string, unknown>;
}

export interface TeacherDataStore {
  getUserProfile(uid: string): Promise<Record<string, unknown> | null>;
  getActiveAssignments(uid: string): Promise<readonly RawTeacherAssignment[]>;
}

export interface AuthorizedTeacher {
  readonly uid: string;
  readonly role: "teacher";
  readonly name: string;
  readonly email: string;
  readonly department: string;
}

export interface TeacherAssignment {
  readonly id: string;
  readonly subjectId: string;
  readonly batchId: string;
  readonly academicYear?: string;
}

export class TeacherAuthorizationError extends Error {
  constructor(
    readonly statusCode: number,
    readonly code: string,
    message: string,
  ) {
    super(message);
  }
}

function errorCode(error: unknown): string | undefined {
  if (typeof error !== "object" || error === null || !("code" in error)) {
    return undefined;
  }
  return typeof error.code === "string" ? error.code : undefined;
}

function requiredString(data: Record<string, unknown>, field: string): string | null {
  const value = data[field];
  if (typeof value !== "string") {
    return null;
  }
  const trimmed = value.trim();
  return trimmed.length > 0 ? trimmed : null;
}

function mapTokenError(error: unknown): TeacherAuthorizationError {
  switch (errorCode(error)) {
    case "auth/id-token-expired":
      return new TeacherAuthorizationError(
        401,
        "TOKEN_EXPIRED",
        "Your Firebase session has expired. Please sign in again.",
      );
    case "auth/id-token-revoked":
      return new TeacherAuthorizationError(
        401,
        "TOKEN_REVOKED",
        "Your Firebase session is no longer valid. Please sign in again.",
      );
    case "auth/argument-error":
    case "auth/invalid-id-token":
      return new TeacherAuthorizationError(
        401,
        "INVALID_TOKEN",
        "The Firebase authentication token is invalid.",
      );
    default:
      return new TeacherAuthorizationError(
        503,
        "AUTHORIZATION_SERVICE_UNAVAILABLE",
        "Teacher authorization is temporarily unavailable.",
      );
  }
}

function mapTeacher(uid: string, data: Record<string, unknown>): AuthorizedTeacher {
  const role = requiredString(data, "role");
  if (role === null) {
    throw new TeacherAuthorizationError(
      403,
      "TEACHER_PROFILE_INCOMPLETE",
      "The teacher profile is incomplete.",
    );
  }
  if (role !== "teacher") {
    throw new TeacherAuthorizationError(
      403,
      "TEACHER_ACCESS_DENIED",
      "This account is not authorized for the teacher portal.",
    );
  }

  if (data.active !== undefined && typeof data.active !== "boolean") {
    throw new TeacherAuthorizationError(
      403,
      "TEACHER_PROFILE_INCOMPLETE",
      "The teacher profile is incomplete.",
    );
  }
  if (data.active === false) {
    throw new TeacherAuthorizationError(
      403,
      "TEACHER_ACCOUNT_INACTIVE",
      "This teacher account is inactive.",
    );
  }

  const name = requiredString(data, "name");
  const email = requiredString(data, "email");
  const department = requiredString(data, "department");
  if (name === null || email === null || department === null) {
    throw new TeacherAuthorizationError(
      403,
      "TEACHER_PROFILE_INCOMPLETE",
      "The teacher profile is incomplete.",
    );
  }

  return { uid, role: "teacher", name, email, department };
}

function mapAssignment(uid: string, raw: RawTeacherAssignment): TeacherAssignment {
  const id = raw.id.trim();
  const teacherUid = requiredString(raw.data, "teacherUid");
  const subjectId = requiredString(raw.data, "subjectId");
  const batchId = requiredString(raw.data, "batchId");
  const active = raw.data.active;
  const academicYearValue = raw.data.academicYear;

  if (
    id.length === 0 ||
    teacherUid !== uid ||
    subjectId === null ||
    batchId === null ||
    active !== true ||
    (academicYearValue !== undefined && requiredString(raw.data, "academicYear") === null)
  ) {
    throw new TeacherAuthorizationError(
      500,
      "AUTHORIZATION_DATA_INVALID",
      "Teacher authorization data is invalid.",
    );
  }

  const academicYear = requiredString(raw.data, "academicYear");
  return academicYear === null
    ? { id, subjectId, batchId }
    : { id, subjectId, batchId, academicYear };
}

export class TeacherAuthorizationService {
  constructor(
    private readonly tokenVerifier: TokenVerifier,
    private readonly dataStore: TeacherDataStore,
  ) {}

  async authorizeToken(idToken: string): Promise<AuthorizedTeacher> {
    let identity: VerifiedIdentity;
    try {
      identity = await this.tokenVerifier.verifyIdToken(idToken, true);
    } catch (error) {
      throw mapTokenError(error);
    }

    const uid = identity.uid.trim();
    if (uid.length === 0) {
      throw new TeacherAuthorizationError(
        401,
        "INVALID_TOKEN",
        "The Firebase authentication token is invalid.",
      );
    }

    let profile: Record<string, unknown> | null;
    try {
      profile = await this.dataStore.getUserProfile(uid);
    } catch {
      throw new TeacherAuthorizationError(
        503,
        "AUTHORIZATION_SERVICE_UNAVAILABLE",
        "Teacher authorization is temporarily unavailable.",
      );
    }

    if (profile === null) {
      throw new TeacherAuthorizationError(
        403,
        "TEACHER_PROFILE_MISSING",
        "No authorized teacher profile exists for this account.",
      );
    }

    return mapTeacher(uid, profile);
  }

  async getAssignments(uid: string): Promise<readonly TeacherAssignment[]> {
    let rawAssignments: readonly RawTeacherAssignment[];
    try {
      rawAssignments = await this.dataStore.getActiveAssignments(uid);
    } catch {
      throw new TeacherAuthorizationError(
        503,
        "AUTHORIZATION_SERVICE_UNAVAILABLE",
        "Teacher authorization is temporarily unavailable.",
      );
    }

    return rawAssignments.map((assignment) => mapAssignment(uid, assignment));
  }
}
