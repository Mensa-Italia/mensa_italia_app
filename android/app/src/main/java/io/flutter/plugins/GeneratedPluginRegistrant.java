package io.flutter.plugins;

import io.flutter.plugin.common.PluginRegistry;
import flutter.plugins.contactsservice.contactsservice.ContactsServicePlugin;
import com.pichillilorenzo.flutter_inappwebview.InAppWebViewFlutterPlugin;
import de.ffuf.in_app_update.InAppUpdatePlugin;
import io.flutter.plugins.pathprovider.PathProviderPlugin;
import com.example.pdfviewerplugin.PdfViewerPlugin;
import com.baseflow.permissionhandler.PermissionHandlerPlugin;
import flutter.plugins.screen.screen.ScreenPlugin;
import io.flutter.plugins.sharedpreferences.SharedPreferencesPlugin;
import com.tekartik.sqflite.SqflitePlugin;
import io.flutter.plugins.urllauncher.UrlLauncherPlugin;
import io.flutter.plugins.videoplayer.VideoPlayerPlugin;
import creativecreatorormaybenot.wakelock.WakelockPlugin;
import io.flutter.plugins.webviewflutter.WebViewFlutterPlugin;

/**
 * Generated file. Do not edit.
 */
public final class GeneratedPluginRegistrant {
  public static void registerWith(PluginRegistry registry) {
    if (alreadyRegisteredWith(registry)) {
      return;
    }
    ContactsServicePlugin.registerWith(registry.registrarFor("flutter.plugins.contactsservice.contactsservice.ContactsServicePlugin"));
    InAppWebViewFlutterPlugin.registerWith(registry.registrarFor("com.pichillilorenzo.flutter_inappwebview.InAppWebViewFlutterPlugin"));
    InAppUpdatePlugin.registerWith(registry.registrarFor("de.ffuf.in_app_update.InAppUpdatePlugin"));
    PathProviderPlugin.registerWith(registry.registrarFor("io.flutter.plugins.pathprovider.PathProviderPlugin"));
    PdfViewerPlugin.registerWith(registry.registrarFor("com.example.pdfviewerplugin.PdfViewerPlugin"));
    PermissionHandlerPlugin.registerWith(registry.registrarFor("com.baseflow.permissionhandler.PermissionHandlerPlugin"));
    ScreenPlugin.registerWith(registry.registrarFor("flutter.plugins.screen.screen.ScreenPlugin"));
    SharedPreferencesPlugin.registerWith(registry.registrarFor("io.flutter.plugins.sharedpreferences.SharedPreferencesPlugin"));
    SqflitePlugin.registerWith(registry.registrarFor("com.tekartik.sqflite.SqflitePlugin"));
    UrlLauncherPlugin.registerWith(registry.registrarFor("io.flutter.plugins.urllauncher.UrlLauncherPlugin"));
    VideoPlayerPlugin.registerWith(registry.registrarFor("io.flutter.plugins.videoplayer.VideoPlayerPlugin"));
    WakelockPlugin.registerWith(registry.registrarFor("creativecreatorormaybenot.wakelock.WakelockPlugin"));
    WebViewFlutterPlugin.registerWith(registry.registrarFor("io.flutter.plugins.webviewflutter.WebViewFlutterPlugin"));
  }

  private static boolean alreadyRegisteredWith(PluginRegistry registry) {
    final String key = GeneratedPluginRegistrant.class.getCanonicalName();
    if (registry.hasPlugin(key)) {
      return true;
    }
    registry.registrarFor(key);
    return false;
  }
}
