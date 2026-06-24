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

    public func setMinTimeBetweenSessions(_ seconds: Int) {
        AppsFlyerLib.shared().minTimeBetweenSessions = UInt(seconds)
    }

    public func setDisableAdvertisingIdentifier(_ disable: Bool) {
        AppsFlyerLib.shared().disableAdvertisingIdentifier = disable
    }

    public func setDisableSKAdNetwork(_ disable: Bool) {
        AppsFlyerLib.shared().disableSKAdNetwork = disable
    }

    public func setUserEmails(_ emails: [String]?, cryptType: Int) {
        let type: EmailCryptType
        switch cryptType {
        case 0: type = EmailCryptTypeNone
        case 3: type = EmailCryptTypeSHA256
        default: type = EmailCryptTypeNone
        }
        AppsFlyerLib.shared().setUserEmails(emails, with: type)
    }

    public func registerUninstall(_ token: Data) {
        AppsFlyerLib.shared().registerUninstall(token)
    }

    public func setOneLinkCustomDomains(_ domains: [String]) {
        AppsFlyerLib.shared().oneLinkCustomDomains = domains
    }

    public func appendParametersToDeepLinkingURL(_ contains: String, parameters: [String: String]) {
        AppsFlyerLib.shared().appendParametersToDeepLinkingURL(contains: contains, parameters: parameters)
    }

    public func setPartnerData(_ partnerId: String, data: [AnyHashable: Any]?) {
        let stringData = data as? [String: Any]
        AppsFlyerLib.shared().setPartnerData(partnerId: partnerId, data: stringData)
    }

    public func addPushNotificationDeepLinkPath(_ deepLinkPath: [String]) {
        AppsFlyerLib.shared().addPushNotificationDeepLinkPath(deepLinkPath)
    }

    public func setResolveDeepLinkURLs(_ urls: [String]) {
        AppsFlyerLib.shared().resolveDeepLinkURLs = urls
    }

    public func setHost(_ hostPrefix: String, hostName: String) {
        AppsFlyerLib.shared().setHost(hostPrefix, hostName: hostName)
    }

    public func getHostName() -> String {
        return AppsFlyerLib.shared().host ?? ""
    }

    public func getHostPrefix() -> String {
        return AppsFlyerLib.shared().hostPrefix ?? ""
    }

    public func setAppInviteOneLink(_ oneLinkId: String) {
        AppsFlyerLib.shared().appInviteOneLinkID = oneLinkId
    }

    public func setPhoneNumber(_ phoneNumber: String?) {
        AppsFlyerLib.shared().phoneNumber = phoneNumber
    }

    public func performOnAppAttribution(_ urlString: String) {
        if let url = URL(string: urlString) {
            AppsFlyerLib.shared().performOnAppAttribution(with: url)
        }
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

    public func validateAndLogInAppPurchase(
        _ productId: String,
        transactionId: String,
        purchaseType: Int,
        additionalParameters: [AnyHashable: Any]?,
        completion: @escaping (NSDictionary?, String?) -> Void
    ) {
        let type: AFSDKPurchaseType
        switch purchaseType {
        case 0: type = .subscription
        case 1: type = .oneTimePurchase
        default: type = .oneTimePurchase
        }
        let details = AFSDKPurchaseDetails(
            productId: productId,
            transactionId: transactionId,
            purchaseType: type
        )
        guard let details else {
            completion(nil, "Failed to create purchase details")
            return
        }
        AppsFlyerLib.shared().validateAndLogInAppPurchase(
            purchaseDetails: details,
            purchaseAdditionalDetails: additionalParameters
        ) { response, error in
            if let error = error {
                completion(nil, error.localizedDescription)
            } else {
                completion(response as NSDictionary?, nil)
            }
        }
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
