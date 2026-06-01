const banner = "/* capacitor-native-tabs */";

export default [
  {
    input: "dist/esm/index.js",
    external: ["@capacitor/core"],
    output: {
      banner,
      file: "dist/plugin.cjs.js",
      format: "cjs",
      exports: "named",
      sourcemap: true,
    },
  },
  {
    input: "dist/esm/index.js",
    external: ["@capacitor/core"],
    output: {
      banner,
      file: "dist/plugin.js",
      format: "iife",
      globals: {
        "@capacitor/core": "capacitorExports",
      },
      name: "capacitorNativeTabs",
      sourcemap: true,
    },
  },
];
