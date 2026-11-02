import "dotenv/config";

import { createApp } from "./app.js";
import { loadEnvironment } from "./config/environment.js";

const config = loadEnvironment(process.env);
const app = createApp(config);

app.listen(config.port, "127.0.0.1", () => {
  console.log(`SmartAttend teacher server listening at http://localhost:${config.port}`);
});
