import SwiftUI
import ComposeApp

class AppDelegate: NSObject, UIApplicationDelegate {
    func application(
        _ application: UIApplication,
        didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]? = nil
    ) -> Bool {
        let devKey = Bundle.main.object(forInfoDictionaryKey: "AF_DEV_KEY") as? String ?? ""
        let appId = Bundle.main.object(forInfoDictionaryKey: "AF_IOS_APP_ID") as? String ?? ""

        AppsFlyer.shared.initialize(
            config: AppsFlyerConfig(
                devKey: devKey,
                isDebug: true,
                iosAppId: appId,
                collectAndroidId: false,
                anonymizeUser: false,
                enableTCFDataCollection: false,
                consentData: nil,
                sharingFilterPartners: []
            ),
            launchOptions: launchOptions
        )
        return true
    }
}

@main
struct AppsFlyerDemoApp: App {
    @UIApplicationDelegateAdaptor(AppDelegate.self) var delegate

    var body: some Scene {
        WindowGroup {
            ContentView()
                .onOpenURL { url in
                    AppsFlyer.shared.linkHandler.handleOpenUrl(url: url)
                }
                .onContinueUserActivity(NSUserActivityTypeBrowsingWeb) { activity in
                    AppsFlyer.shared.linkHandler.handleUserActivity(userActivity: activity)
                }
        }
    }
}
