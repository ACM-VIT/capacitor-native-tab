import { WebPlugin } from "@capacitor/core";

import type {
  InitializeOptions,
  NativeTabsPlugin,
  TabConfig,
  UpdateTabsOptions,
} from "./definitions";

export class NativeTabsWeb extends WebPlugin implements NativeTabsPlugin {
  private tabs: TabConfig[] = [];
  private selectedIndex = -1;
  private visible = false;

  async initialize(options: InitializeOptions): Promise<void> {
    this.tabs = options.tabs ?? [];
    this.selectedIndex = normalizeIndex(options.selectedIndex, this.tabs.length);
    this.visible = true;
  }

  async updateTabs(options: UpdateTabsOptions): Promise<void> {
    this.tabs = options.tabs ?? [];
    this.selectedIndex = normalizeIndex(this.selectedIndex, this.tabs.length);
  }

  async setSelectedTab(options: { index: number }): Promise<{ index: number }> {
    this.selectedIndex = normalizeIndex(options.index, this.tabs.length);
    this.emitSelection();
    return { index: this.selectedIndex };
  }

  async deselectAll(): Promise<{ index: number }> {
    this.selectedIndex = -1;
    return { index: this.selectedIndex };
  }

  async getSelectedTab(): Promise<{ index: number }> {
    return { index: this.selectedIndex };
  }

  async hideTabBar(): Promise<void> {
    this.visible = false;
  }

  async showTabBar(): Promise<void> {
    this.visible = true;
  }

  isVisible(): boolean {
    return this.visible;
  }

  private emitSelection() {
    const tab = this.tabs[this.selectedIndex];
    if (!tab) return;

    void this.notifyListeners("tabSelected", {
      index: this.selectedIndex,
      tab,
    });
  }
}

function normalizeIndex(index: number | undefined, size: number) {
  if (size <= 0) return -1;
  if (index === undefined || index < 0 || index >= size) return 0;
  return index;
}
