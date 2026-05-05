import { describe, expect, test } from "vitest";
import { generateLabels, derivePrefix, HOMEROW } from "./label";

const seq = (vals: number[]) => {
  let i = 0;
  return () => vals[i++ % vals.length] ?? 0;
};

describe("derivePrefix", () => {
  test("returns lowercased first 2 alpha chars", () => {
    expect(derivePrefix("About")).toBe("ab");
    expect(derivePrefix("CONTACT")).toBe("co");
  });

  test("strips non-alphabetic characters", () => {
    expect(derivePrefix("→ next")).toBe("ne");
    expect(derivePrefix("1. Item")).toBe("it");
    expect(derivePrefix("  Hello")).toBe("he");
  });

  test("returns null when fewer than 2 alpha chars", () => {
    expect(derivePrefix("")).toBeNull();
    expect(derivePrefix("a")).toBeNull();
    expect(derivePrefix("→")).toBeNull();
    expect(derivePrefix("123")).toBeNull();
  });
});

describe("generateLabels", () => {
  test("uses first-2-letter prefix when unique", () => {
    const result = generateLabels([
      { id: "a", text: "About" },
      { id: "b", text: "Contact" },
      { id: "c", text: "Profile" },
    ]);
    expect(result.get("a")).toBe("ab");
    expect(result.get("b")).toBe("co");
    expect(result.get("c")).toBe("pr");
  });

  test("first-come-wins on collision; loser gets random label", () => {
    const result = generateLabels(
      [
        { id: "first", text: "Apple" },
        { id: "second", text: "April" },
      ],
      { random: seq([0, 0, 0, 0, 0]) },
    );
    expect(result.get("first")).toBe("ap");
    const loser = result.get("second");
    expect(loser).toBeDefined();
    expect(loser).not.toBe("ap");
    expect(loser).toMatch(/^[asdfghjkl]{2}$/);
  });

  test("element with empty text gets a random label", () => {
    const result = generateLabels(
      [{ id: "icon", text: "" }],
      { random: seq([0]) },
    );
    const lbl = result.get("icon");
    expect(lbl).toMatch(/^[asdfghjkl]{2}$/);
  });

  test("derived prefix that happens to be a home-row pair is excluded from random pool", () => {
    // 'asdf' → prefix 'as' — taken. 'asphalt' must not also receive 'as'.
    const result = generateLabels(
      [
        { id: "x", text: "asdf" },
        { id: "y", text: "asphalt" },
      ],
      { random: seq([0]) },
    );
    expect(result.get("x")).toBe("as");
    expect(result.get("y")).not.toBe("as");
    expect(result.get("y")).toMatch(/^[asdfghjkl]{2}$/);
  });

  test("all labels are unique across the result", () => {
    const inputs = Array.from({ length: 20 }, (_, i) => ({
      id: i,
      text: i < 10 ? `Link ${i}` : "",
    }));
    const result = generateLabels(inputs, { random: Math.random });
    const labels = [...result.values()];
    expect(new Set(labels).size).toBe(labels.length);
  });

  test("runs out of labels gracefully when more than 81+derived candidates", () => {
    const inputs = Array.from({ length: 200 }, (_, i) => ({
      id: i,
      text: "",
    }));
    const result = generateLabels(inputs, { random: Math.random });
    // Random pool has 81 home-row pairs at most.
    expect(result.size).toBeLessThanOrEqual(HOMEROW.length * HOMEROW.length);
    const labels = [...result.values()];
    expect(new Set(labels).size).toBe(labels.length);
  });

  test("leading non-alpha is skipped (numbered list items, emoji prefix)", () => {
    const result = generateLabels([
      { id: "1", text: "1. First item" },
      { id: "2", text: "2. Second item" },
    ]);
    expect(result.get("1")).toBe("fi");
    expect(result.get("2")).toBe("se");
  });
});
