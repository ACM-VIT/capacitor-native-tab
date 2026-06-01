import UIKit
import Capacitor
import WebKit

final class NativeTabBarController: NSObject, UITabBarDelegate {

    private weak var hostViewController: UIViewController?
    private weak var plugin: NativeTabsPlugin?

    private(set) var tabBar: UITabBar = UITabBar()

    private var tabs: [TabConfig] = []

    private var heightConstraint: NSLayoutConstraint?
    private var bottomConstraint: NSLayoutConstraint?

    private var baseContentInset: UIEdgeInsets = .zero
    private var baseIndicatorInset: UIEdgeInsets = .zero
    private var insetsCaptured = false

    private var isAttached = false
    private var isHidden = false

    init(hostViewController: UIViewController, plugin: NativeTabsPlugin) {
        self.hostViewController = hostViewController
        self.plugin = plugin
        super.init()

        tabBar.delegate = self
        tabBar.translatesAutoresizingMaskIntoConstraints = false

        // iOS 15+ system-like appearance
        let appearance = UITabBarAppearance()
        appearance.configureWithDefaultBackground()
        tabBar.standardAppearance = appearance
        if #available(iOS 15.0, *) {
            tabBar.scrollEdgeAppearance = appearance
        }
    }

    // MARK: - Lifecycle

    func attach() {
        guard !isAttached else { return }
        guard let hostVC = hostViewController else { return }

        hostVC.view.addSubview(tabBar)

        bottomConstraint = tabBar.bottomAnchor.constraint(equalTo: hostVC.view.bottomAnchor)
        let initialHeight = calculatedTabBarHeight()
        heightConstraint = tabBar.heightAnchor.constraint(equalToConstant: initialHeight)

        NSLayoutConstraint.activate([
            tabBar.leadingAnchor.constraint(equalTo: hostVC.view.leadingAnchor),
            tabBar.trailingAnchor.constraint(equalTo: hostVC.view.trailingAnchor),
            bottomConstraint!,
            heightConstraint!
        ])

        isAttached = true

        // Apply insets to webview so content doesn't go under the tab bar
        captureBaseInsetsIfNeeded()
        applyWebViewInsets()

        // Update height again after layout to reflect safeAreaInsets.bottom properly
        DispatchQueue.main.async { [weak self] in
            self?.updateHeightIfNeeded()
            self?.applyWebViewInsets()
        }

        // Optional: update on orientation / foreground changes (simple & effective)
        NotificationCenter.default.addObserver(
            self,
            selector: #selector(environmentChanged),
            name: UIApplication.willEnterForegroundNotification,
            object: nil
        )
        NotificationCenter.default.addObserver(
            self,
            selector: #selector(environmentChanged),
            name: UIDevice.orientationDidChangeNotification,
            object: nil
        )
    }

    func detach() {
        NotificationCenter.default.removeObserver(self)
        tabBar.removeFromSuperview()
        isAttached = false
    }

    deinit {
        NotificationCenter.default.removeObserver(self)
    }

    // MARK: - Public API

    func updateTabs(_ newTabs: [TabConfig]) {
        self.tabs = newTabs

        let items: [UITabBarItem] = newTabs.enumerated().map { idx, tab in
            let item = UITabBarItem(
                title: tab.title,
                image: UIImage(systemName: tab.systemImage),
                tag: idx
            )
            item.badgeValue = tab.badge // nil removes badge
            return item
        }

        tabBar.items = items

        // If selection points to a missing item, reset to first
        if let selected = tabBar.selectedItem, selected.tag < 0 || selected.tag >= items.count {
            tabBar.selectedItem = items.first
        }
    }

    func setSelectedTab(_ index: Int) {
        guard let items = tabBar.items, !items.isEmpty else { return }
        if let item = items.first(where: { $0.tag == index }) {
            tabBar.selectedItem = item
        }
    }

    func deselectAll() {
        tabBar.selectedItem = nil
    }

    func getSelectedTab() -> Int {
        tabBar.selectedItem?.tag ?? -1
    }

    func hideTabBar() {
        guard !isHidden else { return }
        isHidden = true
        tabBar.isHidden = true
        removeWebViewInsets()
    }

    func showTabBar() {
        guard isHidden else { return }
        isHidden = false
        tabBar.isHidden = false
        updateHeightIfNeeded()
        applyWebViewInsets()
    }

    // MARK: - UITabBarDelegate

    func tabBar(_ tabBar: UITabBar, didSelect item: UITabBarItem) {
        let index = item.tag
        guard index >= 0, index < tabs.count else { return }
        plugin?.notifyTabSelected(index: index, tab: tabs[index])
    }

    // MARK: - Insets / Layout helpers

    @objc private func environmentChanged() {
        guard !isHidden else { return }
        updateHeightIfNeeded()
        applyWebViewInsets()
    }

    private func updateHeightIfNeeded() {
        guard let heightConstraint else { return }
        heightConstraint.constant = calculatedTabBarHeight()
        hostViewController?.view.layoutIfNeeded()
    }

    /// Mimic UITabBarController sizing: 49pt + safeAreaInsets.bottom (home indicator)
    private func calculatedTabBarHeight() -> CGFloat {
        guard let hostVC = hostViewController else { return 49 }
        let bottomSafe = hostVC.view.safeAreaInsets.bottom
        return 49 + bottomSafe
    }

    private func bridgeWebView() -> WKWebView? {
        // host could be CAPBridgeViewController or a wrapper; try common paths
        if let cap = hostViewController as? CAPBridgeViewController {
            return cap.webView
        }
        // fallback: walk children
        for child in hostViewController?.children ?? [] {
            if let cap = child as? CAPBridgeViewController {
                return cap.webView
            }
        }
        return nil
    }

    private func captureBaseInsetsIfNeeded() {
        guard !insetsCaptured else { return }
        guard let webView = bridgeWebView() else { return }
        baseContentInset = webView.scrollView.contentInset
        baseIndicatorInset = webView.scrollView.verticalScrollIndicatorInsets
        insetsCaptured = true
    }

    private func applyWebViewInsets() {
        guard !isHidden else { return }
        guard let webView = bridgeWebView() else { return }
        captureBaseInsetsIfNeeded()

        let extra = calculatedTabBarHeight()

        var content = baseContentInset
        content.bottom = max(content.bottom, baseContentInset.bottom + extra)

        var indicators = baseIndicatorInset
        indicators.bottom = max(indicators.bottom, baseIndicatorInset.bottom + extra)

        webView.scrollView.contentInset = content
        webView.scrollView.verticalScrollIndicatorInsets = indicators
    }

    private func removeWebViewInsets() {
        guard let webView = bridgeWebView() else { return }
        guard insetsCaptured else { return }
        webView.scrollView.contentInset = baseContentInset
        webView.scrollView.verticalScrollIndicatorInsets = baseIndicatorInset
    }
}
