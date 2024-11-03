import 'package:flutter/cupertino.dart';
import 'package:flutter/material.dart';
import 'package:stacked_services/stacked_services.dart';

class SearchBarActions {
  final Function(String) onChanged;
  final Function(String) onSubmitted;
  final String hintText;
  final TextEditingController controller;

  FocusNode? focusNode;

  SearchBarActions({
    required this.onChanged,
    required this.controller,
    required this.onSubmitted,
    this.focusNode,
    this.hintText = 'Search',
  });
}

getAppBarSliverPlatform({required String title, String? previousPageTitle, List<Widget>? trailings, Widget? leading, List<Widget>? trailingTitle, SearchBarActions? searchBarActions}) {
  if (Theme.of(StackedService.navigatorKey!.currentContext!).platform == TargetPlatform.iOS) {
    return CupertinoSliverNavigationBar(
      previousPageTitle: previousPageTitle,
      largeTitle: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Text(
                title,
                style: const TextStyle(fontWeight: FontWeight.w900),
              ),
              const Expanded(child: SizedBox()),
            ],
          ),
          if (searchBarActions != null)
            SizedBox(
              height: 40,
              child: Padding(
                padding: const EdgeInsets.only(right: 15, top: 3),
                child: Row(
                  children: [
                    Expanded(
                      child: CupertinoSearchTextField(
                        onChanged: searchBarActions.onChanged,
                        controller: searchBarActions.controller,
                        prefixIcon: const Icon(CupertinoIcons.search),
                        onSubmitted: searchBarActions.onSubmitted,
                        focusNode: searchBarActions.focusNode,
                        placeholder: searchBarActions.hintText,
                      ),
                    ),
                    ...trailingTitle ?? [],
                  ],
                ),
              ),
            ),
        ],
      ),
      stretch: true,
      backgroundColor: Theme.of(StackedService.navigatorKey!.currentContext!).scaffoldBackgroundColor.withOpacity(.9),
      border: null,
      middle: Text(
        title,
        style: const TextStyle(fontWeight: FontWeight.bold),
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
                padding: EdgeInsets.only(top: kToolbarHeight + MediaQuery.of(StackedService.navigatorKey!.currentContext!).padding.top),
                child: Column(
                  children: [
                    if (searchBarActions != null)
                      Container(
                        padding: const EdgeInsets.only(right: 15, top: 3, left: 15),
                        height: kToolbarHeight,
                        child: Row(
                          children: [
                            Expanded(
                              child: TextField(
                                onChanged: searchBarActions.onChanged,
                                controller: searchBarActions.controller,
                                onSubmitted: searchBarActions.onSubmitted,
                                focusNode: searchBarActions.focusNode,
                                decoration: InputDecoration(
                                  hintText: searchBarActions.hintText,
                                  prefixIcon: Icon(Icons.search),
                                  isDense: true,
                                  contentPadding: EdgeInsets.symmetric(horizontal: 10, vertical: 5),
                                ),
                              ),
                            ),
                            ...trailingTitle ?? [],
                          ],
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
      expandedHeight: getExpandedHeight(
        StackedService.navigatorKey!.currentContext!,
        hasSearchBar: searchBarActions != null,
        hasTrailingTitle: trailingTitle != null,
      ),
      floating: false,
      pinned: true,
      actions: [leading ?? const SizedBox(), ...(trailings ?? [])],
      backgroundColor: Theme.of(StackedService.navigatorKey!.currentContext!).scaffoldBackgroundColor,
    );
  }
}

double getExpandedHeight(BuildContext context, {bool hasSearchBar = false, bool hasTrailingTitle = false}) {
  double expandedHeight = kToolbarHeight;
  if (hasSearchBar) {
    expandedHeight += kToolbarHeight;
  }
  if (hasTrailingTitle) {
    expandedHeight += kToolbarHeight;
  }
  return expandedHeight;
}

getAppBarPlatform({required String title, String? previousPageTitle, List<Widget>? trailings, Widget? leading, SearchBarActions? searchBarActions}) {
  if (Theme.of(StackedService.navigatorKey!.currentContext!).platform == TargetPlatform.iOS) {
    return PreferredSize(
      preferredSize: Size.fromHeight(kToolbarHeight + (searchBarActions != null ? 50 : 0)),
      child: Column(
        children: [
          CupertinoNavigationBar(
            previousPageTitle: previousPageTitle,
            middle: Text(
              title,
            ),
            backgroundColor: Theme.of(StackedService.navigatorKey!.currentContext!).scaffoldBackgroundColor.withOpacity(.8),
            border: null,
            leading: leading,
            trailing: trailings == null
                ? null
                : Row(
                    mainAxisSize: MainAxisSize.min,
                    children: trailings,
                  ),
          ),
          if (searchBarActions != null)
            Padding(
              padding: const EdgeInsets.symmetric(horizontal: 10).copyWith(bottom: 10),
              child: TextField(
                onChanged: searchBarActions.onChanged,
                controller: searchBarActions.controller,
                onSubmitted: searchBarActions.onSubmitted,
                focusNode: searchBarActions.focusNode,
                decoration: InputDecoration(
                  hintText: searchBarActions.hintText,
                  prefixIcon: Icon(CupertinoIcons.search),
                ),
              ),
            ),
        ],
      ),
    );
  } else {
    return AppBar(
      title: Text(
        title,
        style: const TextStyle(
          fontWeight: FontWeight.bold,
        ),
      ),
      bottom: searchBarActions != null
          ? PreferredSize(
              preferredSize: const Size.fromHeight(50),
              child: Padding(
                padding: const EdgeInsets.symmetric(horizontal: 10).copyWith(bottom: 10),
                child: TextField(
                  onChanged: searchBarActions.onChanged,
                  controller: searchBarActions.controller,
                  onSubmitted: searchBarActions.onSubmitted,
                  focusNode: searchBarActions.focusNode,
                  decoration: const InputDecoration(
                    hintText: 'Search',
                    prefixIcon: Icon(Icons.search),
                  ),
                ),
              ),
            )
          : null,
      actions: [leading ?? const SizedBox(), ...(trailings ?? [])],
      backgroundColor: Theme.of(StackedService.navigatorKey!.currentContext!).scaffoldBackgroundColor,
    );
  }
}
