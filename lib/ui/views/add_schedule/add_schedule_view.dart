import 'package:flutter/cupertino.dart';
import 'package:flutter/material.dart';
import 'package:loading_animation_widget/loading_animation_widget.dart';
import 'package:mensa_italia_app/model/event_schedule.dart';
import 'package:mensa_italia_app/ui/common/app_bar.dart';
import 'package:stacked/stacked.dart';

import 'add_schedule_viewmodel.dart';

class AddScheduleView extends StackedView<AddScheduleViewModel> {
  final EventScheduleModel? event;
  const AddScheduleView({Key? key, this.event}) : super(key: key);

  @override
  Widget builder(
      BuildContext context, AddScheduleViewModel viewModel, Widget? child) {
    return Scaffold(
      appBar: getAppBarPlatform(
        title: event == null ? 'Add Event' : 'Edit Event',
        previousPageTitle: 'Back',
        trailings: event == null
            ? null
            : [
                CupertinoButton(
                  padding: EdgeInsets.zero,
                  onPressed: viewModel.deleteEvent,
                  child: const Icon(
                    CupertinoIcons.trash,
                    color: Colors.red,
                  ),
                )
              ],
      ),
      body: ListView(
        children: [
          Form(
            key: viewModel.formKey,
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.stretch,
              children: [
                Padding(
                  padding: const EdgeInsets.all(20.0),
                  child: TextFormField(
                    controller: viewModel.nameController,
                    decoration: const InputDecoration(
                      hintText: 'Name',
                    ),
                  ),
                ),
                Padding(
                  padding: const EdgeInsets.all(20.0).copyWith(top: 0),
                  child: TextFormField(
                    controller: viewModel.descriptionController,
                    minLines: 3,
                    maxLines: 5,
                    decoration: const InputDecoration(
                      hintText: 'Description',
                    ),
                  ),
                ),
                Padding(
                  padding: const EdgeInsets.all(20.0).copyWith(top: 0),
                  child: TextFormField(
                    controller: viewModel.dateTimeEvent,
                    onTap: viewModel.pickDateTime,
                    canRequestFocus: false,
                    enableInteractiveSelection: false,
                    decoration: const InputDecoration(
                      hintText: 'When',
                    ),
                  ),
                ),
                Padding(
                  padding: const EdgeInsets.all(20.0).copyWith(top: 0),
                  child: TextFormField(
                    controller: viewModel.linkController,
                    decoration: const InputDecoration(
                      hintText: 'Info Link',
                    ),
                  ),
                ),
                Padding(
                  padding: const EdgeInsets.all(20.0),
                  child: ElevatedButton(
                    onPressed: viewModel.save,
                    child: viewModel.isBusy
                        ? LoadingAnimationWidget.beat(
                            color: Colors.white.withOpacity(.8),
                            size: 20,
                          )
                        : const Text(
                            'Save',
                            style: const TextStyle(color: Colors.white),
                          ),
                  ),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }

  @override
  AddScheduleViewModel viewModelBuilder(BuildContext context) =>
      AddScheduleViewModel(event: event);
}
