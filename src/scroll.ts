export const SCROLL_STEP = 60;
export const SCROLL_PAGE_RATIO = 0.9;

export function scrollBy(dx: number, dy: number): void {
  window.scrollBy({ left: dx, top: dy, behavior: "auto" });
}

export function scrollHalfPage(direction: 1 | -1): void {
  scrollBy(0, direction * window.innerHeight * 0.5);
}

export function scrollTop(): void {
  window.scrollTo({ top: 0, behavior: "auto" });
}

export function scrollBottom(): void {
  window.scrollTo({
    top: document.documentElement.scrollHeight,
    behavior: "auto",
  });
}
