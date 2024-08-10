import 'package:flutter/cupertino.dart';
import 'package:flutter/material.dart';
import 'package:mensa_italia_app/model/location.dart';
import 'package:mensa_italia_app/ui/common/app_colors.dart';
import 'package:stacked/stacked.dart';

import 'calendar_linker_viewmodel.dart';

class CalendarLinkerView extends StackedView<CalendarLinkerViewModel> {
  const CalendarLinkerView({super.key});

  @override
  Widget builder(BuildContext context, CalendarLinkerViewModel viewModel, Widget? child) {
    return Scaffold(
      appBar: const CupertinoNavigationBar(
        previousPageTitle: "Settings",
        middle: Text('Calendar Linker'),
      ),
      body: viewModel.calendarLink == null
          ? const Center(child: CircularProgressIndicator())
          : ListView(
              children: [
                GestureDetector(
                  onTap: viewModel.addToCalendar,
                  onLongPress: viewModel.copyToClipboard,
                  child: Container(
                    padding: const EdgeInsets.all(16),
                    margin: const EdgeInsets.all(16),
                    decoration: BoxDecoration(
                      color: Colors.white,
                      border: Border.all(color: kcPrimaryColor, width: 4),
                      borderRadius: BorderRadius.circular(8),
                    ),
                    child: Text(
                      "webcal:${viewModel.baseUrl}",
                      style: TextStyle(
                        color: Colors.black,
                        fontSize: 16,
                      ),
                      maxLines: 1,
                      overflow: TextOverflow.ellipsis,
                    ),
                  ),
                ),
                Padding(
                  padding: const EdgeInsets.all(16).copyWith(top: 0),
                  child: ElevatedButton(
                    onPressed: viewModel.addToCalendar,
                    child: const Text("Add to your calendar"),
                  ),
                ),
                Container(
                  margin: const EdgeInsets.all(16).copyWith(left: 32, right: 32, top: 0),
                  decoration: BoxDecoration(
                    borderRadius: BorderRadius.circular(8),
                  ),
                  child: const Text(
                    "Using the above link, you can add the Mensa Italia calendar to your favorite calendar app. You can long press to copy it or just add it to your calendar.",
                    style: TextStyle(
                      color: Colors.black,
                      fontSize: 14,
                    ),
                    textAlign: TextAlign.center,
                  ),
                ),
                Padding(
                  padding: const EdgeInsets.symmetric(horizontal: 16).copyWith(top: 16),
                  child: Text("Events subscriptions"),
                ),
                const Divider(indent: 16, endIndent: 16),
                ListTile(
                  visualDensity: VisualDensity.compact,
                  dense: true,
                  title: Text("National events"),
                  subtitle: Text("Not editable"),
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
                }).toList(),
                Container(
                  padding: const EdgeInsets.all(16).copyWith(top: 20),
                  child: Text(
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
  CalendarLinkerViewModel viewModelBuilder(BuildContext context) => CalendarLinkerViewModel();
}
