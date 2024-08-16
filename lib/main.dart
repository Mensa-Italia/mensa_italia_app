import 'package:flutter/material.dart';
import 'package:intl/find_locale.dart';
import 'package:intl/intl.dart';
import 'package:mensa_italia_app/app/app.bottomsheets.dart';
import 'package:mensa_italia_app/app/app.dialogs.dart';
import 'package:mensa_italia_app/app/app.locator.dart';
import 'package:mensa_italia_app/app/app.router.dart';
import 'package:mensa_italia_app/ui/common/app_colors.dart';
import 'package:sentry_flutter/sentry_flutter.dart';
import 'package:stacked_services/stacked_services.dart';
import 'package:flutter_localizations/flutter_localizations.dart';

Future<void> main() async {
  WidgetsFlutterBinding.ensureInitialized();

  Intl.defaultLocale = await findSystemLocale();
  await setupLocator();
  setupDialogUi();
  setupBottomSheetUi();
  await SentryFlutter.init(
    (options) {
      options.dsn = 'https://342c1850679ce1b9cadafb7b0e6f59aa@o4504321709309952.ingest.us.sentry.io/4507707395211264';

      options.tracesSampleRate = 1.0;
      options.profilesSampleRate = 1.0;
    },
    appRunner: () => runApp(const MainApp()),
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
      navigatorObservers: [
        StackedService.routeObserver,
      ],
      localizationsDelegates: const [
        GlobalMaterialLocalizations.delegate,
        GlobalWidgetsLocalizations.delegate,
        GlobalCupertinoLocalizations.delegate,
      ],
      supportedLocales: const [
        Locale('en'),
        Locale('it'),
      ],
      theme: ThemeData(
        //   platform: TargetPlatform.android,
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
        bottomAppBarTheme: const BottomAppBarTheme(
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
