import { registerPlugin } from "@capacitor/core";
import { NativeTabsWeb } from "./web";
const NativeTabs = registerPlugin("NativeTabs", {
    web: () => Promise.resolve(new NativeTabsWeb()),
});
export * from "./definitions";
export { NativeTabs };
//# sourceMappingURL=index.js.map