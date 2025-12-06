import 'package:mensa_italia_app/api/api.dart';
import 'package:mensa_italia_app/app/app.router.dart';
import 'package:mensa_italia_app/model/location.dart';
import 'package:mensa_italia_app/ui/common/master_model.dart';
import 'package:mensa_italia_app/ui/views/map_picker/map_picker_viewmodel.dart';

class LocationListPickerViewModel extends MasterModel {
  @override
  String componentName = "views.location_list_picker.title";
  List<LocationModel> locations = [];
  void addLocation() {
    navigationService.navigateToMapPickerView(
      previousPageTitle: componentName,).then((value) {
      if (value != null && value is LocationSelected) {
        Api()
            .createLocation(
          name: value.locationName,
          address: value.locationAddress,
          latitude: value.coordinates.latitude,
          longitude: value.coordinates.longitude,
        )
            .then((location) {
          locations.add(location);
          rebuildUi();
        });
      }
    });
  }

  LocationListPickerViewModel() {
    init();
  }

  init() {
    setBusy(true);
    Api().getLocaitons().then((value) {
      locations.clear();
      locations.addAll(value);
      setBusy(false);
    });
  }

  Function() deleteLocation(String id) {
    return () {
      Api().deleteLocation(id).then((value) {
        locations.removeWhere((element) => element.id == id);
        rebuildUi();
      });
    };
  }
}
