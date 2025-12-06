import 'package:easy_localization/easy_localization.dart';
import 'package:flutter/cupertino.dart';
import 'package:flutter/material.dart';
import 'package:intl/intl.dart';
import 'package:mensa_italia_app/model/event_schedule.dart';
import 'package:mensa_italia_app/ui/common/app_bar.dart';
import 'package:mensa_italia_app/ui/common/app_colors.dart';
import 'package:stacked/stacked.dart';

import 'add_event_schedule_list_viewmodel.dart';

class AddEventScheduleListView extends StackedView<AddEventScheduleListViewModel> {
  final String previousPageTitle;
  final List<EventScheduleModel> eventSchedules;

  const AddEventScheduleListView({super.key, required this.eventSchedules, required this.previousPageTitle});

  @override
  Widget builder(BuildContext context, AddEventScheduleListViewModel viewModel, Widget? child) {
    return Scaffold(
      appBar: getAppBarPlatform(
        title: viewModel.componentName.tr(),
        previousPageTitle: previousPageTitle.tr(),
        trailings: [
          CupertinoButton(
            padding: EdgeInsets.zero,
            onPressed: viewModel.onTapAddSchedule,
            child: const Icon(
              CupertinoIcons.add_circled_solid,
              color: kcPrimaryColor,
            ),
          )
        ],
      ),
      body: ListView.separated(
          padding: const EdgeInsets.symmetric(vertical: 10, horizontal: 20),
          itemCount: viewModel.eventSchedules.length,
          itemBuilder: (context, index) {
            final event = viewModel.eventSchedules[index];
            return TileSchedue(eventSchedule: event, onTap: viewModel.tapEdit(event));
          },
          separatorBuilder: (context, index) {
            if (index != viewModel.eventSchedules.length - 1 && !DateUtils.isSameDay(viewModel.eventSchedules[index].whenStart, viewModel.eventSchedules[index + 1].whenStart)) {
              return Column(
                crossAxisAlignment: CrossAxisAlignment.stretch,
                children: [
                  const SizedBox(height: 30),
                  Container(
                    padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 0),
                    child: Text(
                      DateFormat('EEEE, d MMMM').format(viewModel.eventSchedules[index + 1].whenStart),
                      style: TextStyle(
                        color: Colors.grey.shade500,
                        height: 0,
                        fontWeight: FontWeight.bold,
                      ),
                    ),
                  ),
                  const Divider(),
                ],
              );
            }
            return const Divider();
          }),
    );
  }

  @override
  AddEventScheduleListViewModel viewModelBuilder(BuildContext context) => AddEventScheduleListViewModel(
        eventSchedules: eventSchedules,
      );
}

class TileSchedue extends StatelessWidget {
  final EventScheduleModel eventSchedule;
  final VoidCallback onTap;
  const TileSchedue({super.key, required this.eventSchedule, required this.onTap});

  @override
  Widget build(BuildContext context) {
    return GestureDetector(
      onTap: onTap,
      child: Container(
        padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 1),
        foregroundDecoration: (eventSchedule.id ?? "").startsWith("DELETE:") ? StrikeThroughDecoration() : null,
        child: Row(
          children: [
            Expanded(
              child: Text(
                eventSchedule.title,
                style: const TextStyle(
                  fontWeight: FontWeight.bold,
                ),
              ),
            ),
            Text(
              DateFormat('HH:mm').format(eventSchedule.whenStart),
            ),
          ],
        ),
      ),
    );
  }
}

class StrikeThroughDecoration extends Decoration {
  @override
  BoxPainter createBoxPainter([VoidCallback? onChanged]) {
    return _StrikeThroughPainter();
  }
}

class _StrikeThroughPainter extends BoxPainter {
  @override
  void paint(Canvas canvas, Offset offset, ImageConfiguration configuration) {
    final paint = Paint()
      ..strokeWidth = 1.0
      ..color = Colors.black
      ..style = PaintingStyle.fill;

    final rect = offset & configuration.size!;
    canvas.drawLine(Offset(rect.left, rect.top + rect.height / 2), Offset(rect.right, rect.top + rect.height / 2), paint);
    canvas.restore();
  }
}
