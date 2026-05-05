import { collectInteractive, visibleText } from "../hints/collect";
import { generateLabels, type LabelInput } from "../hints/label";
import { Overlay, type HintNode } from "../hints/render";
import { activate, type ActivateMode } from "../hints/activate";
import {
  scrollBy,
  scrollHalfPage,
  scrollTop,
  scrollBottom,
  SCROLL_STEP,
} from "../scroll";
import { KEYMAP } from "../help";
import { isEditableTarget, type Mode } from "./types";
import { loadSettings, onSettingsChanged } from "../storage";

export class ModeManager {
  private mode: Mode = "normal";
  private overlay = new Overlay();
  private hints: HintNode[] = [];
  private hintBuffer = "";
  private hintActivateMode: ActivateMode = "click";
  private lastG = 0;

  async init(): Promise<void> {
    const settings = await loadSettings();
    this.overlay.setFlavor(settings.flavor);
    onSettingsChanged((s) => this.overlay.setFlavor(s.flavor));
    document.addEventListener("keydown", this.onKeydown, true);
  }

  destroy(): void {
    document.removeEventListener("keydown", this.onKeydown, true);
    this.overlay.clearAll();
  }

  private onKeydown = (ev: KeyboardEvent): void => {
    // If user is typing in a form field, never intercept (unless we're already
    // in hint mode — hint mode owns keyboard regardless).
    if (this.mode !== "hint" && isEditableTarget(ev.target)) {
      this.mode = "insert";
      return;
    }
    if (this.mode === "insert" && !isEditableTarget(ev.target)) {
      this.mode = "normal";
    }
    if (ev.altKey || ev.ctrlKey || ev.metaKey) return;

    switch (this.mode) {
      case "hint":
        this.handleHintKey(ev);
        return;
      case "help":
        ev.preventDefault();
        ev.stopPropagation();
        this.exitHelp();
        return;
      case "insert":
        return;
      case "normal":
        this.handleNormalKey(ev);
        return;
    }
  };

  private handleNormalKey(ev: KeyboardEvent): void {
    const key = ev.key;
    const consume = () => {
      ev.preventDefault();
      ev.stopPropagation();
    };
    switch (key) {
      case "j":
        consume();
        scrollBy(0, SCROLL_STEP);
        return;
      case "k":
        consume();
        scrollBy(0, -SCROLL_STEP);
        return;
      case "h":
        consume();
        scrollBy(-SCROLL_STEP, 0);
        return;
      case "l":
        consume();
        scrollBy(SCROLL_STEP, 0);
        return;
      case "d":
        consume();
        scrollHalfPage(1);
        return;
      case "u":
        consume();
        scrollHalfPage(-1);
        return;
      case "G":
        consume();
        scrollBottom();
        return;
      case "g":
        consume();
        if (Date.now() - this.lastG < 500) {
          scrollTop();
          this.lastG = 0;
        } else {
          this.lastG = Date.now();
        }
        return;
      case "f":
        consume();
        this.enterHints("click");
        return;
      case "F":
        consume();
        this.enterHints("newTab");
        return;
      case "?":
        consume();
        this.enterHelp();
        return;
      case "i":
        consume();
        this.mode = "insert";
        return;
      case "Escape":
        consume();
        this.overlay.clearAll();
        return;
    }
  }

  private enterHints(mode: ActivateMode): void {
    const elements = collectInteractive();
    if (elements.length === 0) return;
    const inputs: LabelInput[] = elements.map((el) => ({
      id: el,
      text: visibleText(el),
    }));
    const labels = generateLabels(inputs);
    this.hints = [];
    for (const el of elements) {
      const label = labels.get(el);
      if (label) this.hints.push({ element: el, label });
    }
    this.hintBuffer = "";
    this.hintActivateMode = mode;
    this.mode = "hint";
    this.overlay.renderHints(this.hints, "");
    this.overlay.showHud(mode === "newTab" ? "hint → new tab" : "hint");
  }

  private exitHints(): void {
    this.mode = "normal";
    this.hints = [];
    this.hintBuffer = "";
    this.overlay.clearAll();
  }

  private handleHintKey(ev: KeyboardEvent): void {
    ev.preventDefault();
    ev.stopPropagation();
    const key = ev.key;
    if (key === "Escape") {
      this.exitHints();
      return;
    }
    if (key === "Backspace") {
      this.hintBuffer = this.hintBuffer.slice(0, -1);
      this.overlay.renderHints(this.hints, this.hintBuffer);
      return;
    }
    if (!/^[a-z]$/.test(key)) return;
    const next = this.hintBuffer + key;
    const matches = this.hints.filter((h) => h.label.startsWith(next));
    if (matches.length === 0) {
      this.exitHints();
      return;
    }
    if (matches.length === 1 && matches[0]!.label === next) {
      const target = matches[0]!.element;
      this.exitHints();
      activate(target, this.hintActivateMode);
      return;
    }
    this.hintBuffer = next;
    this.overlay.renderHints(this.hints, this.hintBuffer);
  }

  private enterHelp(): void {
    this.mode = "help";
    this.overlay.showHelp(KEYMAP);
  }

  private exitHelp(): void {
    this.mode = "normal";
    this.overlay.clearHelp();
  }
}
