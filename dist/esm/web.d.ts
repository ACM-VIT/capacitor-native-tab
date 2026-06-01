import { WebPlugin } from "@capacitor/core";
import type { InitializeOptions, NativeTabsPlugin, UpdateTabsOptions } from "./definitions";
export declare class NativeTabsWeb extends WebPlugin implements NativeTabsPlugin {
    private tabs;
    private selectedIndex;
    private visible;
    initialize(options: InitializeOptions): Promise<void>;
    updateTabs(options: UpdateTabsOptions): Promise<void>;
    setSelectedTab(options: {
        index: number;
    }): Promise<{
        index: number;
    }>;
    deselectAll(): Promise<{
        index: number;
    }>;
    getSelectedTab(): Promise<{
        index: number;
    }>;
    hideTabBar(): Promise<void>;
    showTabBar(): Promise<void>;
    isVisible(): boolean;
    private emitSelection;
}
//# sourceMappingURL=web.d.ts.map