const INTERACTIVE_SELECTOR = [
  "a[href]",
  "button:not([disabled])",
  'input:not([type="hidden"]):not([disabled])',
  "select:not([disabled])",
  "textarea:not([disabled])",
  '[role="button"]',
  '[role="link"]',
  '[role="checkbox"]',
  '[role="radio"]',
  '[role="tab"]',
  '[role="menuitem"]',
  '[role="switch"]',
  '[contenteditable=""]',
  '[contenteditable="true"]',
].join(",");

export function collectInteractive(doc: Document = document): HTMLElement[] {
  const candidates = doc.querySelectorAll<HTMLElement>(INTERACTIVE_SELECTOR);
  const result: HTMLElement[] = [];
  for (const el of candidates) {
    if (isVisible(el)) result.push(el);
  }
  // Also pick up elements with explicit positive tabIndex
  const tabbable = doc.querySelectorAll<HTMLElement>("[tabindex]");
  for (const el of tabbable) {
    if (result.includes(el)) continue;
    const ti = el.tabIndex;
    if (ti >= 0 && isVisible(el)) result.push(el);
  }
  return result;
}

export function isVisible(el: HTMLElement): boolean {
  const rect = el.getBoundingClientRect();
  if (rect.width === 0 || rect.height === 0) return false;
  if (
    rect.bottom < 0 ||
    rect.top > window.innerHeight ||
    rect.right < 0 ||
    rect.left > window.innerWidth
  ) {
    return false;
  }
  const style = window.getComputedStyle(el);
  if (style.visibility === "hidden") return false;
  if (style.display === "none") return false;
  if (parseFloat(style.opacity) === 0) return false;
  return true;
}

export function visibleText(el: HTMLElement): string {
  const aria = el.getAttribute("aria-label");
  if (aria && aria.trim()) return aria.trim();
  const text = (el.textContent ?? "").trim();
  if (text) return text;
  const title = el.getAttribute("title");
  if (title && title.trim()) return title.trim();
  if (el instanceof HTMLInputElement) {
    if (el.placeholder) return el.placeholder;
    if (el.value) return el.value;
    if (el.name) return el.name;
  }
  const img = el.querySelector("img[alt]");
  if (img) {
    const alt = img.getAttribute("alt");
    if (alt && alt.trim()) return alt.trim();
  }
  return "";
}
