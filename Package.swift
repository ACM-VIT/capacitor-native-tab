// swift-tools-version: 5.9
import PackageDescription

let package = Package(
    name: "NativeTabsPlugin",
    platforms: [.iOS(.v15)],
    products: [
        .library(
            name: "NativeTabsPlugin",
            targets: ["NativeTabsPlugin"]),
        .library(
            name: "CapacitorNativeTab",
            targets: ["NativeTabsPlugin"])
    ],
    dependencies: [
        .package(url: "https://github.com/ionic-team/capacitor-swift-pm.git", from: "8.0.0")
    ],
    targets: [
        .target(
            name: "NativeTabsPlugin",
            dependencies: [
                .product(name: "Capacitor", package: "capacitor-swift-pm"),
                .product(name: "Cordova", package: "capacitor-swift-pm")
            ],
            path: "ios/Sources/NativeTabsPlugin")
    ]
)
