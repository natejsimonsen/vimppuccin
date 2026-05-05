export const HOMEROW = "asdfghjkl";

export type LabelInput = {
  /** Stable per-element id used internally. The DOM element itself is fine. */
  id: unknown;
  /** Visible text of the element. May be empty. */
  text: string;
};

export type LabelResult = Map<unknown, string>;

export type LabelOptions = {
  /** RNG in [0, 1). Injectable for tests. */
  random?: () => number;
};

/**
 * Per the design: label = first 2 alpha chars of the element's visible text,
 * lowercased. On collision (a prefix already taken) or insufficient text,
 * fall back to a random unused 2-char label drawn from `asdfghjkl`.
 *
 * First-come wins on prefix claims; iteration order of `inputs` determines
 * priority.
 */
export function generateLabels(
  inputs: LabelInput[],
  opts: LabelOptions = {},
): LabelResult {
  const random = opts.random ?? Math.random;
  const labels: LabelResult = new Map();
  const taken = new Set<string>();
  const needsRandom: unknown[] = [];

  for (const { id, text } of inputs) {
    const prefix = derivePrefix(text);
    if (prefix && !taken.has(prefix)) {
      labels.set(id, prefix);
      taken.add(prefix);
    } else {
      needsRandom.push(id);
    }
  }

  if (needsRandom.length === 0) return labels;

  const pool = buildRandomPool(taken);
  shuffle(pool, random);

  for (const id of needsRandom) {
    const next = pool.pop();
    if (!next) break;
    labels.set(id, next);
    taken.add(next);
  }

  return labels;
}

export function derivePrefix(text: string): string | null {
  const alpha = text.toLowerCase().replace(/[^a-z]/g, "");
  if (alpha.length < 2) return null;
  return alpha.slice(0, 2);
}

function buildRandomPool(taken: Set<string>): string[] {
  const pool: string[] = [];
  for (const a of HOMEROW) {
    for (const b of HOMEROW) {
      const lbl = a + b;
      if (!taken.has(lbl)) pool.push(lbl);
    }
  }
  return pool;
}

function shuffle<T>(arr: T[], random: () => number): void {
  for (let i = arr.length - 1; i > 0; i--) {
    const j = Math.floor(random() * (i + 1));
    const tmp = arr[i] as T;
    arr[i] = arr[j] as T;
    arr[j] = tmp;
  }
}
