import "dotenv/config";

import { createApp } from "./app.js";
import { loadEnvironment } from "./config/environment.js";
import { createFirebaseAdminRuntime } from "./firebase/admin-runtime.js";

async function start(): Promise<void> {
  const config = loadEnvironment(process.env);
  const firebase = createFirebaseAdminRuntime(config.firebase);
  const app = createApp(
    firebase === undefined
      ? config
      : { ...config, teacherAuthorization: firebase.teacherAuthorization },
  );

  const server = app.listen(config.port, "127.0.0.1", () => {
    console.log(
      `SmartAttend teacher server listening at http://localhost:${config.port}`,
    );
    console.log(`Firebase teacher authentication mode: ${config.firebase.mode}`);
  });

  async function shutdown(): Promise<void> {
    server.close();
    await firebase?.close();
  }

  process.once("SIGINT", () => void shutdown());
  process.once("SIGTERM", () => void shutdown());
}

void start().catch((error: unknown) => {
  const message = error instanceof Error ? error.message : "Unknown startup error";
  console.error(`SmartAttend teacher server failed to start: ${message}`);
  process.exitCode = 1;
});
