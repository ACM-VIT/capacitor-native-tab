# capacitor-native-tab

Native tab bars for Capacitor apps.

- iOS uses `UITabBar`.
- Android uses Jetpack Compose Material 3 `NavigationBar`.
- Web keeps a no-op stateful fallback so the same API can run outside native shells.

## Install

```sh
npm install capacitor-native-tab
npx cap sync
```

## Usage

```ts
import { NativeTabs, type TabConfig } from "capacitor-native-tab";

const tabs: TabConfig[] = [
  { title: "Home", systemImage: "house.fill", route: "/" },
  { title: "Search", systemImage: "magnifyingglass", route: "/search" },
  { title: "Settings", systemImage: "gearshape.fill", route: "/settings" },
];

await NativeTabs.initialize({ tabs, selectedIndex: 0 });

const listener = await NativeTabs.addListener("tabSelected", ({ tab }) => {
  if (tab.route) {
    // Route with your app's router.
  }
});
```

## Android Icons

Android resolves icons in this order:

1. `androidIcon` drawable name from the host app.
2. A bundled generic vector drawable mapped from common `systemImage` names.
3. A generic circle icon.

```ts
await NativeTabs.initialize({
  tabs: [
    {
      title: "Inbox",
      systemImage: "tray.fill",
      androidIcon: "ic_tab_inbox",
      route: "/inbox",
    },
  ],
});
```

## Theme

Pass `theme: "light"` or `theme: "dark"` to force the native tab bar theme. If omitted, the platform theme is used.

## License

MIT. Third-party notices are listed in `THIRD_PARTY_NOTICES.md`.
