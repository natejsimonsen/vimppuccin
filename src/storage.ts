import { DEFAULT_FLAVOR, FLAVOR_NAMES, type FlavorName } from "./theme/flavors";

export type Settings = {
  flavor: FlavorName;
};

const DEFAULTS: Settings = {
  flavor: DEFAULT_FLAVOR,
};

const KEY = "settings";

export async function loadSettings(): Promise<Settings> {
  const result = await chrome.storage.local.get(KEY);
  const stored = result[KEY] as Partial<Settings> | undefined;
  return {
    flavor: isFlavorName(stored?.flavor) ? stored.flavor : DEFAULTS.flavor,
  };
}

export async function saveSettings(patch: Partial<Settings>): Promise<void> {
  const current = await loadSettings();
  const next: Settings = { ...current, ...patch };
  await chrome.storage.local.set({ [KEY]: next });
}

export function onSettingsChanged(listener: (s: Settings) => void): () => void {
  const handler = (
    changes: { [key: string]: chrome.storage.StorageChange },
    area: string,
  ) => {
    if (area !== "local" || !changes[KEY]) return;
    void loadSettings().then(listener);
  };
  chrome.storage.onChanged.addListener(handler);
  return () => chrome.storage.onChanged.removeListener(handler);
}

function isFlavorName(v: unknown): v is FlavorName {
  return typeof v === "string" && (FLAVOR_NAMES as string[]).includes(v);
}
