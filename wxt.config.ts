import { defineConfig } from "wxt";

export default defineConfig({
  manifest: {
    name: "Vimppuccin",
    description:
      "Vim-mode browser extension with Catppuccin colors. Zero telemetry.",
    permissions: ["storage"],
    host_permissions: ["<all_urls>"],
    content_security_policy: {
      extension_pages: "script-src 'self'; object-src 'self'",
    },
  },
  runner: {
    disabled: true,
  },
});
