import 'package:flutter/rendering.dart';
import 'package:mensa_italia_app/api/api.dart';
import 'package:mensa_italia_app/model/device.dart';
import 'package:mensa_italia_app/ui/common/master_model.dart';
import 'package:stacked/stacked.dart';

class DevicesViewModel extends MasterModel {
  List<DeviceModel> devices = [];

  DevicesViewModel() {
    Api().getDevices().then((value) {
      devices = value;
      rebuildUi();
    });
  }

  void deleteDevice(DeviceModel device) {
    Api().deleteDevice(device.id).then((value) {
      devices.remove(device);
      rebuildUi();
    });
  }
}
