import 'package:flutter/material.dart';
import 'package:stacked_services/stacked_services.dart';

getIconPlatform(
    {required IconData ios, required IconData android, Color? color}) {
  if (Theme.of(StackedService.navigatorKey!.currentContext!).platform ==
      TargetPlatform.iOS) {
    return Icon(ios, color: color);
  } else {
    return Icon(android, color: color);
  }
}
