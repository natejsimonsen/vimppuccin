export type Mode = "normal" | "insert" | "hint" | "help";

export function isEditableTarget(target: EventTarget | null): boolean {
  if (!(target instanceof HTMLElement)) return false;
  if (target instanceof HTMLInputElement) {
    const type = (target.type ?? "").toLowerCase();
    const passthrough = new Set([
      "text",
      "search",
      "email",
      "url",
      "tel",
      "password",
      "number",
      "date",
      "datetime-local",
      "month",
      "time",
      "week",
    ]);
    return passthrough.has(type);
  }
  if (target instanceof HTMLTextAreaElement) return true;
  if (target instanceof HTMLSelectElement) return true;
  if (target.isContentEditable) return true;
  return false;
}
