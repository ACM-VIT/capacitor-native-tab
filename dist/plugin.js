/* capacitor-native-tab */
var capacitorNativeTab = (function (exports, core) {
    'use strict';

    class NativeTabsWeb extends core.WebPlugin {
        tabs = [];
        selectedIndex = -1;
        visible = false;
        async initialize(options) {
            this.tabs = options.tabs ?? [];
            this.selectedIndex = normalizeIndex(options.selectedIndex, this.tabs.length);
            this.visible = true;
        }
        async updateTabs(options) {
            this.tabs = options.tabs ?? [];
            this.selectedIndex = normalizeIndex(this.selectedIndex, this.tabs.length);
        }
        async setSelectedTab(options) {
            this.selectedIndex = normalizeIndex(options.index, this.tabs.length);
            this.emitSelection();
            return { index: this.selectedIndex };
        }
        async deselectAll() {
            this.selectedIndex = -1;
            return { index: this.selectedIndex };
        }
        async getSelectedTab() {
            return { index: this.selectedIndex };
        }
        async hideTabBar() {
            this.visible = false;
        }
        async showTabBar() {
            this.visible = true;
        }
        isVisible() {
            return this.visible;
        }
        emitSelection() {
            const tab = this.tabs[this.selectedIndex];
            if (!tab)
                return;
            void this.notifyListeners("tabSelected", {
                index: this.selectedIndex,
                tab,
            });
        }
    }
    function normalizeIndex(index, size) {
        if (size <= 0)
            return -1;
        if (index === undefined || index < 0 || index >= size)
            return 0;
        return index;
    }

    const NativeTabs = core.registerPlugin("NativeTabs", {
        web: () => Promise.resolve(new NativeTabsWeb()),
    });

    exports.NativeTabs = NativeTabs;

    return exports;

})({}, capacitorExports);
//# sourceMappingURL=plugin.js.map
