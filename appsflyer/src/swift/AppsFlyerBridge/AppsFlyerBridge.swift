import Foundation
import UIKit
import AppsFlyerLib

@objcMembers
public class AppsFlyerBridge: NSObject, AppsFlyerLibDelegate, AppsFlyerDeepLinkDelegate {
    private var onConversion: ((NSDictionary) -> Void)?
    private var onConversionError: ((String?) -> Void)?
    private var onDeepLinkFound: ((String?, Bool, String?, String?, NSDictionary?) -> Void)?
    private var onDeepLinkNotFound: (() -> Void)?
    private var onDeepLinkError: ((String?) -> Void)?
    private var onStart: ((Bool, Int, String?) -> Void)?

    public func configure(
        devKey: String,
        appId: String,
        isDebug: Bool,
        onConversion: @escaping (NSDictionary) -> Void,
        onConversionError: @escaping (String?) -> Void,
        onDeepLinkFound: @escaping (String?, Bool, String?, String?, NSDictionary?) -> Void,
        onDeepLinkNotFound: @escaping () -> Void,
        onDeepLinkError: @escaping (String?) -> Void,
        onStart: @escaping (Bool, Int, String?) -> Void
    ) {
        self.onConversion = onConversion
        self.onConversionError = onConversionError
        self.onDeepLinkFound = onDeepLinkFound
        self.onDeepLinkNotFound = onDeepLinkNotFound
        self.onDeepLinkError = onDeepLinkError
        self.onStart = onStart
        AppsFlyerLib.shared().initialize(devKey: devKey, appId: appId)
        AppsFlyerLib.shared().delegate = self
        AppsFlyerLib.shared().deepLinkDelegate = self
        AppsFlyerLib.shared().isDebug = isDebug

        AppsFlyerLib.shared().start(completionHandler: { (dictionary, error) in
            if let error = error {
                let code = (error as NSError).code
                self.onStart?(false, code, error.localizedDescription)
            } else {
                self.onStart?(true, 0, nil)
            }
        })
    }

    public func setCustomerUserId(_ id: String?) {
        AppsFlyerLib.shared().customerUserID = id
    }

    public func getAppsFlyerUID() -> String {
        return AppsFlyerLib.shared().getAppsFlyerUID()
    }

    public func stop(_ stop: Bool) {
        AppsFlyerLib.shared().isStopped = stop
    }

    public func isStopped() -> Bool {
        return AppsFlyerLib.shared().isStopped
    }

    public func anonymizeUser(_ shouldAnonymize: Bool) {
        AppsFlyerLib.shared().anonymizeUser = shouldAnonymize
    }

    public func setConsentData(
        isUserSubjectToGDPR: NSNumber?,
        hasConsentForDataUsage: NSNumber?,
        hasConsentForAdsPersonalization: NSNumber?,
        hasConsentForAdStorage: NSNumber?
    ) {
        let afConsent = AppsFlyerConsent(
            isUserSubjectToGDPR: isUserSubjectToGDPR,
            hasConsentForDataUsage: hasConsentForDataUsage,
            hasConsentForAdsPersonalization: hasConsentForAdsPersonalization,
            hasConsentForAdStorage: hasConsentForAdStorage
        )
        AppsFlyerLib.shared().setConsentData(afConsent)
    }

    public func enableTCFDataCollection(_ enabled: Bool) {
        AppsFlyerLib.shared().enableTCFDataCollection(enabled)
    }

    public func logEvent(_ name: String, values: NSDictionary?) {
        let dict = values as? [AnyHashable: Any] ?? [:]
        AppsFlyerLib.shared().logEvent(name, withValues: dict)
    }

    public func handleOpenUrl(_ url: URL, options: NSDictionary?) {
        let opts = options as? [UIApplication.OpenURLOptionsKey: Any] ?? [:]
        AppsFlyerLib.shared().handleOpen(url, options: opts)
    }

    public func handleUniversalLink(_ userActivity: NSUserActivity) {
        AppsFlyerLib.shared().continue(userActivity, restorationHandler: nil)
    }

    // MARK: AppsFlyerLibDelegate

    public func onConversionDataSuccess(_ conversionInfo: [AnyHashable: Any]) {
        onConversion?(conversionInfo as NSDictionary)
    }

    public func onConversionDataFail(_ error: Error) {
        onConversionError?(error.localizedDescription)
    }

    // MARK: AppsFlyerDeepLinkDelegate

    public func didResolveDeepLink(_ result: DeepLinkResult) {
        switch result.status {
        case .found:
            let deepLink = result.deepLink
            onDeepLinkFound?(
                deepLink?.deeplinkValue,
                deepLink?.isDeferred ?? false,
                deepLink?.mediaSource,
                deepLink?.campaign,
                deepLink?.clickEvent as NSDictionary?
            )
        case .notFound:
            onDeepLinkNotFound?()
        case .failure:
            onDeepLinkError?(result.error?.localizedDescription)
        @unknown default:
            onDeepLinkError?("Unknown deep link status")
        }
    }
}
