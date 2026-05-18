# Info.plist additions required

The new Tableport QR scanner (`Features/Addons/Tableport/QRScannerView.swift`)
relies on `AVCaptureSession`, which requires the user-facing camera usage
description. Add the following entry under `targets.iosApp.info.properties`
in `project.yml` (and regenerate the Xcode project with `xcodegen generate`):

```yaml
        NSCameraUsageDescription: "Mensa Italia usa la fotocamera per scansionare i QR dei francobolli Tableport."
```

Without this key, the app will crash on first `AVCaptureDeviceInput` access.
The scanner code itself does not need any other entitlement.
