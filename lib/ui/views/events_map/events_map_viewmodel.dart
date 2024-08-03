import 'package:flutter/services.dart';
import 'package:maplibre_gl/maplibre_gl.dart';
import 'package:mensa_italia_app/api/api.dart';
import 'package:mensa_italia_app/model/event.dart';
import 'package:mensa_italia_app/ui/common/master_model.dart';
import 'package:url_launcher/url_launcher_string.dart';

class EventsMapViewModel extends MasterModel {
  MapLibreMapController? mapController;

  List<EventModel> events = [];

  load() {
    Api().getEvents().then((value) {
      events = value;
      loadEventsAsMarkers();
    });
  }

  void onUserLocationUpdated(UserLocation location) {}

  void onMapCreated(MapLibreMapController controller) {
    mapController = controller;
    loadMarkerImage('assets/images/marker.png').then((value) {
      controller.addImage('marker_cs_image', value).then((value) {
        loadMarkerImage('assets/images/marker_blue.png').then((value2) {
          return controller.addImage('marker_cs_image_blue', value2);
        });
      });
    });
    controller.onSymbolTapped.add(onSymbolTapped);
  }

  void onSymbolTapped(Symbol symbol) async {
    final event = (symbol.data!['event'] as EventModel);
    if (event.infoLink.trim().isNotEmpty && await canLaunchUrlString(event.infoLink.trim())) {
      launchUrlString(
        event.infoLink.trim(),
      );
    } else {
      dialogService.showDialog(
        title: 'Not ready yet',
        description: 'This event is being prepared, please try again later.',
      );
    }
  }

  void onStyleLoadedCallback() {
    mapController?.requestMyLocationLatLng().then((value) async {
      if (value != null) {
        mapController?.moveCamera(
          CameraUpdate.newLatLngZoom(
            LatLng(value.latitude, value.longitude),
            8,
          ),
        );
      }
    });
    load();
  }

  void loadEventsAsMarkers() {
    for (var event in events) {
      if (event.position == null) {
        continue;
      }
      mapController?.addSymbol(
        SymbolOptions(
          geometry: event.position!.toLatLng(),
          iconImage: event.isNational ? 'marker_cs_image' : 'marker_cs_image_blue',
          iconSize: event.isNational ? 0.35 : 0.25,
          textField: event.isNational ? event.name : null,
          iconAnchor: 'bottom',
          textColor: "#184295",
          textAnchor: 'top',
          textHaloColor: '#ffffff',
          textHaloWidth: 2,
          textOffset: const Offset(0, 0),
        ),
        {
          'event': event,
        },
      );
    }
  }
}

Future<Uint8List> loadMarkerImage(String image) async {
  try {
    var byteData = await rootBundle.load(image);
    return byteData.buffer.asUint8List();
  } catch (e) {
    print(e);
    return Uint8List(0);
  }


  void advancedMapUsage() async{
    
  }
}
