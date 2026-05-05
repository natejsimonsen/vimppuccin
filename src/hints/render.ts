import { flavorToCssVars } from "../theme/apply";
import type { FlavorName } from "../theme/flavors";

const HINT_CSS = `
.vp-hint {
  position: absolute;
  z-index: 2147483647;
  font-family: ui-monospace, "SF Mono", "Menlo", "Cascadia Code", monospace;
  font-size: 11px;
  font-weight: 700;
  line-height: 1;
  padding: 2px 4px;
  border-radius: 3px;
  background: var(--ctp-yellow);
  color: var(--ctp-base);
  border: 1px solid var(--ctp-overlay0);
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.35);
  pointer-events: none;
  text-transform: uppercase;
  letter-spacing: 0.5px;
  white-space: nowrap;
}
.vp-hint.vp-hint--matched-prefix {
  background: var(--ctp-peach);
}
.vp-hint > .vp-hint__matched {
  color: var(--ctp-overlay0);
}
.vp-hud {
  position: fixed;
  bottom: 8px;
  right: 8px;
  z-index: 2147483647;
  font-family: ui-monospace, "SF Mono", "Menlo", monospace;
  font-size: 12px;
  padding: 4px 8px;
  border-radius: 4px;
  background: var(--ctp-mantle);
  color: var(--ctp-text);
  border: 1px solid var(--ctp-surface1);
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.3);
  pointer-events: none;
}
.vp-help {
  position: fixed;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  z-index: 2147483647;
  font-family: ui-monospace, "SF Mono", "Menlo", monospace;
  font-size: 13px;
  padding: 16px 20px;
  min-width: 360px;
  max-width: 80vw;
  border-radius: 6px;
  background: var(--ctp-mantle);
  color: var(--ctp-text);
  border: 1px solid var(--ctp-surface1);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.5);
}
.vp-help h2 {
  margin: 0 0 8px;
  color: var(--ctp-mauve);
  font-size: 14px;
}
.vp-help dl {
  display: grid;
  grid-template-columns: max-content 1fr;
  gap: 4px 16px;
  margin: 0;
}
.vp-help dt {
  color: var(--ctp-yellow);
  font-weight: 700;
}
.vp-help dd {
  margin: 0;
  color: var(--ctp-subtext1);
}
.vp-help footer {
  margin-top: 12px;
  font-size: 11px;
  color: var(--ctp-subtext0);
}
`;

export type HintNode = {
  element: HTMLElement;
  label: string;
};

export class Overlay {
  private host: HTMLElement | null = null;
  private shadow: ShadowRoot | null = null;
  private themeStyle: HTMLStyleElement | null = null;

  ensure(): ShadowRoot {
    if (this.shadow) return this.shadow;
    this.host = document.createElement("div");
    this.host.id = "vimppuccin-host";
    this.host.style.cssText =
      "all: initial; position: fixed; top: 0; left: 0; width: 0; height: 0;";
    this.shadow = this.host.attachShadow({ mode: "closed" });

    const baseStyle = document.createElement("style");
    baseStyle.textContent = HINT_CSS;
    this.shadow.appendChild(baseStyle);

    this.themeStyle = document.createElement("style");
    this.shadow.appendChild(this.themeStyle);

    document.documentElement.appendChild(this.host);
    return this.shadow;
  }

  setFlavor(flavor: FlavorName): void {
    this.ensure();
    if (this.themeStyle) {
      this.themeStyle.textContent = flavorToCssVars(flavor);
    }
  }

  renderHints(hints: HintNode[], typed: string): void {
    const root = this.ensure();
    this.clearHints();
    const layer = document.createElement("div");
    layer.className = "vp-hint-layer";
    for (const { element, label } of hints) {
      if (!label.startsWith(typed)) continue;
      const rect = element.getBoundingClientRect();
      const node = document.createElement("div");
      node.className =
        "vp-hint" + (typed.length > 0 ? " vp-hint--matched-prefix" : "");
      const left = window.scrollX + rect.left;
      const top = window.scrollY + rect.top;
      node.style.left = left + "px";
      node.style.top = Math.max(top - 14, 0) + "px";
      if (typed && label.startsWith(typed)) {
        const matched = document.createElement("span");
        matched.className = "vp-hint__matched";
        matched.textContent = typed;
        const rest = document.createElement("span");
        rest.textContent = label.slice(typed.length);
        node.appendChild(matched);
        node.appendChild(rest);
      } else {
        node.textContent = label;
      }
      layer.appendChild(node);
    }
    root.appendChild(layer);
  }

  clearHints(): void {
    if (!this.shadow) return;
    this.shadow.querySelectorAll(".vp-hint-layer").forEach((n) => n.remove());
  }

  showHud(text: string): void {
    const root = this.ensure();
    this.clearHud();
    const hud = document.createElement("div");
    hud.className = "vp-hud";
    hud.textContent = text;
    root.appendChild(hud);
  }

  clearHud(): void {
    if (!this.shadow) return;
    this.shadow.querySelectorAll(".vp-hud").forEach((n) => n.remove());
  }

  showHelp(rows: Array<[string, string]>): void {
    const root = this.ensure();
    this.clearHelp();
    const wrap = document.createElement("div");
    wrap.className = "vp-help";
    const h2 = document.createElement("h2");
    h2.textContent = "Vimppuccin — keymap";
    wrap.appendChild(h2);
    const dl = document.createElement("dl");
    for (const [keys, desc] of rows) {
      const dt = document.createElement("dt");
      dt.textContent = keys;
      const dd = document.createElement("dd");
      dd.textContent = desc;
      dl.appendChild(dt);
      dl.appendChild(dd);
    }
    wrap.appendChild(dl);
    const footer = document.createElement("footer");
    footer.textContent = "Press any key to dismiss.";
    wrap.appendChild(footer);
    root.appendChild(wrap);
  }

  clearHelp(): void {
    if (!this.shadow) return;
    this.shadow.querySelectorAll(".vp-help").forEach((n) => n.remove());
  }

  clearAll(): void {
    this.clearHints();
    this.clearHud();
    this.clearHelp();
  }
}
