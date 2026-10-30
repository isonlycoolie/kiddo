// SVG lets arc flags run together ("a14 14 0 01-4.45 3"); Compose's PathParser reads "01" as one number and draws the
// shape wrong. This rewrites every path with each number and flag separated by a space, so any parser reads it right.
const ARGS = { m: 2, l: 2, h: 1, v: 1, c: 6, s: 4, q: 4, t: 2, a: 7, z: 0 };

export function normalizePath(d) {
  const out = [];
  let i = 0;
  const skip = () => {
    while (i < d.length && /[\s,]/.test(d[i])) i++;
  };
  const number = () => {
    skip();
    const m = /^[+-]?(\d+\.?\d*|\.\d+)([eE][+-]?\d+)?/.exec(d.slice(i));
    if (!m) throw new Error(`bad path near "${d.slice(i, i + 12)}"`);
    i += m[0].length;
    return m[0];
  };
  const flag = () => {
    skip();
    const f = d[i];
    if (f !== "0" && f !== "1") throw new Error(`bad arc flag near "${d.slice(i, i + 12)}"`);
    i++;
    return f;
  };
  let command = null;
  while (true) {
    skip();
    if (i >= d.length) break;
    if (/[a-zA-Z]/.test(d[i])) {
      command = d[i++];
      out.push(command);
      if (command.toLowerCase() === "z") continue;
    } else if (command === null) {
      throw new Error("path starts without a command");
    }
    const lower = command.toLowerCase();
    if (lower === "a") {
      out.push(number(), number(), number(), flag(), flag(), number(), number());
    } else {
      for (let k = 0; k < ARGS[lower]; k++) out.push(number());
    }
  }
  return out.join(" ");
}
