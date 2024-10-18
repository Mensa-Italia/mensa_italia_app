import 'package:flutter/material.dart';
import 'package:stacked/stacked.dart';

import 'addon_stamp_viewmodel.dart';

class AddonStampView extends StackedView<AddonStampViewModel> {
  const AddonStampView({Key? key}) : super(key: key);

  @override
  Widget builder(BuildContext context, AddonStampViewModel viewModel, Widget? child) {
    return Scaffold(
      body: Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          crossAxisAlignment: CrossAxisAlignment.center,
          children: [
            AnimatedTableport(),
          ],
        ),
      ),
    );
  }

  @override
  AddonStampViewModel viewModelBuilder(BuildContext context) => AddonStampViewModel();
}

//book opening animation
class AnimatedTableport extends StatefulWidget {
  const AnimatedTableport({super.key});

  @override
  State<AnimatedTableport> createState() => _AnimatedTableportState();
}

class _AnimatedTableportState extends State<AnimatedTableport> with SingleTickerProviderStateMixin {
  late AnimationController _controller;
  late Animation<double> _animation;

  @override
  void initState() {
    super.initState();
    _controller = AnimationController(duration: const Duration(seconds: 2), vsync: this);
    _animation = Tween<double>(begin: 0, end: 1).animate(_controller)
      ..addListener(() {
        setState(() {});
      });
    _controller.forward();
  }

  @override
  Widget build(BuildContext context) {
    final widthPassport = MediaQuery.of(context).size.width - 80;
    final heightPassport = widthPassport * 125 / 88;
    return Transform.scale(
      scale: _animation.value,
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
    );
  }
}
