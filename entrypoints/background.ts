import { defineBackground } from "wxt/sandbox";

export default defineBackground(() => {
  // No persistent background work in v1. The service worker exists only so
  // the extension is fully formed; settings are read directly by content
  // scripts and the options page via chrome.storage.local.
  chrome.runtime.onInstalled.addListener(() => {
    // No-op. Future: open options page on first install.
  });
});
