import { latte } from "./latte";
import { frappe } from "./frappe";
import { macchiato } from "./macchiato";
import { mocha } from "./mocha";
import type { Flavor, FlavorName } from "./types";

export const FLAVORS: Record<FlavorName, Flavor> = {
  latte,
  frappe,
  macchiato,
  mocha,
};

export const FLAVOR_NAMES: FlavorName[] = [
  "latte",
  "frappe",
  "macchiato",
  "mocha",
];

export const DEFAULT_FLAVOR: FlavorName = "mocha";

export type { Flavor, FlavorName };
