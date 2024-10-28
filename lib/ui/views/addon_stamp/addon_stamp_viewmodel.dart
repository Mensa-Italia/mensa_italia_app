import 'dart:async';
import 'dart:ui';

import 'package:easy_localization/easy_localization.dart';
import 'package:flutter/material.dart';
import 'package:mensa_italia_app/api/api.dart';
import 'package:mensa_italia_app/model/stamp.dart';
import 'package:mensa_italia_app/model/stamp_user.dart';
import 'package:mensa_italia_app/ui/common/master_model.dart';
import 'package:mobile_scanner/mobile_scanner.dart';

class AddonStampViewModel extends MasterModel {
  final List<StampUserModel> stamps = [];

  AddonStampViewModel() {
    Api().getStamps().then((value) {
      stamps.clear();
      stamps.addAll(value);
      rebuildUi();
    });
  }

  addStamp() {
    showBeautifulBottomSheet(
      child: _addStampModal(),
    ).then((value) {
      Api().getStamps().then((value) {
        stamps.clear();
        stamps.addAll(value);
        rebuildUi();
      });
    });
  }

  showStamp(StampModel stamp) {
    showBeautifulBottomSheet(
      child: _showStamp(stamp: stamp),
    );
  }
}

class _showStamp extends StatelessWidget {
  final StampModel stamp;
  const _showStamp({super.key, required this.stamp});

  @override
  Widget build(BuildContext context) {
    return ClipRRect(
      borderRadius: const BorderRadius.only(
        topLeft: Radius.circular(30),
        topRight: Radius.circular(30),
      ),
      child: BackdropFilter(
        filter: ImageFilter.blur(sigmaX: 20, sigmaY: 20),
        child: Container(
          decoration: BoxDecoration(
            color: Colors.white.withOpacity(.6),
            borderRadius: const BorderRadius.only(
              topLeft: Radius.circular(30),
              topRight: Radius.circular(30),
            ),
            border: Border.all(color: Colors.white.withOpacity(.4), width: 2),
          ),
          child: SafeArea(
            child: Column(
              mainAxisSize: MainAxisSize.min,
              crossAxisAlignment: CrossAxisAlignment.stretch,
              children: [
                SizedBox(
                  height: 20,
                ),
                Center(
                  child: Container(
                    width: MediaQuery.of(context).size.width * 0.8,
                    height: MediaQuery.of(context).size.width * 0.8,
                    decoration: BoxDecoration(
                      image: DecorationImage(
                        image: NetworkImage(stamp.image),
                        fit: BoxFit.cover,
                      ),
                    ),
                    clipBehavior: Clip.antiAlias,
                  ),
                ),
                SizedBox(
                  height: 40,
                ),
                Center(
                  child: Text(
                    stamp.description,
                    style: const TextStyle(
                      color: Colors.black,
                      fontWeight: FontWeight.bold,
                      fontSize: 20,
                    ),
                  ),
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}

class _addStampModal extends StatefulWidget {
  const _addStampModal({super.key});

  @override
  State<_addStampModal> createState() => __addStampModalState();
}

class __addStampModalState extends State<_addStampModal> {
  String idStamp = "";
  String codeStamp = "";
  MobileScannerController controller = MobileScannerController();
  StreamSubscription<BarcodeCapture>? _subscription;
  int state = 0;
  StampModel? stamp;

  @override
  void initState() {
    _subscription = controller.barcodes.listen(_handleBarcode);
    super.initState();
  }

  void _handleBarcode(BarcodeCapture barcode) {
    if ((barcode.barcodes.first.rawValue ?? "").contains(":::") == false) {
      return;
    }
    try {
      setState(() {
        state = 1;
      });
      _subscription?.cancel();
      final valuesScanned = (barcode.barcodes.first.rawValue ?? "").split(":::");
      idStamp = valuesScanned[0];
      codeStamp = valuesScanned[1];
      Api().getStamp(idStamp, codeStamp).then((value) {
        stamp = value;
        setState(() {
          state = 2;
        });
      });
    } catch (e) {
      print(e);
      _subscription = controller.barcodes.listen(_handleBarcode);
      setState(() {
        state = 0;
      });
    }
  }

  @override
  void dispose() {
    _subscription?.cancel();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return ClipRRect(
      borderRadius: const BorderRadius.only(
        topLeft: Radius.circular(30),
        topRight: Radius.circular(30),
      ),
      child: BackdropFilter(
        filter: ImageFilter.blur(sigmaX: 20, sigmaY: 20),
        child: Container(
          decoration: BoxDecoration(
            color: Colors.white.withOpacity(.6),
            borderRadius: const BorderRadius.only(
              topLeft: Radius.circular(30),
              topRight: Radius.circular(30),
            ),
            border: Border.all(color: Colors.white.withOpacity(.4), width: 2),
          ),
          child: SafeArea(
            child: Column(
              mainAxisSize: MainAxisSize.min,
              crossAxisAlignment: CrossAxisAlignment.stretch,
              children: [
                const SizedBox(height: 50),
                Center(
                  child: Container(
                    padding: const EdgeInsets.all(4),
                    decoration: state == 2
                        ? null
                        : BoxDecoration(
                            border: state == 2
                                ? null
                                : Border.all(
                                    color: Colors.black,
                                    width: 8,
                                  ),
                            borderRadius: BorderRadius.circular(300),
                          ),
                    child: state == 0
                        ? Container(
                            width: MediaQuery.of(context).size.width * 0.5,
                            height: MediaQuery.of(context).size.width * 0.5,
                            decoration: BoxDecoration(
                              borderRadius: BorderRadius.circular(300),
                            ),
                            clipBehavior: Clip.antiAlias,
                            child: ColorFiltered(
                              colorFilter: ColorFilter.matrix([
                                10, 10, 10, 0, -1275,
                                // Green channel
                                10, 10, 10, 0, -1275,
                                // Blue channel
                                10, 10, 10, 0, -1275,
                                // Alpha channel
                                0, 0, 0, 1, 0,
                              ]),
                              child: MobileScanner(
                                controller: controller,
                              ),
                            ),
                          )
                        : state == 1
                            ? Container(
                                width: MediaQuery.of(context).size.width * 0.5,
                                height: MediaQuery.of(context).size.width * 0.5,
                                child: CircularProgressIndicator(),
                              )
                            : Container(
                                width: MediaQuery.of(context).size.width * 0.5,
                                height: MediaQuery.of(context).size.width * 0.5,
                                decoration: BoxDecoration(
                                  image: DecorationImage(
                                    image: NetworkImage(stamp?.image ?? ""),
                                    fit: BoxFit.cover,
                                  ),
                                ),
                                clipBehavior: Clip.antiAlias,
                              ),
                  ),
                ),
                const SizedBox(height: 20),
                Center(
                  child: Text(
                    stamp?.description ?? ("addons.tableport.addstamp.scanqr".tr()),
                    style: const TextStyle(
                      color: Colors.black,
                      fontWeight: FontWeight.bold,
                      fontSize: 20,
                    ),
                  ),
                ),
                const SizedBox(height: 50),
                if (state == 2)
                  Padding(
                    padding: const EdgeInsets.symmetric(horizontal: 20),
                    child: ElevatedButton(
                      onPressed: () {
                        Api().addStamp(idStamp, codeStamp).then((value) {
                          Navigator.of(context).pop();
                        });
                      },
                      child: Text("addons.tableport.addstamp.addstamp".tr()),
                    ),
                  ),
                TextButton(
                  onPressed: () {
                    Navigator.of(context).pop();
                  },
                  child: Text("addons.tableport.addstamp.close".tr()),
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}
