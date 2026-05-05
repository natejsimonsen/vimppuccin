export type ActivateMode = "click" | "newTab";

export function activate(el: HTMLElement, mode: ActivateMode): void {
  if (mode === "newTab") {
    if (el instanceof HTMLAnchorElement && el.href) {
      window.open(el.href, "_blank", "noopener,noreferrer");
      return;
    }
    // Fall through to click for non-anchor elements
  }
  // Synthesize a click that mirrors a real one. Some sites listen to
  // mousedown/mouseup, so dispatch the trio.
  const opts: MouseEventInit = {
    bubbles: true,
    cancelable: true,
    view: window,
    button: 0,
  };
  el.dispatchEvent(new MouseEvent("mousedown", opts));
  el.dispatchEvent(new MouseEvent("mouseup", opts));
  el.dispatchEvent(new MouseEvent("click", opts));
  // For form controls, focus is the right primary action
  if (
    el instanceof HTMLInputElement ||
    el instanceof HTMLTextAreaElement ||
    el instanceof HTMLSelectElement ||
    el.isContentEditable
  ) {
    el.focus();
  }
}
