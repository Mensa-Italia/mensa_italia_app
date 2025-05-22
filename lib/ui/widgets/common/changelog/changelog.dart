import 'package:enefty_icons/enefty_icons.dart';
import 'package:flutter/material.dart';
import 'package:stacked/stacked.dart';

import 'changelog_model.dart';

class Changelog extends StackedView<ChangelogModel> {
  const Changelog({super.key});

  @override
  Widget builder(BuildContext context, ChangelogModel viewModel, Widget? child) {
    return Container(
      decoration: BoxDecoration(
        color: Theme.of(context).scaffoldBackgroundColor,
      ),
      height: MediaQuery.of(context).size.height - MediaQuery.of(context).padding.top - MediaQuery.of(context).padding.bottom - 50,
      child: SafeArea(
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            Expanded(
              child: SingleChildScrollView(
                  child: Column(
                children: [
                  SizedBox(height: 50),
                  Center(
                    child: Text(
                      "What's new",
                      style: TextStyle(
                        fontSize: 30,
                        fontWeight: FontWeight.bold,
                      ),
                      textAlign: TextAlign.center,
                    ),
                  ),
                  SizedBox(height: 40),
                  _WhatsNewComponent(
                    icon: EneftyIcons.ticket_2_outline,
                    title: "Semplified event creation",
                    description: "We have simplified the event creation process, making it easier and faster to create events.",
                    color: Colors.blueAccent,
                  ),
                  SizedBox(height: 20),
                  _WhatsNewComponent(
                    icon: EneftyIcons.timer_2_outline,
                    title: "DateTime input fixed",
                    description: "We have fixed the DateTime input, making it more user-friendly and reliable.",
                    color: Colors.pink,
                  ),
                  SizedBox(height: 20),
                  _WhatsNewComponent(
                    icon: EneftyIcons.people_outline,
                    title: "Improved contacts addon",
                    description: "We have improved the performance of the contacts addon, making it faster and more efficient.",
                    color: Colors.green,
                  ),
                  SizedBox(height: 20),
                  _WhatsNewComponent(
                    icon: EneftyIcons.calendar_2_outline,
                    title: "Birthday reminder",
                    description: "We have added a birthday reminder feature, so you never forget a friend's birthday again.",
                    color: Colors.purple,
                  ),
                  SizedBox(height: 20),
                ],
              )),
            ),
            Padding(
              padding: const EdgeInsets.all(8.0).copyWith(left: 30, right: 30),
              child: ElevatedButton(
                onPressed: viewModel.close,
                child: Text("PERFECT!"),
              ),
            ),
          ],
        ),
      ),
    );
  }

  @override
  ChangelogModel viewModelBuilder(BuildContext context) => ChangelogModel();
}

class _WhatsNewComponent extends StatelessWidget {
  final IconData icon;
  final String title;
  final String description;
  final Color color;
  const _WhatsNewComponent({required this.icon, required this.title, required this.description, required this.color});

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.all(8.0).copyWith(left: 50, right: 50),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Icon(
            icon,
            color: color,
            size: 50,
          ),
          SizedBox(width: 20),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  title,
                  style: TextStyle(
                    fontSize: 16,
                    fontWeight: FontWeight.bold,
                  ),
                ),
                Text(description, style: TextStyle(fontSize: 14)),
              ],
            ),
          ),
        ],
      ),
    );
  }
}
