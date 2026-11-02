import path from "node:path";
import { fileURLToPath } from "node:url";

import type { AppConfig } from "../app.js";

export interface ServerConfig extends AppConfig {
  readonly port: number;
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

export function loadEnvironment(env: NodeJS.ProcessEnv): ServerConfig {
  const staticDirectory = env.STATIC_DIR?.trim()
    ? path.resolve(backendDirectory, env.STATIC_DIR.trim())
    : defaultStaticDirectory;

  return {
    port: parsePort(env.PORT),
    allowedOrigins: parseAllowedOrigins(env.ALLOWED_ORIGINS),
    jsonLimit: env.JSON_LIMIT?.trim() || "100kb",
    staticDirectory,
  };
}
