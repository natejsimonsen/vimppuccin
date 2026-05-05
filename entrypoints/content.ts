import { defineContentScript } from "wxt/sandbox";
import { ModeManager } from "../src/modes/ModeManager";

export default defineContentScript({
  matches: ["<all_urls>"],
  runAt: "document_idle",
  allFrames: false,
  async main() {
    const manager = new ModeManager();
    await manager.init();
  },
});
