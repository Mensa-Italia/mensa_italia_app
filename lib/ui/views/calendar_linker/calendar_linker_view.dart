import 'package:easy_localization/easy_localization.dart';
import 'package:flutter/cupertino.dart';
import 'package:flutter/material.dart';
import 'package:mensa_italia_app/model/location.dart';
import 'package:mensa_italia_app/ui/common/app_colors.dart';
import 'package:stacked/stacked.dart';
import 'package:url_launcher/url_launcher_string.dart';

import 'calendar_linker_viewmodel.dart';

class CalendarLinkerView extends StackedView<CalendarLinkerViewModel> {
  final String previousPageTitle;
  const CalendarLinkerView({super.key, required this.previousPageTitle});

  @override
  Widget builder(
      BuildContext context, CalendarLinkerViewModel viewModel, Widget? child) {
    return Scaffold(
      appBar: CupertinoNavigationBar(
        previousPageTitle: previousPageTitle.tr(),
        middle: Text(
          viewModel.componentName.tr(),
          maxLines: 1,
        ),
      ),
      body: viewModel.calendarLink == null
          ? const Center(child: CircularProgressIndicator())
          : ListView(
              children: [
                Padding(
                  padding: const EdgeInsets.all(16),
                  child: ElevatedButton(
                    onPressed: viewModel.addToCalendar,
                    child: const Text("Link into your calendar"),
                  ),
                ),
                if (Theme.of(context).platform != TargetPlatform.iOS)
                  GestureDetector(
                    onTap: viewModel.copyToClipboard,
                    onLongPress: viewModel.copyToClipboard,
                    child: Container(
                      padding: const EdgeInsets.all(16),
                      margin:
                          const EdgeInsets.all(16).copyWith(bottom: 0, top: 0),
                      decoration: BoxDecoration(
                        color: Colors.white,
                        border: Border.all(color: kcPrimaryColor, width: 4),
                        borderRadius: BorderRadius.circular(8),
                      ),
                      child: Text(
                        "https:${viewModel.baseUrl}",
                        style: const TextStyle(
                          color: Colors.black,
                          fontSize: 16,
                        ),
                        maxLines: 1,
                        overflow: TextOverflow.ellipsis,
                      ),
                    ),
                  ),
                if (Theme.of(context).platform != TargetPlatform.iOS)
                  TextButton(
                    onPressed: () async {
                      if (await canLaunchUrlString(
                          "https://support.google.com/calendar/answer/37100?hl=en&co=GENIE.Platform%3DDesktop#:~:text=Use a link to add a public calendar")) {
                        launchUrlString(
                          "https://support.google.com/calendar/answer/37100?hl=en&co=GENIE.Platform%3DDesktop#:~:text=Use a link to add a public calendar",
                        );
                      }
                    },
                    child: const Text(
                      "Click here to watch a tutorial on how to add a subscribed calendar!",
                      style: TextStyle(
                        color: kcPrimaryColor,
                        fontSize: 14,
                      ),
                      textAlign: TextAlign.center,
                    ),
                  ),
                if (Theme.of(context).platform != TargetPlatform.iOS)
                  Container(
                    margin: const EdgeInsets.all(16)
                        .copyWith(left: 32, right: 32, top: 20),
                    decoration: BoxDecoration(
                      borderRadius: BorderRadius.circular(8),
                    ),
                    child: const Text(
                      "You can click on the button to add it to your google calendar. If it dosen't work or you use a different calendar you can hold and copy the link above and use it into a subscribed calendar.",
                      style: TextStyle(
                        color: Colors.black,
                        fontSize: 14,
                      ),
                      textAlign: TextAlign.center,
                    ),
                  ),
                Padding(
                  padding: const EdgeInsets.symmetric(horizontal: 16)
                      .copyWith(top: 16),
                  child: const Text("Events subscriptions"),
                ),
                const Divider(indent: 16, endIndent: 16),
                ListTile(
                  visualDensity: VisualDensity.compact,
                  dense: true,
                  title: const Text("National events"),
                  subtitle: const Text("Not editable"),
                  trailing: CupertinoSwitch(
                    value: true,
                    activeColor: kcPrimaryColor.withOpacity(.5),
                    onChanged: (value) {},
                  ),
                ),
                ...ListOfStates.map((state) {
                  return ListTile(
                    visualDensity: VisualDensity.compact,
                    dense: true,
                    title: Text(state),
                    trailing: CupertinoSwitch(
                      value: viewModel.hasState(state),
                      activeColor: kcPrimaryColor,
                      onChanged: viewModel.changeState(state),
                    ),
                  );
                }),
                Container(
                  padding: const EdgeInsets.all(16).copyWith(top: 20),
                  child: const Text(
                    "The updates will be reflected in your calendar but may take some time to appear. Check the update time in the calendar app.",
                    style: TextStyle(color: Colors.black, fontSize: 14),
                    textAlign: TextAlign.center,
                  ),
                ),
                SafeArea(child: Container(height: 16)),
              ],
            ),
    );
  }

  @override
  CalendarLinkerViewModel viewModelBuilder(BuildContext context) =>
      CalendarLinkerViewModel();
}
