import type { PluginListenerHandle } from "@capacitor/core";
export type NativeTabsTheme = "light" | "dark";
export interface NativeTabsPlugin {
    initialize(options: InitializeOptions): Promise<void>;
    updateTabs(options: UpdateTabsOptions): Promise<void>;
    setSelectedTab(options: {
        index: number;
    }): Promise<{
        index: number;
    } | void>;
    deselectAll(): Promise<{
        index: number;
    } | void>;
    getSelectedTab(): Promise<{
        index: number;
    }>;
    hideTabBar(): Promise<void>;
    showTabBar(): Promise<void>;
    addListener(eventName: "tabSelected", listenerFunc: (info: TabSelectedEvent) => void): Promise<PluginListenerHandle>;
    removeAllListeners(): Promise<void>;
}
export interface TabConfig {
    title: string;
    systemImage: string;
    badge?: string;
    role?: "search" | "default";
    route?: string;
    customData?: unknown;
    /**
     * Android drawable resource name, resolved from the host app first.
     * If omitted, the Android implementation maps common SF Symbol names
     * to bundled generic vector drawables.
     */
    androidIcon?: string;
    /**
     * Optional native theme override. If omitted, the native platform theme is used.
     */
    theme?: NativeTabsTheme;
}
export interface InitializeOptions {
    tabs: TabConfig[];
    selectedIndex?: number;
    theme?: NativeTabsTheme;
}
export interface UpdateTabsOptions {
    tabs: TabConfig[];
    theme?: NativeTabsTheme;
}
export interface TabSelectedEvent {
    index: number;
    tab: TabConfig;
}
//# sourceMappingURL=definitions.d.ts.map