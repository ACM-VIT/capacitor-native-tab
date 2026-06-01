import { registerPlugin } from "@capacitor/core";

import type { NativeTabsPlugin } from "./definitions";
import { NativeTabsWeb } from "./web";

const NativeTabs = registerPlugin<NativeTabsPlugin>("NativeTabs", {
  web: () => Promise.resolve(new NativeTabsWeb()),
});

export * from "./definitions";
export { NativeTabs };
