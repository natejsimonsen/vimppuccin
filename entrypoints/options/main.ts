import { loadSettings, saveSettings } from "../../src/storage";
import { flavorToCssVars } from "../../src/theme/apply";
import {
  FLAVOR_NAMES,
  type FlavorName,
  DEFAULT_FLAVOR,
} from "../../src/theme/flavors";
import { KEYMAP } from "../../src/help";

function applyTheme(name: FlavorName): void {
  let style = document.getElementById("vp-flavor-style") as HTMLStyleElement | null;
  if (!style) {
    style = document.createElement("style");
    style.id = "vp-flavor-style";
    document.head.appendChild(style);
  }
  style.textContent = flavorToCssVars(name);
}

function isFlavorName(v: string): v is FlavorName {
  return (FLAVOR_NAMES as string[]).includes(v);
}

async function main(): Promise<void> {
  const select = document.getElementById("flavor") as HTMLSelectElement;
  const dl = document.getElementById("keymap") as HTMLDListElement;

  for (const [keys, desc] of KEYMAP) {
    const dt = document.createElement("dt");
    dt.textContent = keys;
    const dd = document.createElement("dd");
    dd.textContent = desc;
    dl.appendChild(dt);
    dl.appendChild(dd);
  }

  const settings = await loadSettings();
  const initial: FlavorName = settings.flavor ?? DEFAULT_FLAVOR;
  select.value = initial;
  applyTheme(initial);

  select.addEventListener("change", async () => {
    const v = select.value;
    if (!isFlavorName(v)) return;
    applyTheme(v);
    await saveSettings({ flavor: v });
  });
}

void main();
