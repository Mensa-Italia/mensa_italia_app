import Flutter
import UIKit
import GoogleMaps

@main
@objc class AppDelegate: FlutterAppDelegate {
  override func application(
    _ application: UIApplication,
    didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]?
  ) -> Bool {
    if let mapsApiKey = ProcessInfo.processInfo.environment["MAPS_API_KEY"] {
      GMSServices.provideAPIKey(mapsApiKey)
    } else {
      print("⚠️ MAPS_API_KEY non trovata nelle variabili d'ambiente")
    }
    GeneratedPluginRegistrant.register(with: self)
    return super.application(application, didFinishLaunchingWithOptions: launchOptions)
  }
}
