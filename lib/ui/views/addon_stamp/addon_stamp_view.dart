import 'dart:math';
import 'package:easy_localization/easy_localization.dart';
import 'package:enefty_icons/enefty_icons.dart';
import 'package:flutter/material.dart';
import 'package:mensa_italia_app/model/stamp.dart';
import 'package:mensa_italia_app/model/stamp_user.dart';
import 'package:mensa_italia_app/ui/common/app_bar.dart';
import 'package:stacked/stacked.dart';

import 'addon_stamp_viewmodel.dart';

class AddonStampView extends StackedView<AddonStampViewModel> {
  const AddonStampView({Key? key}) : super(key: key);

  @override
  Widget builder(BuildContext context, AddonStampViewModel viewModel, Widget? child) {
    return Scaffold(
      appBar: getAppBarPlatform(
        title: "addons.tableport.title".tr(),
        previousPageTitle: "addons.tableport.details.previouspagetitle".tr(),
      ),
      body: Center(
        child: AnimatedTableport(
          stamps: viewModel.stamps,
          onTapAddStamp: viewModel.addStamp,
          showStamp: viewModel.showStamp,
        ),
      ),
    );
  }

  @override
  AddonStampViewModel viewModelBuilder(BuildContext context) => AddonStampViewModel();
}

//book opening animation
class AnimatedTableport extends StatefulWidget {
  final List<StampUserModel> stamps;
  final Function onTapAddStamp;
  final Function(StampModel stamp) showStamp;
  const AnimatedTableport({super.key, required this.stamps, required this.onTapAddStamp, required this.showStamp});

  @override
  State<AnimatedTableport> createState() => _AnimatedTableportState();
}

class _AnimatedTableportState extends State<AnimatedTableport> with SingleTickerProviderStateMixin {
  late AnimationController _controller;
  late Animation<double> _animationFirst;
  late Animation<double> _animationSecond;
  late Animation<double> _animationThird;
  late Animation<double> _animationFourth;

  @override
  void initState() {
    super.initState();
    _controller = AnimationController(duration: const Duration(milliseconds: 6000), vsync: this)
      ..addListener(() {
        setState(() {});
      });
    _animationFirst = Tween<double>(begin: 1, end: 0).animate(
      CurvedAnimation(
        parent: _controller,
        curve: const Interval(0, 0.2, curve: Curves.easeInOut),
      ),
    );
    _animationSecond = Tween<double>(begin: 0, end: pi / 2).animate(
      CurvedAnimation(
        parent: _controller,
        curve: const Interval(0.3, .5, curve: Curves.easeInOut),
      ),
    );
    _animationThird = Tween<double>(begin: 1, end: 0).animate(
      CurvedAnimation(
        parent: _controller,
        curve: const Interval(0.6, 1, curve: Curves.easeInOut),
      ),
    );
    _animationFourth = Tween<double>(begin: 0.8, end: 1).animate(
      CurvedAnimation(
        parent: _controller,
        curve: const Interval(0.3, .5, curve: Curves.easeInOut),
      ),
    );
    _controller.forward();
  }

  @override
  Widget build(BuildContext context) {
    final widthPassport = MediaQuery.of(context).size.width;
    final heightPassport = widthPassport * 125 / 88;
    return Transform.scale(
      scale: _animationFourth.value,
      child: Stack(
        children: [
          Positioned(
            top: (MediaQuery.of(context).size.height / 2 - heightPassport / 2) * _animationThird.value,
            bottom: (MediaQuery.of(context).size.height / 2 - heightPassport / 2) * _animationThird.value,
            right: 0,
            left: 0,
            child: Transform.scale(
              scale: (_animationFirst.value * 2) + 1,
              child: Transform.translate(
                offset: Offset(_animationFirst.value * MediaQuery.of(context).size.width, ((_animationFirst.value * (MediaQuery.of(context).size.height)) / 2)),
                child: GridView.builder(
                  gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(
                    crossAxisCount: 3,
                    childAspectRatio: 1,
                  ),
                  reverse: false,
                  itemCount: (widget.stamps.length + 1) + (24 - (widget.stamps.length + 1) % 24),
                  itemBuilder: (context, index) {
                    if (index >= (widget.stamps.length + 1)) {
                      return Container(
                        decoration: BoxDecoration(
                          color: Theme.of(context).scaffoldBackgroundColor,
                          border: Border.all(
                            color: Colors.black.withOpacity(.1),
                            width: .5,
                          ),
                        ),
                      );
                    }
                    final sizeOfCell = MediaQuery.of(context).size.width / 3;
                    if (index == 0) {
                      return GestureDetector(
                        onTap: () {
                          widget.onTapAddStamp();
                        },
                        child: Container(
                          decoration: BoxDecoration(
                            color: Theme.of(context).scaffoldBackgroundColor,
                            border: Border.all(
                              color: Colors.black.withOpacity(.1),
                              width: .5,
                            ),
                          ),
                          child: Column(
                            children: [
                              Expanded(child: SizedBox()),
                              Text(
                                "",
                                style: TextStyle(
                                  color: Colors.black,
                                  fontSize: 12,
                                ),
                              ),
                              Expanded(child: SizedBox()),
                              Icon(
                                EneftyIcons.add_circle_outline,
                                size: sizeOfCell / 2,
                              ),
                              Expanded(child: SizedBox()),
                              Text(
                                "addons.tableport.addnewstamp".tr(),
                                style: TextStyle(
                                  color: Colors.black,
                                  fontSize: 12,
                                ),
                                textAlign: TextAlign.center,
                              ),
                              Expanded(child: SizedBox()),
                            ],
                          ),
                        ),
                      );
                    }
                    final stamp = widget.stamps[index - 1];
                    return GestureDetector(
                      onTap: () {
                        widget.showStamp(stamp.stamp);
                      },
                      child: Container(
                        decoration: BoxDecoration(
                          color: Theme.of(context).scaffoldBackgroundColor,
                          border: Border.all(
                            color: Colors.black.withOpacity(.1),
                            width: .5,
                          ),
                        ),
                        child: Transform.rotate(
                          angle: doubleInRange(Random(stamp.fastHash()), -pi / 2, pi / 2),
                          child: Transform.translate(
                            offset: Offset(
                              doubleInRange(Random(stamp.fastHash()), -sizeOfCell / 10, sizeOfCell / 10),
                              doubleInRange(Random(stamp.fastHash()), -sizeOfCell / 10, sizeOfCell / 10),
                            ),
                            child: Transform.scale(
                              scale: .9,
                              child: Container(
                                decoration: BoxDecoration(
                                  borderRadius: BorderRadius.circular(300),
                                  image: DecorationImage(
                                    image: NetworkImage(stamp.stamp.image),
                                    fit: BoxFit.cover,
                                  ),
                                ),
                              ),
                            ),
                          ),
                        ),
                      ),
                    );
                  },
                ),
              ),
            ),
          ),
          Positioned(
            top: (MediaQuery.of(context).size.height / 2 - heightPassport / 2) * _animationThird.value,
            bottom: (MediaQuery.of(context).size.height / 2 - heightPassport / 2) * _animationThird.value,
            right: 0,
            left: 0,
            child: IgnorePointer(
              child: Container(
                decoration: BoxDecoration(
                  gradient: LinearGradient(
                    begin: Alignment.topCenter,
                    end: Alignment.bottomCenter,
                    colors: [
                      Theme.of(context).scaffoldBackgroundColor.withOpacity(0),
                      Theme.of(context).scaffoldBackgroundColor,
                    ],
                    stops: [
                      0.5 + (1 - _animationThird.value) / 2,
                      1,
                    ],
                  ),
                ),
              ),
            ),
          ),
          Positioned(
            left: 0,
            top: MediaQuery.of(context).size.height / 2 - heightPassport / 2,
            child: Transform(
              transform: Matrix4.identity()
                ..setEntry(3, 2, 0.001)
                ..rotateY(_animationSecond.value),
              origin: Offset(0, heightPassport / 2),
              child: Transform.scale(
                scale: (_animationFirst.value * 2) + 1,
                child: Transform.translate(
                  offset: Offset(_animationFirst.value * MediaQuery.of(context).size.width, _animationFirst.value * MediaQuery.of(context).size.height / 2),
                  child: Container(
                    width: widthPassport,
                    height: heightPassport,
                    decoration: BoxDecoration(
                      image: DecorationImage(image: AssetImage("assets/images/tableport_base.jpg"), fit: BoxFit.cover),
                      borderRadius: BorderRadius.only(
                        topRight: Radius.circular(30),
                        bottomRight: Radius.circular(30),
                      ),
                    ),
                  ),
                ),
              ),
            ),
          ),
        ],
      ),
    );
  }

  double doubleInRange(Random source, num start, num end) => source.nextDouble() * (end - start) + start;
}
