import { FLAVORS, type FlavorName } from "./flavors";

export function flavorToCssVars(name: FlavorName): string {
  const flavor = FLAVORS[name];
  const lines: string[] = [];
  for (const [key, value] of Object.entries(flavor)) {
    if (key === "name") continue;
    lines.push(`  --ctp-${key}: ${value};`);
  }
  return `:host, :root {\n${lines.join("\n")}\n}`;
}
