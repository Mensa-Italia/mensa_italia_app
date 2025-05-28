import 'package:cached_network_image/cached_network_image.dart';
import 'package:dotted_border/dotted_border.dart';
import 'package:enefty_icons/enefty_icons.dart';
import 'package:flutter/cupertino.dart';
import 'package:flutter/material.dart';
import 'package:loading_animation_widget/loading_animation_widget.dart';
import 'package:mensa_italia_app/model/event.dart';
import 'package:mensa_italia_app/ui/common/app_bar.dart';
import 'package:mensa_italia_app/ui/common/app_colors.dart';
import 'package:stacked/stacked.dart';

import 'add_event_viewmodel.dart';

class AddEventView extends StackedView<AddEventViewModel> {
  final EventModel? event;
  const AddEventView({super.key, this.event});

  @override
  Widget builder(BuildContext context, AddEventViewModel viewModel, Widget? child) {
    return Scaffold(
      extendBodyBehindAppBar: true,
      appBar: getAppBarPlatform(
        previousPageTitle: "Events",
        title: event == null ? 'Add Event' : 'Edit Event',
        trailings: (event != null)
            ? [
                IconButton(
                  icon: const Icon(
                    EneftyIcons.trash_outline,
                    size: 22,
                    color: Colors.red,
                  ),
                  onPressed: viewModel.deleteEvent,
                )
              ]
            : null,
      ),
      body: ListView(
        children: [
          if (viewModel.allowControlEvents()) ...[
            GestureDetector(
              onTap: viewModel.pickImage,
              child: Padding(
                padding: const EdgeInsets.all(20.0).copyWith(bottom: 0),
                child: DottedBorder(
                  borderType: BorderType.RRect,
                  radius: const Radius.circular(30),
                  strokeWidth: 3,
                  dashPattern: const [3, 5],
                  child: ClipRRect(
                    borderRadius: const BorderRadius.all(Radius.circular(30)),
                    child: AspectRatio(
                      aspectRatio: 1528 / 603,
                      child: Container(
                        alignment: Alignment.center,
                        decoration: BoxDecoration(
                          color: Colors.grey[200],
                          image: viewModel.imageBytes != null
                              ? DecorationImage(
                                  image: MemoryImage(viewModel.imageBytes!),
                                  fit: BoxFit.cover,
                                )
                              : event?.image != null && event!.image.isNotEmpty
                                  ? DecorationImage(
                                      image: CachedNetworkImageProvider(event!.image),
                                      fit: BoxFit.cover,
                                    )
                                  : null,
                        ),
                        child: !(viewModel.imageBytes != null || event?.image != null)
                            ? const Text(
                                'Add Image',
                                style: TextStyle(
                                  fontWeight: FontWeight.bold,
                                ),
                              )
                            : null,
                      ),
                    ),
                  ),
                ),
              ),
            ),
            Padding(
              padding: const EdgeInsets.all(20.0).copyWith(bottom: 40),
              child: ElevatedButton(onPressed: viewModel.generateImage, child: const Text("IA Generate Image")),
            ),
          ],
          Form(
            key: viewModel.formKey,
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.stretch,
              children: [
                if (viewModel.allowControlEvents())
                  _SettingContainer(
                    children: [
                      Padding(
                        padding: const EdgeInsets.symmetric(horizontal: 20),
                        child: Row(
                          children: [
                            const Expanded(
                              child: Text(
                                "Is online?",
                                style: TextStyle(
                                  fontSize: 16,
                                  fontWeight: FontWeight.bold,
                                ),
                              ),
                            ),
                            CupertinoSwitch(
                              value: viewModel.isOnline,
                              onChanged: viewModel.toggleOnline,
                              activeColor: Colors.blue,
                            ),
                          ],
                        ),
                      ),
                      Padding(
                        padding: const EdgeInsets.symmetric(horizontal: 20),
                        child: Row(
                          children: [
                            const Expanded(
                              child: Text(
                                "Is a national event?",
                                style: TextStyle(
                                  fontSize: 16,
                                  fontWeight: FontWeight.bold,
                                ),
                              ),
                            ),
                            CupertinoSwitch(
                              value: viewModel.isNational,
                              onChanged: viewModel.toggleNational,
                              activeColor: Colors.blue,
                            ),
                          ],
                        ),
                      ),
                    ],
                  ),
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
                if (!viewModel.isOnline)
                  Padding(
                    padding: const EdgeInsets.all(20.0).copyWith(top: 0),
                    child: TextFormField(
                      controller: viewModel.locationController,
                      onTap: viewModel.pickLocation,
                      canRequestFocus: false,
                      enableInteractiveSelection: false,
                      decoration: const InputDecoration(
                        hintText: 'Where',
                      ),
                    ),
                  ),
                Padding(
                  padding: const EdgeInsets.all(20.0).copyWith(top: 0),
                  child: TextFormField(
                    controller: viewModel.dateTimeStartEvent,
                    onTap: viewModel.pickStartTime,
                    canRequestFocus: false,
                    enableInteractiveSelection: false,
                    decoration: const InputDecoration(
                      hintText: 'Starting at',
                    ),
                  ),
                ),
                Padding(
                  padding: const EdgeInsets.all(20.0).copyWith(top: 0),
                  child: TextFormField(
                    controller: viewModel.dateTimeEndEvent,
                    onTap: viewModel.pickEndTime,
                    canRequestFocus: false,
                    enableInteractiveSelection: false,
                    decoration: const InputDecoration(
                      hintText: 'Ending date',
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
                Container(
                  margin: const EdgeInsets.symmetric(horizontal: 20),
                  padding: const EdgeInsets.all(8).copyWith(right: 0, left: 2),
                  decoration: BoxDecoration(
                    color: Colors.white,
                    borderRadius: BorderRadius.circular(10),
                  ),
                  child: ListTile(
                    onTap: viewModel.editSchedule,
                    title: const Text("Edit schedule"),
                    trailing: const Icon(CupertinoIcons.chevron_forward),
                  ),
                ),
                Padding(
                  padding: const EdgeInsets.all(20.0),
                  child: ElevatedButton(
                    onPressed: viewModel.addEvent,
                    child: viewModel.isBusy
                        ? LoadingAnimationWidget.beat(
                            color: Colors.white.withOpacity(.8),
                            size: 20,
                          )
                        : Text(
                            event == null ? 'Add Event' : 'Edit Event',
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
  AddEventViewModel viewModelBuilder(BuildContext context) => AddEventViewModel(event: event);
}

class _SettingContainer extends StatelessWidget {
  final List<Widget> children;
  const _SettingContainer({required this.children});

  @override
  Widget build(BuildContext context) {
    return Container(
      margin: const EdgeInsets.symmetric(horizontal: 20),
      padding: const EdgeInsets.all(8).copyWith(right: 0, left: 2),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(10),
      ),
      child: ListView.separated(
        shrinkWrap: true,
        physics: const NeverScrollableScrollPhysics(),
        itemCount: children.length,
        padding: EdgeInsets.zero,
        itemBuilder: (context, index) => children[index],
        separatorBuilder: (context, index) => Divider(
          indent: 50,
          color: kcLightGrey.withOpacity(.0),
        ),
      ),
    );
  }
}
