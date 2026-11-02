import type { NextFunction, Request, RequestHandler, Response } from "express";

import {
  TeacherAuthorizationError,
  type AuthorizedTeacher,
  type TeacherAuthorizationService,
} from "../auth/teacher-authorization.js";

export interface TeacherLocals {
  teacher: AuthorizedTeacher;
}

function writeError(
  response: Response,
  statusCode: number,
  code: string,
  message: string,
): void {
  response.status(statusCode).json({ error: { code, message } });
}

export function writeTeacherAuthorizationError(
  response: Response,
  error: unknown,
): void {
  if (error instanceof TeacherAuthorizationError) {
    writeError(response, error.statusCode, error.code, error.message);
    return;
  }

  writeError(
    response,
    503,
    "AUTHORIZATION_SERVICE_UNAVAILABLE",
    "Teacher authorization is temporarily unavailable.",
  );
}

function extractBearerToken(request: Request): string {
  const header = request.get("Authorization");
  if (header === undefined) {
    throw new TeacherAuthorizationError(
      401,
      "AUTHENTICATION_REQUIRED",
      "A Firebase authentication token is required.",
    );
  }

  const match = /^Bearer ([^\s]+)$/i.exec(header);
  if (match === null) {
    throw new TeacherAuthorizationError(
      401,
      "INVALID_AUTHORIZATION_HEADER",
      "Use the Authorization header with one Bearer token.",
    );
  }

  return match[1] ?? "";
}

export function createRequireTeacher(
  service: TeacherAuthorizationService | undefined,
): RequestHandler {
  return async (request: Request, response: Response, next: NextFunction) => {
    if (service === undefined) {
      writeError(
        response,
        503,
        "FIREBASE_NOT_CONFIGURED",
        "Firebase teacher authentication is not configured.",
      );
      return;
    }

    try {
      const token = extractBearerToken(request);
      response.locals.teacher = await service.authorizeToken(token);
      next();
    } catch (error) {
      writeTeacherAuthorizationError(response, error);
    }
  };
}
