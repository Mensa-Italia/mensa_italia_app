import 'package:flutter/material.dart';
import 'package:stacked_services/stacked_services.dart';

getCustomScrollViewPlatform(
    {required List<Widget> slivers, ScrollController? controller}) {
  final isIos =
      (Theme.of(StackedService.navigatorKey!.currentContext!).platform ==
          TargetPlatform.iOS);
  return CustomScrollView(
    physics: const AlwaysScrollableScrollPhysics(),
    controller: controller,
    anchor: isIos ? 0.06 : 0.0,
    slivers: slivers,
  );
}
