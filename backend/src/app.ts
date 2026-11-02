import cors from "cors";
import express, { type ErrorRequestHandler, type Express } from "express";

import type { TeacherAuthorizationService } from "./auth/teacher-authorization.js";
import {
  createRequireTeacher,
  writeTeacherAuthorizationError,
} from "./middleware/require-teacher.js";

export interface AppConfig {
  readonly allowedOrigins: readonly string[];
  readonly staticDirectory: string;
  readonly jsonLimit?: string;
  readonly now?: () => Date;
  readonly teacherAuthorization?: TeacherAuthorizationService;
}

class ApiError extends Error {
  constructor(
    readonly statusCode: number,
    readonly code: string,
    message: string,
  ) {
    super(message);
  }
}

function hasErrorType(error: unknown, expectedType: string): boolean {
  return (
    typeof error === "object" &&
    error !== null &&
    "type" in error &&
    error.type === expectedType
  );
}

export function createApp(config: AppConfig): Express {
  const app = express();
  const now = config.now ?? (() => new Date());

  app.disable("x-powered-by");

  app.use(
    cors({
      origin(origin, callback) {
        if (origin === undefined || config.allowedOrigins.includes(origin)) {
          callback(null, true);
          return;
        }

        callback(
          new ApiError(
            403,
            "ORIGIN_NOT_ALLOWED",
            "This browser origin is not allowed.",
          ),
        );
      },
    }),
  );

  app.use(express.json({ limit: config.jsonLimit ?? "100kb" }));

  app.get("/api/health", (_request, response) => {
    response.status(200).json({
      status: "ok",
      service: "smartattend-teacher-api",
      timestamp: now().toISOString(),
    });
  });

  const requireTeacher = createRequireTeacher(config.teacherAuthorization);

  app.get("/api/me", requireTeacher, (_request, response) => {
    response.status(200).json({ teacher: response.locals.teacher });
  });

  app.get("/api/teacher/assignments", requireTeacher, async (_request, response) => {
    try {
      const assignments = await config.teacherAuthorization!.getAssignments(
        response.locals.teacher.uid,
      );
      response.status(200).json({ assignments });
    } catch (error) {
      writeTeacherAuthorizationError(response, error);
    }
  });

  app.use("/api", (_request, response) => {
    response.status(404).json({
      error: {
        code: "API_ROUTE_NOT_FOUND",
        message: "The requested API route does not exist.",
      },
    });
  });

  app.use(express.static(config.staticDirectory));

  app.use((_request, response) => {
    response.status(404).json({
      error: {
        code: "NOT_FOUND",
        message: "The requested resource does not exist.",
      },
    });
  });

  const errorHandler: ErrorRequestHandler = (error, _request, response, _next) => {
    if (error instanceof ApiError) {
      response.status(error.statusCode).json({
        error: {
          code: error.code,
          message: error.message,
        },
      });
      return;
    }

    if (hasErrorType(error, "entity.parse.failed")) {
      response.status(400).json({
        error: {
          code: "INVALID_JSON",
          message: "The request body must contain valid JSON.",
        },
      });
      return;
    }

    if (hasErrorType(error, "entity.too.large")) {
      response.status(413).json({
        error: {
          code: "PAYLOAD_TOO_LARGE",
          message: "The request body is too large.",
        },
      });
      return;
    }

    response.status(500).json({
      error: {
        code: "INTERNAL_SERVER_ERROR",
        message: "The server could not complete the request.",
      },
    });
  };

  app.use(errorHandler);

  return app;
}
