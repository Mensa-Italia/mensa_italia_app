import 'package:dotted_border/dotted_border.dart';
import 'package:flutter/cupertino.dart';
import 'package:flutter/material.dart';
import 'package:mensa_italia_app/model/event.dart';
import 'package:stacked/stacked.dart';

import 'add_event_viewmodel.dart';

class AddEventView extends StackedView<AddEventViewModel> {
  final EventModel? event;
  const AddEventView({Key? key, this.event}) : super(key: key);

  @override
  Widget builder(
    BuildContext context,
    AddEventViewModel viewModel,
    Widget? child,
  ) {
    return Scaffold(
      extendBody: true,
      extendBodyBehindAppBar: true,
      appBar: CupertinoNavigationBar(
        backgroundColor: Colors.white.withOpacity(0.6),
        previousPageTitle: "Events",
        middle: const Text('Add Event'),
      ),
      body: ListView(
        children: [
          GestureDetector(
            onTap: viewModel.pickImage,
            child: Padding(
              padding: const EdgeInsets.all(20.0),
              child: DottedBorder(
                borderType: BorderType.RRect,
                radius: const Radius.circular(30),
                strokeWidth: 3,
                dashPattern: [3, 5],
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
                            : event?.image != null
                                ? DecorationImage(
                                    image: NetworkImage(event!.image),
                                    fit: BoxFit.cover,
                                  )
                                : null,
                      ),
                      child: !(viewModel.imageBytes != null ||
                              event?.image != null)
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
          Form(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.stretch,
              children: [
                Padding(
                  padding: const EdgeInsets.all(20.0),
                  child: TextFormField(
                    controller: viewModel.nameController,
                    decoration: const InputDecoration(
                      labelText: 'Name',
                    ),
                  ),
                ),
                Padding(
                  padding: const EdgeInsets.all(20.0).copyWith(top: 0),
                  child: TextFormField(
                    controller: viewModel.linkController,
                    decoration: const InputDecoration(
                      labelText: 'Description',
                    ),
                  ),
                ),
                Padding(
                  padding: const EdgeInsets.all(20.0).copyWith(top: 0),
                  child: TextFormField(
                    controller: viewModel.linkController,
                    decoration: const InputDecoration(
                      labelText: 'Location',
                    ),
                  ),
                ),
                Padding(
                  padding: const EdgeInsets.all(20.0).copyWith(top: 0),
                  child: TextFormField(
                    controller: viewModel.linkController,
                    decoration: const InputDecoration(
                      labelText: 'When',
                    ),
                  ),
                ),
                Padding(
                  padding: const EdgeInsets.all(20.0).copyWith(top: 0),
                  child: TextFormField(
                    controller: viewModel.linkController,
                    decoration: const InputDecoration(
                      labelText: 'Info Link',
                    ),
                  ),
                ),
                Padding(
                  padding: const EdgeInsets.all(20.0),
                  child: ElevatedButton(
                    onPressed: viewModel.addEvent,
                    child: const Text('Add Event'),
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
  AddEventViewModel viewModelBuilder(
    BuildContext context,
  ) =>
      AddEventViewModel();
}
