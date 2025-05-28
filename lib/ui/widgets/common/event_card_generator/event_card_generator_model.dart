import 'dart:typed_data';
import 'package:flutter/widgets.dart';
import 'package:mensa_italia_app/ui/common/master_model.dart';
import 'package:stacked/stacked.dart';
import 'package:http/http.dart' as http;

class EventCardGeneratorModel extends MasterModel {
  Uri imageUrl = Uri.parse("https://svc.mensa.it/static/event_card_template.png");
  String? _shortTitle;
  String? _date;
  String? _time;
  String? _location;
  String? _address;
  String? _city;
  final formKey = GlobalKey<FormState>();
  Uint8List? generatedImage;

  void setShortTitle(String? newValue) {
    _shortTitle = newValue;
  }

  void setDate(String? newValue) {
    _date = newValue;
  }

  void setTime(String? newValue) {
    _time = newValue;
  }

  void setRestaurantName(String? newValue) {
    _location = newValue;
  }

  void setAddress(String? newValue) {
    _address = newValue;
  }

  void setCity(String? newValue) {
    _city = newValue;
  }

  void generate() {
    if (!formKey.currentState!.validate()) {
      return;
    }
    formKey.currentState!.save();
    final queryParameters = {
      'title': _shortTitle,
      'line0': _date,
      'line1': _time,
      'line2': _location,
      'line3': _address,
      'line4': _city,
    };

    imageUrl = Uri.parse("https://svc.mensa.it/api/cs/generate-event-card").replace(queryParameters: queryParameters);

    setBusy(true);

    () async {
      final response = await http.get(imageUrl);
      if (response.statusCode == 200) {
        generatedImage = response.bodyBytes;
        setBusy(false);
      } else {
        setBusy(false);
      }
    }();
  }

  void sendBack() {
    navigationService.back(result: generatedImage);
  }
}
