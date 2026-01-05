import 'package:easy_localization/easy_localization.dart';
import 'package:firebase_core/firebase_core.dart';
import 'package:flutter/material.dart';
import 'package:flutter_timezone/flutter_timezone.dart';
import 'package:intl/find_locale.dart';
import 'package:mensa_italia_app/api/translation_loader.dart';
import 'package:mensa_italia_app/app/app.bottomsheets.dart';
import 'package:mensa_italia_app/app/app.dialogs.dart';
import 'package:mensa_italia_app/app/app.locator.dart';
import 'package:mensa_italia_app/app/app.router.dart';
import 'package:mensa_italia_app/database/database.dart';
import 'package:mensa_italia_app/firebase_options.dart';
import 'package:mensa_italia_app/services/maps_api_header.dart';
import 'package:mensa_italia_app/ui/common/app_colors.dart';
import 'package:stacked_services/stacked_services.dart';
import 'package:timezone/data/latest.dart' as tz;
import 'package:timezone/timezone.dart' as tz;

Future<void> main() async {
  await WidgetsFlutterBinding.ensureInitialized();
  await Firebase.initializeApp();
  await EasyLocalization.ensureInitialized();
  await DB.init();
  await MapsApiHeader.init();
  tz.initializeTimeZones();
  Intl.defaultLocale = await findSystemLocale();
  try {
    tz.setLocalLocation(tz.getLocation((await FlutterTimezone.getLocalTimezone()).identifier));
  } catch (_) {}
  try {
    await Firebase.initializeApp(
      options: DefaultFirebaseOptions.currentPlatform,
    );
  } catch (_) {}
  await setupLocator();
  setupDialogUi();
  setupBottomSheetUi();
  runApp(
    EasyLocalization(
      supportedLocales: await TranslationLoader.getLocalizationList(),
      path: 'not required because translation are fetched from Tolgee',
      fallbackLocale: Locale('en', 'US'),
      assetLoader: TranslationLoader(),
      child: const MainApp(),
    ),
  );
}

class MainApp extends StatelessWidget {
  const MainApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      initialRoute: Routes.startupView,
      onGenerateRoute: StackedRouter().onGenerateRoute,
      navigatorKey: StackedService.navigatorKey,
      debugShowCheckedModeBanner: false,
      localizationsDelegates: context.localizationDelegates,
      supportedLocales: context.supportedLocales,
      locale: context.locale,
      navigatorObservers: [
        StackedService.routeObserver,
      ],
      theme: ThemeData(
        // platform: TargetPlatform.android,
        fontFamily: "Gotham",
        scaffoldBackgroundColor: const Color.fromRGBO(241, 245, 255, 1),
        inputDecorationTheme: InputDecorationTheme(
          filled: true,
          fillColor: kcBackgroundColor.withOpacity(.8),
          border: const OutlineInputBorder(
            borderSide: BorderSide.none,
            borderRadius: BorderRadius.all(Radius.circular(10)),
          ),
          isDense: true,
        ),
        appBarTheme: const AppBarTheme(
          iconTheme: IconThemeData(color: kcPrimaryColor, size: 30),
        ),
        elevatedButtonTheme: ElevatedButtonThemeData(
          style: ButtonStyle(
            backgroundColor: WidgetStateProperty.all(kcPrimaryColor),
            padding: WidgetStateProperty.all(const EdgeInsets.all(15)),
            shape: WidgetStateProperty.all(
              RoundedRectangleBorder(
                borderRadius: BorderRadius.circular(10),
              ),
            ),
            foregroundColor: WidgetStateProperty.all(Colors.white),
            textStyle: WidgetStateProperty.all(
              const TextStyle(
                fontSize: 16,
                fontWeight: FontWeight.bold,
              ),
            ),
          ),
        ),
        bottomNavigationBarTheme: const BottomNavigationBarThemeData(
          backgroundColor: Colors.transparent,
          selectedItemColor: kcPrimaryColor,
          unselectedItemColor: kcMediumGrey,
          showSelectedLabels: false,
          showUnselectedLabels: false,
        ),
        bottomAppBarTheme: const BottomAppBarThemeData(
          color: kcBackgroundColor,
          elevation: 0,
        ),
        listTileTheme: const ListTileThemeData(
          contentPadding: EdgeInsets.symmetric(horizontal: 20, vertical: 10),
          iconColor: kcPrimaryColor,
          titleTextStyle: TextStyle(
            color: kcPrimaryColorDark,
            fontSize: 16,
            fontWeight: FontWeight.bold,
          ),
        ),
        textButtonTheme: TextButtonThemeData(
          style: ButtonStyle(
            textStyle: WidgetStateProperty.all(
              const TextStyle(
                fontSize: 16,
                fontWeight: FontWeight.bold,
              ),
            ),
            visualDensity: VisualDensity.compact,
          ),
        ),
      ),
    );
  }
}
