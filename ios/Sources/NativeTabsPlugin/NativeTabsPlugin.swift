import Foundation
import Capacitor
import UIKit
import WebKit

/**
 * NativeTabs Plugin
 * Provides a native iOS UITabBar overlay that integrates with Capacitor's webview
 *
 * Key points:
 * - Does NOT replace the root view controller (keeps CAPBridgeViewController)
 * - Uses ONE WKWebView; tab bar is UI-only
 * - Tab selection is forwarded to JS via notifyListeners
 */
@objc(NativeTabsPlugin)
public class NativeTabsPlugin: CAPPlugin, CAPBridgedPlugin {
    public let identifier = "NativeTabsPlugin"
    public let jsName = "NativeTabs"
    public let pluginMethods: [CAPPluginMethod] = [
        CAPPluginMethod(name: "initialize", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "updateTabs", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "setSelectedTab", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "deselectAll", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "getSelectedTab", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "hideTabBar", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "showTabBar", returnType: CAPPluginReturnPromise),
    ]

    private var tabBarController: NativeTabBarController?

    // MARK: - Public plugin methods

    @objc func initialize(_ call: CAPPluginCall) {
        guard let tabsArray = call.getArray("tabs", JSObject.self) else {
            call.reject("tabs parameter is required")
            return
        }

        let tabs = tabsArray.compactMap { TabConfig(from: $0) }
        let selectedIndex = call.getInt("selectedIndex") ?? 0

        DispatchQueue.main.async {
            guard let bridge = self.bridge,
                  let hostVC = bridge.viewController else {
                call.reject("Bridge not available")
                return
            }

            if self.tabBarController == nil {
                self.tabBarController = NativeTabBarController(hostViewController: hostVC, plugin: self)
                self.tabBarController?.attach()
            }

            self.tabBarController?.updateTabs(tabs)
            self.tabBarController?.setSelectedTab(selectedIndex)

            call.resolve()
        }
    }

    @objc func updateTabs(_ call: CAPPluginCall) {
        guard let tabsArray = call.getArray("tabs", JSObject.self) else {
            call.reject("tabs parameter is required")
            return
        }

        let tabs = tabsArray.compactMap { TabConfig(from: $0) }

        DispatchQueue.main.async {
            self.tabBarController?.updateTabs(tabs)
            call.resolve()
        }
    }

    @objc func setSelectedTab(_ call: CAPPluginCall) {
        guard let index = call.getInt("index") else {
            call.reject("index parameter is required")
            return
        }

        DispatchQueue.main.async {
            self.tabBarController?.setSelectedTab(index)
            call.resolve()
        }
    }

    @objc func deselectAll(_ call: CAPPluginCall) {
        DispatchQueue.main.async {
            self.tabBarController?.deselectAll()
            call.resolve()
        }
    }

    @objc func getSelectedTab(_ call: CAPPluginCall) {
        DispatchQueue.main.async {
            let index = self.tabBarController?.getSelectedTab() ?? 0
            call.resolve(["index": index])
        }
    }

    @objc func hideTabBar(_ call: CAPPluginCall) {
        DispatchQueue.main.async {
            self.tabBarController?.hideTabBar()
            call.resolve()
        }
    }

    @objc func showTabBar(_ call: CAPPluginCall) {
        DispatchQueue.main.async {
            self.tabBarController?.showTabBar()
            call.resolve()
        }
    }

    // MARK: - Called when tab is selected from native UI

    func notifyTabSelected(index: Int, tab: TabConfig) {
        notifyListeners("tabSelected", data: [
            "index": index,
            "tab": tab.toJSObject()
        ])
    }
}

// MARK: - TabConfig Model

struct TabConfig {
    let title: String
    let systemImage: String
    let badge: String?
    let role: String?
    let route: String?
    let customData: [String: Any]?

    init?(from jsObject: JSObject) {
        guard let title = jsObject["title"] as? String,
              let systemImage = jsObject["systemImage"] as? String else {
            return nil
        }

        self.title = title
        self.systemImage = systemImage

        // badge:
        // - if missing => nil
        // - if empty string => treat as nil (optional)
        if let badge = jsObject["badge"] as? String, !badge.isEmpty {
            self.badge = badge
        } else {
            self.badge = nil
        }

        self.role = jsObject["role"] as? String
        self.route = jsObject["route"] as? String
        self.customData = jsObject["customData"] as? [String: Any]
    }

    func toJSObject() -> JSObject {
        var obj: JSObject = [
            "title": title,
            "systemImage": systemImage
        ]
        if let badge = badge { obj["badge"] = badge }
        if let role = role { obj["role"] = role }
        if let route = route { obj["route"] = route }
        if let customData = customData as? JSObject { obj["customData"] = customData }
        return obj
    }
}
