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
        anonymizeUser: Bool,
        enableTCFDataCollection: Bool,
        consentData: NSDictionary?,
        sharingFilterPartners: [String],
        launchOptions: [AnyHashable: Any]?,
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

        AppsFlyerLib.shared().anonymizeUser = anonymizeUser
        AppsFlyerLib.shared().enableTCFDataCollection(enableTCFDataCollection)
        if let consentDict = consentData {
            let consent = AppsFlyerConsent(
                isUserSubjectToGDPR: consentDict["isUserSubjectToGDPR"] as? NSNumber,
                hasConsentForDataUsage: consentDict["hasConsentForDataUsage"] as? NSNumber,
                hasConsentForAdsPersonalization: consentDict["hasConsentForAdsPersonalization"] as? NSNumber,
                hasConsentForAdStorage: consentDict["hasConsentForAdStorage"] as? NSNumber
            )
            AppsFlyerLib.shared().setConsentData(consent)
        }
        AppsFlyerLib.shared().sharingFilter = sharingFilterPartners

        AppsFlyerLib.shared().handleLaunchOptions(launchOptions)
        AppsFlyerLib.shared().registerSessionReadyListener { [weak self] in
            guard let self = self else { return }
            AppsFlyerLib.shared().start(completionHandler: { (dictionary, error) in
                if let error = error {
                    let code = (error as NSError).code
                    self.onStart?(false, code, error.localizedDescription)
                } else {
                    self.onStart?(true, 0, nil)
                }
            })
        }
    }

    public func setCustomerUserId(_ id: String?) {
        AppsFlyerLib.shared().customerUserID = id
    }

    public func setAnonymizeUser(_ enabled: Bool) {
        AppsFlyerLib.shared().anonymizeUser = enabled
    }

    public func setSharingFilterPartners(_ partners: [String]) {
        AppsFlyerLib.shared().sharingFilter = partners
    }

    public func getAppsFlyerUID() -> String {
        return AppsFlyerLib.shared().getAppsFlyerUID()
    }

    public func getSdkVersion() -> String {
        return AppsFlyerLib.shared().getSdkVersion()
    }

    public func setCurrencyCode(_ currency: String) {
        AppsFlyerLib.shared().currencyCode = currency
    }

    public func logLocation(_ longitude: Double, latitude: Double) {
        AppsFlyerLib.shared().logLocation(longitude: longitude, latitude: latitude)
    }

    public func setCustomData(_ data: [AnyHashable: Any]?) {
        AppsFlyerLib.shared().customData = data
    }

    public func stop(_ stop: Bool) {
        AppsFlyerLib.shared().isStopped = stop
    }

    public func isStopped() -> Bool {
        return AppsFlyerLib.shared().isStopped
    }

    public func logEvent(_ name: String, values: NSDictionary?) {
        let dict = values as? [AnyHashable: Any] ?? [:]
        AppsFlyerLib.shared().logEvent(name, withValues: dict)
    }

    public func logEventForResult(
        _ name: String,
        values: NSDictionary?,
        onResult: @escaping (Bool, Int, String?) -> Void
    ) {
        let dict = (values as? [String: Any]) ?? [:]
        AppsFlyerLib.shared().logEvent(name: name, values: dict, completionHandler: { dictionary, error in
            if let error = error {
                onResult(false, (error as NSError).code, error.localizedDescription)
            } else {
                onResult(true, 0, nil)
            }
        })
    }

    public func handleOpenUrl(_ url: URL, options: NSDictionary?) {
        let opts = options as? [UIApplication.OpenURLOptionsKey: Any] ?? [:]
        AppsFlyerLib.shared().handleOpen(url, options: opts)
    }

    public func handleUniversalLink(_ userActivity: NSUserActivity) {
        AppsFlyerLib.shared().continue(userActivity, restorationHandler: nil)
    }

    public func logAdRevenue(
        _ monetizationNetwork: String,
        mediationNetwork: Int,
        currency: String,
        revenue: Double,
        additionalParameters: NSDictionary?
    ) {
        let mediation = mapMediationNetworkType(mediationNetwork)
        let adRevenueData = AFAdRevenueData(
            monetizationNetwork: monetizationNetwork,
            mediationNetwork: mediation,
            currencyIso4217Code: currency,
            eventRevenue: NSNumber(value: revenue)
        )
        let params = (additionalParameters as? [String: Any]) ?? [:]
        AppsFlyerLib.shared().logAdRevenue(adRevenueData, additionalParameters: params)
    }

    private func mapMediationNetworkType(_ rawValue: Int) -> MediationNetworkType {
        switch rawValue {
        case 0: return .googleAdMob
        case 1: return .ironSource
        case 2: return .applovinMax
        case 3: return .fyber
        case 4: return .appodeal
        case 5: return .admost
        case 6: return .topon
        case 7: return .tradplus
        case 8: return .yandex
        case 9: return .chartBoost
        case 10: return .unity
        case 11: return .toponPte
        case 12: return .custom
        case 13: return .directMonetization
        default: return .googleAdMob
        }
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
