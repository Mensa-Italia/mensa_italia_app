import 'package:flutter/cupertino.dart';
import 'package:flutter/material.dart';
import 'package:stacked_services/stacked_services.dart';

class SearchBarActions {
  final Function(String) onChanged;
  final Function(String) onSubmitted;
  final TextEditingController controller;

  SearchBarActions(
      {required this.onChanged,
      required this.controller,
      required this.onSubmitted});
}

getAppBarSliverPlatform(
    {required String title,
    String? previousPageTitle,
    List<Widget>? trailings,
    Widget? leading,
    List<Widget>? trailingTitle,
    SearchBarActions? searchBarActions}) {
  if (Theme.of(StackedService.navigatorKey!.currentContext!).platform ==
      TargetPlatform.iOS) {
    return CupertinoSliverNavigationBar(
      previousPageTitle: previousPageTitle,
      largeTitle: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Text(
                title,
                style: TextStyle(fontWeight: FontWeight.w900),
              ),
              const Expanded(child: SizedBox()),
              ...trailingTitle ?? [],
            ],
          ),
          if (searchBarActions != null)
            SizedBox(
              height: 40,
              child: Padding(
                padding: const EdgeInsets.only(right: 15, top: 3),
                child: CupertinoSearchTextField(
                  onChanged: searchBarActions.onChanged,
                  controller: searchBarActions.controller,
                  prefixIcon: const Icon(CupertinoIcons.search),
                  onSubmitted: searchBarActions.onSubmitted,
                ),
              ),
            ),
        ],
      ),
      stretch: true,
      backgroundColor: Theme.of(StackedService.navigatorKey!.currentContext!)
          .scaffoldBackgroundColor
          .withOpacity(.9),
      border: null,
      middle: Text(
        title,
        style: TextStyle(fontWeight: FontWeight.bold),
      ),
      alwaysShowMiddle: false,
      leading: leading,
      trailing: trailings == null
          ? null
          : Row(
              mainAxisSize: MainAxisSize.min,
              crossAxisAlignment: CrossAxisAlignment.center,
              children: trailings,
            ),
    );
  } else {
    return SliverAppBar(
      flexibleSpace: trailingTitle != null || searchBarActions != null
          ? FlexibleSpaceBar(
              collapseMode: CollapseMode.parallax,
              background: Padding(
                padding: const EdgeInsets.only(top: kToolbarHeight * 2),
                child: Column(
                  children: [
                    trailingTitle != null
                        ? Row(
                            children: [
                              Spacer(),
                              ...trailingTitle,
                            ],
                          )
                        : const SizedBox(),
                    if (searchBarActions != null)
                      Container(
                        padding:
                            const EdgeInsets.only(right: 15, top: 3, left: 15),
                        height: kToolbarHeight,
                        child: TextField(
                          onChanged: searchBarActions.onChanged,
                          controller: searchBarActions.controller,
                          onSubmitted: searchBarActions.onSubmitted,
                          decoration: const InputDecoration(
                            hintText: 'Search',
                            prefixIcon: Icon(Icons.search),
                            isDense: true,
                            contentPadding: const EdgeInsets.symmetric(
                                horizontal: 10, vertical: 5),
                          ),
                        ),
                      ),
                  ],
                ),
              ),
            )
          : null,
      title: Text(
        title,
        style: const TextStyle(
          fontWeight: FontWeight.bold,
        ),
      ),
      expandedHeight: kToolbarHeight *
          (searchBarActions != null || trailingTitle != null
              ? trailingTitle != null
                  ? 3
                  : 2
              : 1),
      floating: false,
      pinned: true,
      actions: [leading ?? const SizedBox(), ...(trailings ?? [])],
      backgroundColor: Theme.of(StackedService.navigatorKey!.currentContext!)
          .scaffoldBackgroundColor,
    );
  }
}

getAppBarPlatform(
    {required String title,
    String? previousPageTitle,
    List<Widget>? trailings,
    Widget? leading,
    SearchBarActions? searchBarActions}) {
  if (Theme.of(StackedService.navigatorKey!.currentContext!).platform ==
      TargetPlatform.iOS) {
    return CupertinoNavigationBar(
      previousPageTitle: previousPageTitle,
      middle: (searchBarActions != null)
          ? Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              mainAxisAlignment: MainAxisAlignment.center,
              children: [
                Text(
                  title,
                ),
                SizedBox(
                  height: 40,
                  child: Padding(
                    padding: const EdgeInsets.only(right: 15, top: 3),
                    child: CupertinoSearchTextField(
                      onChanged: searchBarActions.onChanged,
                      controller: searchBarActions.controller,
                      prefixIcon: const Icon(CupertinoIcons.search),
                      onSubmitted: searchBarActions.onSubmitted,
                    ),
                  ),
                ),
              ],
            )
          : Text(
              title,
            ),
      backgroundColor: Theme.of(StackedService.navigatorKey!.currentContext!)
          .scaffoldBackgroundColor
          .withOpacity(.8),
      border: null,
      leading: leading,
      trailing: trailings == null
          ? null
          : Row(
              mainAxisSize: MainAxisSize.min,
              children: trailings,
            ),
    );
  } else {
    return AppBar(
      title: Text(
        title,
        style: TextStyle(
          fontWeight: FontWeight.bold,
        ),
      ),
      actions: [leading ?? SizedBox(), ...(trailings ?? [])],
      backgroundColor: Theme.of(StackedService.navigatorKey!.currentContext!)
          .scaffoldBackgroundColor,
    );
  }
}
