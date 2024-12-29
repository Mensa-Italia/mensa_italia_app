import 'dart:io';

import 'package:device_info_plus/device_info_plus.dart';
import 'package:dio/dio.dart';
import 'package:firebase_messaging/firebase_messaging.dart';
import 'package:image_picker/image_picker.dart';
import 'package:mensa_italia_app/api/memoized.dart';
import 'package:mensa_italia_app/api/scraperapi.dart';
import 'package:mensa_italia_app/model/addon.dart';
import 'package:mensa_italia_app/model/calendar_link.dart';
import 'package:mensa_italia_app/model/deal.dart';
import 'package:mensa_italia_app/model/deals_contact.dart';
import 'package:mensa_italia_app/model/event.dart';
import 'package:mensa_italia_app/model/event_schedule.dart';
import 'package:mensa_italia_app/model/sig.dart';
import 'package:mensa_italia_app/model/stamp.dart';
import 'package:mensa_italia_app/model/stamp_user.dart';
import 'package:mensa_italia_app/model/user.dart';
import 'package:mensa_italia_app/ui/views/map_picker/map_picker_viewmodel.dart';
import 'package:native_dio_adapter/native_dio_adapter.dart';
import 'package:pocketbase/pocketbase.dart';
import 'package:http/http.dart' as http;
import 'package:timezone/timezone.dart' as tz;

class Api {
  final Dio dio = Dio(BaseOptions(baseUrl: 'https://svc.mensa.it'));
  final pb = PocketBase('https://svc.mensa.it');
  FirebaseMessaging messaging = FirebaseMessaging.instance;

  Api._privateConstructor() {
    dio.httpClientAdapter = NativeAdapter();
  }

  static final Api _instance = Api._privateConstructor();

  factory Api() {
    return _instance;
  }

  String? _notificationToken = "NOTOKEN";
  Future<void> addDevice() async {
    try {
      NotificationSettings settings = await messaging.requestPermission(
        alert: true,
        announcement: false,
        badge: true,
        carPlay: false,
        criticalAlert: false,
        provisional: false,
        sound: true,
      );

      if (settings.authorizationStatus == AuthorizationStatus.authorized) {
        try {
          _notificationToken = await messaging.getToken();
        } catch (_) {}
        if (_notificationToken != null) {
          DeviceInfoPlugin deviceInfo = DeviceInfoPlugin();
          await removeSimilarDevice(_notificationToken ?? '');
          if (Platform.isAndroid) {
            AndroidDeviceInfo androidInfo = await deviceInfo.androidInfo;
            await pb.collection('users_devices').create(
              body: {
                "user": pb.authStore.model.id,
                "firebase_id": _notificationToken,
                "device_name": androidInfo.model,
              },
            );
          } else if (Platform.isIOS) {
            IosDeviceInfo iosInfo = await deviceInfo.iosInfo;
            await pb.collection('users_devices').create(
              body: {
                "user": pb.authStore.model.id,
                "firebase_id": _notificationToken,
                "device_name": iosInfo.utsname.machine,
              },
            );
          }
        }
      }
    } catch (_) {}
  }

  Future<void> removeSimilarDevice(String token) async {
    try {
      await pb.collection('users_devices').getFullList(query: {
        "firebase_id": token,
      }).then((value) {
        if (value.isNotEmpty) {
          for (RecordModel record in value) {
            if (record.data["user"] != pb.authStore.model.id && record.data["firebase_id"] == token) {
              pb.collection('users_devices').delete(
                record.id,
                body: {
                  "id": record.id,
                  "firebase_id": record.data["firebase_id"],
                },
              );
            }
          }
        }
      });
    } catch (_) {}
  }

  Future<void> removeThisDevice() async {
    try {
      await pb.collection('users_devices').getFullList(query: {
        "firebase_id": _notificationToken,
      }).then((value) {
        if (value.isNotEmpty) {
          for (RecordModel record in value) {
            if (record.data["user"] == pb.authStore.model.id && record.data["firebase_id"] == _notificationToken) {
              pb.collection('users_devices').delete(
                record.id,
                body: {
                  "id": record.id,
                  "firebase_id": record.data["firebase_id"],
                },
              );
            }
          }
        }
      });
    } catch (_) {}
  }

  Future<bool> login({required String email, required String password}) async {
    var formData = FormData();
    formData.fields.add(MapEntry("email", email));
    formData.fields.add(MapEntry("password", password));

    return await dio.post("/api/cs/auth-with-area", data: formData).then((value) async {
      final token = value.data["token"];
      final model = RecordModel.fromJson(value.data["record"]);
      pb.authStore.save(token, model);
      return await ScraperApi().doLoginAndRetrieveMain(email, password).then((value) {
        addDevice();
        return true;
      }).catchError((e) {
        return false;
      });
    }).catchError((e) {
      return false;
    });
  }

  Future getAddonAccessData(String addonId) {
    return dio.get("/api/cs/sign-payload/$addonId", options: Options(headers: {"Authorization": pb.authStore.token})).then((value) {
      return value.data;
    });
  }

  Future<List<SigModel>> getSigs() async {
    if (Memoized().has("all_sigs")) {
      return Memoized().get("all_sigs");
    }
    return await pb.collection('sigs').getFullList(sort: 'name').then((value) {
      Memoized().set(
          "all_sigs",
          value.map((e) {
            Map<String, dynamic> data = e.toJson();
            data["image"] = pb.files.getUrl(e, e.getStringValue("image")).toString();
            return SigModel.fromJson(data);
          }).toList());
      return Memoized().get("all_sigs");
    });
  }

  Future<List<AddonModel>> getAddons() async {
    if (Memoized().has("all_addons")) {
      return Memoized().get("all_addons");
    }
    return await pb.collection('addons').getFullList(sort: 'name').then((value) {
      Memoized().set(
          "all_addons",
          value.map((e) {
            Map<String, dynamic> data = e.toJson();
            data["icon"] = pb.files.getUrl(e, e.getStringValue("icon")).toString();
            return AddonModel.fromJson(data);
          }).toList());
      return Memoized().get("all_addons");
    });
  }

  UserModel? getUser() {
    try {
      Map<String, dynamic> data = (pb.authStore.model as RecordModel).toJson();
      data["avatar"] = pb.files.getUrl(pb.authStore.model, pb.authStore.model.getStringValue("avatar")).toString();
      return UserModel.fromJson(data);
    } catch (_) {
      return null;
    }
  }

  Future<List<EventModel>> getEvents() async {
    if (Memoized().has("all_events")) {
      return Memoized().get("all_events");
    }
    return await pb
        .collection('events')
        .getFullList(
          sort: 'when_end',
          filter: "when_end >= '${tz.TZDateTime.now(tz.local).toIso8601String()}'",
          expand: "position",
        )
        .then((value) {
      try {
        Memoized().set(
            "all_events",
            value.map((e) {
              Map<String, dynamic> data = e.toJson();
              data["image"] = pb.files.getUrl(e, e.getStringValue("image")).toString();
              return EventModel.fromJson(data);
            }).toList());
      } catch (_) {}
      return Memoized().get("all_events");
    });
  }

  Future<bool> canAddEvent() async {
    return await pb
        .collection('events')
        .getFullList(
          filter: "owner = '${pb.authStore.record!.id}' && when_end >= '${tz.TZDateTime.now(tz.local).toIso8601String()}'",
        )
        .then((value) {
      return value.isEmpty;
    });
  }

  Future<EventModel> getEvent(String id) async {
    return await pb
        .collection('events')
        .getOne(
          id,
          expand: "position",
        )
        .then((value) {
      Map<String, dynamic> data = value.toJson();
      data["image"] = pb.files.getUrl(value, value.getStringValue("image")).toString();
      final allEvents = Memoized().get("all_events");
      if (allEvents != null) {
        final index = allEvents.indexWhere((element) => element.id == id);
        if (index != -1) {
          allEvents[index] = EventModel.fromJson(data);
          Memoized().set("all_events", allEvents);
        } else {
          allEvents.add(EventModel.fromJson(data));
          Memoized().set("all_events", allEvents);
        }
      }
      return EventModel.fromJson(data);
    });
  }

  Future<EventModel> getFirstNextEvent() async {
    if (Memoized().has("first_next_event")) {
      return Memoized().get("first_next_event");
    }
    return await pb
        .collection('events')
        .getList(
          page: 1,
          perPage: 1,
          filter: "(when_start >= '${tz.TZDateTime.now(tz.local).toIso8601String()}' && is_national=true)",
          sort: 'when_start',
        )
        .then((value) {
      Memoized().set(
          "first_next_event",
          value.items.map((e) {
            Map<String, dynamic> data = e.toJson();
            data["image"] = pb.files.getUrl(e, e.getStringValue("image")).toString();
            return EventModel.fromJson(data);
          }).first);
      return Memoized().get("first_next_event");
    });
  }

  Future<SigModel> getLastInsertedSig() async {
    if (Memoized().has("last_sig")) {
      return Memoized().get("last_sig");
    }
    return await pb.collection('sigs').getList(page: 1, perPage: 1, sort: '-created').then((value) {
      Memoized().set(
          "last_sig",
          value.items.map((e) {
            Map<String, dynamic> data = e.toJson();
            data["image"] = pb.files.getUrl(e, e.getStringValue("image")).toString();
            return SigModel.fromJson(data);
          }).first);
      return Memoized().get("last_sig");
    });
  }

  Future<bool> addSig({
    required String name,
    required String link,
    required String sigType,
    XFile? image,
  }) async {
    try {
      await pb.collection('sigs').create(
        body: {
          "name": name,
          "link": link,
          "group_type": sigType,
        },
        files: image == null
            ? []
            : [
                http.MultipartFile.fromBytes(
                  'image',
                  await image.readAsBytes(),
                  filename: image.path.split("/").last,
                ),
              ],
      );
      Memoized().remove("all_sigs");
      Memoized().remove("last_sig");

      return true;
    } catch (_) {
      return false;
    }
  }

  Future<bool> updateSig({
    required String id,
    required String name,
    required String link,
    required XFile? image,
    required String sigType,
  }) async {
    try {
      await pb.collection('sigs').update(
            id,
            body: {
              "name": name,
              "link": link,
              "group_type": sigType,
            },
            files: image == null
                ? []
                : [
                    http.MultipartFile.fromBytes(
                      'image',
                      await image.readAsBytes(),
                      filename: image.path.split("/").last,
                    ),
                  ],
          );
      Memoized().remove("all_sigs");
      Memoized().remove("last_sig");

      return true;
    } catch (_) {
      return false;
    }
  }

  Future deleteSig(String id) async {
    await pb.collection('sigs').delete(id);
    Memoized().remove("all_sigs");
    Memoized().remove("last_sig");
  }

  Future createEvent({
    required String name,
    required String description,
    XFile? image,
    LocationSelected? location,
    required String link,
    required tz.TZDateTime startDate,
    required tz.TZDateTime endDate,
    required bool isNational,
    required bool isOnline,
    required bool isSpot,
    List<EventScheduleModel> schedules = const [],
  }) async {
    String? positionId;
    if (!isOnline) {
      final RecordModel createPosition = await pb.collection("positions").create(body: {
        "lat": location!.coordinates.latitude,
        "lon": location.coordinates.longitude,
        "name": location.locationName,
      });
      positionId = createPosition.id;
    }
    await pb.collection('events').create(
      body: {
        "name": name,
        "description": description,
        "info_link": link,
        "when_start": startDate.toIso8601String(),
        "when_end": endDate.toIso8601String(),
        "is_national": isNational,
        "is_spot": isSpot,
        "owner": pb.authStore.record!.id,
        if (!isOnline) "position": positionId,
      },
      files: image == null
          ? []
          : [
              http.MultipartFile.fromBytes(
                'image',
                await image.readAsBytes(),
                filename: image.path.split("/").last,
              ),
            ],
    ).then((value) async {
      for (EventScheduleModel schedule in schedules) {
        await pb.collection('events_schedule').create(
          body: {
            "title": schedule.title,
            "description": schedule.description,
            "when_start": schedule.whenStart.toIso8601String(),
            "when_end": schedule.whenEnd.toIso8601String(),
            "max_external_guests": schedule.maxExternalGuests,
            "price": schedule.price,
            "info_link": schedule.infoLink,
            "is_subscriptable": schedule.isSubscriptable,
            "event": value.id,
          },
        );
      }
    });
    Memoized().remove("all_events");
    Memoized().remove("first_next_event");
  }

  Future<void> updateEvent({
    required String id,
    required String name,
    required String description,
    XFile? image,
    LocationSelected? location,
    required String link,
    required tz.TZDateTime startDate,
    required tz.TZDateTime endDate,
    required bool isNational,
    required bool isOnline,
    required bool isSpot,
    List<EventScheduleModel> schedules = const [],
  }) async {
    String? positionId;
    if (!isOnline) {
      final RecordModel createPosition = await pb.collection("positions").create(body: {
        "lat": location!.coordinates.latitude,
        "lon": location.coordinates.longitude,
        "name": location.locationName,
      });
      positionId = createPosition.id;
    }
    await pb
        .collection('events')
        .update(
          id,
          body: {
            "name": name,
            "description": description,
            "info_link": link,
            "when_start": startDate.toIso8601String(),
            "when_end": endDate.toIso8601String(),
            "is_national": isNational,
            "is_spot": isSpot,
            if (!isOnline) "position": positionId,
          },
          files: image == null
              ? []
              : [
                  http.MultipartFile.fromBytes(
                    'image',
                    await image.readAsBytes(),
                    filename: image.path.split("/").last,
                  ),
                ],
        )
        .then((value) async {
      for (EventScheduleModel schedule in schedules) {
        if (schedule.id == null) {
          await pb.collection('events_schedule').create(
            body: {
              "title": schedule.title,
              "description": schedule.description,
              "when_start": schedule.whenStart.toIso8601String(),
              "when_end": schedule.whenEnd.toIso8601String(),
              "max_external_guests": schedule.maxExternalGuests,
              "price": schedule.price,
              "info_link": schedule.infoLink,
              "is_subscriptable": schedule.isSubscriptable,
              "event": value.id,
            },
          );
        } else if (schedule.id!.startsWith("DELETE:")) {
          try {
            await pb.collection('events_schedule').delete(schedule.id!.split(":").last);
          } catch (_) {}
        } else {
          await pb.collection('events_schedule').update(
            schedule.id!,
            body: {
              "title": schedule.title,
              "description": schedule.description,
              "when_start": schedule.whenStart.toIso8601String(),
              "when_end": schedule.whenEnd.toIso8601String(),
              "max_external_guests": schedule.maxExternalGuests,
              "price": schedule.price,
              "info_link": schedule.infoLink,
              "is_subscriptable": schedule.isSubscriptable,
              "event": value.id,
            },
          );
        }
      }
    });
    Memoized().remove("all_events");
    Memoized().remove("first_next_event");
    Memoized().remove("event_schedules_$id");
  }

  Future<List<EventScheduleModel>> getEventSchedules(String eventId) async {
    if (Memoized().has("event_schedules_$eventId")) {
      return Memoized().get("event_schedules_$eventId");
    }
    return await pb.collection('events_schedule').getFullList(filter: "event='$eventId'").then((value) {
      Memoized().set(
          "event_schedules_$eventId",
          value.map((e) {
            return EventScheduleModel.fromJson(e.toJson());
          }).toList());
      return Memoized().get("event_schedules_$eventId");
    });
  }

  Future<void> deleteEvent(String id) async {
    await pb.collection('events').delete(id);
    Memoized().remove("all_events");
    Memoized().remove("first_next_event");
  }

  Future<CalendarLinkModel> getCalendarLink() async {
    if (Memoized().has("calendar_link")) {
      return Memoized().get("calendar_link");
    }
    return await pb.collection('calendar_link').getList(page: 1, perPage: 1).then((value) {
      Memoized().set("calendar_link", CalendarLinkModel.fromJson(value.items.first.toJson()));
      return Memoized().get("calendar_link");
    });
  }

  Future<CalendarLinkModel> changeCalendarLinkState(String id, List<String> state) async {
    return await pb.collection('calendar_link').update(id, body: {"state": state}).then((value) {
      Memoized().set("calendar_link", CalendarLinkModel.fromJson(value.toJson()));
      return Memoized().get("calendar_link");
    });
  }

  void deleteEventSchedule(String split) {
    pb.collection('events_schedule').delete(split);
    Memoized().remove("all_events");
    Memoized().remove("first_next_event");
  }

  Future<List<DealModel>> getDeals() async {
    if (Memoized().has("all_deals")) {
      return Memoized().get("all_deals");
    }
    return await pb
        .collection('deals')
        .getFullList(
          sort: 'created',
          filter: "ending >= '${tz.TZDateTime.now(tz.local).toIso8601String()}'",
          expand: "position",
        )
        .then((value) {
      Memoized().set(
          "all_deals",
          value.map((e) {
            return DealModel.fromJson(e.toJson());
          }).toList());
      return Memoized().get("all_deals");
    });
  }

  Future<List<DealsContact>> getDealsContacts(String dealId) async {
    if (Memoized().has("deals_contacts_$dealId")) {
      return Memoized().get("deals_contacts_$dealId");
    }
    return await pb.collection('deals_contacts').getFullList(sort: 'created', filter: "deal='$dealId'").then((value) {
      Memoized().set(
          "deals_contacts_$dealId",
          value.map((e) {
            return DealsContact.fromJson(e.toJson());
          }).toList());
      return Memoized().get("deals_contacts_$dealId");
    });
  }

  Future addDeal({
    required String name,
    required String commercialSector,
    required String details,
    required String who,
    required String howToGet,
    required tz.TZDateTime starting,
    required tz.TZDateTime ending,
    required String link,
    required String vatNumber,
    LocationSelected? location,
    required String detailName,
    required String detailEmail,
    required String detailPhone,
    required String detailNote,
  }) async {
    String? positionId;
    if (location != null) {
      final RecordModel createPosition = await pb.collection("positions").create(body: {
        "lat": location.coordinates.latitude,
        "lon": location.coordinates.longitude,
        "name": location.locationName,
      });
      positionId = createPosition.id;
    }
    await pb.collection('deals').create(
      body: {
        "name": name,
        "commercial_sector": commercialSector,
        "details": details,
        "who": who,
        "how_to_get": howToGet,
        "link": link,
        "vat_number": vatNumber,
        "position": positionId,
        "is_active": true,
        "starting": starting.toIso8601String(),
        "ending": ending.toIso8601String(),
      },
    ).then((value) async {
      await pb.collection('deals_contacts').create(
        body: {
          "name": detailName,
          "email": detailEmail,
          "phone_number": detailPhone,
          "note": detailNote,
          "deal": value.id,
          "is_active": true,
        },
      );
    });
    Memoized().remove("all_deals");
  }

  Future updateDeal({
    required String id,
    required String name,
    required String commercialSector,
    required String details,
    required String who,
    required String howToGet,
    required tz.TZDateTime starting,
    required tz.TZDateTime ending,
    required String link,
    required String vatNumber,
    LocationSelected? location,
    String? detailId,
    String? detailName,
    String? detailEmail,
    String? detailPhone,
    String? detailNote,
  }) async {
    String? positionId;
    if (location != null) {
      final RecordModel createPosition = await pb.collection("positions").create(body: {
        "lat": location.coordinates.latitude,
        "lon": location.coordinates.longitude,
        "name": location.locationName,
      });
      positionId = createPosition.id;
    }
    await pb.collection('deals').update(
      id,
      body: {
        "name": name,
        "commercial_sector": commercialSector,
        "details": details,
        "who": who,
        "how_to_get": howToGet,
        "link": link,
        "vat_number": vatNumber,
        "position": positionId,
        "starting": starting.toIso8601String(),
        "ending": ending.toIso8601String(),
        "is_active": true,
      },
    ).then((value) async {
      if (detailId == null) {
        await pb.collection('deals_contacts').create(
          body: {
            "name": detailName,
            "email": detailEmail,
            "phone_number": detailPhone,
            "note": detailNote,
            "deal": value.id,
          },
        );
      } else {
        await pb.collection('deals_contacts').update(
          detailId,
          body: {
            "name": detailName,
            "email": detailEmail,
            "phone_number": detailPhone,
            "note": detailNote,
            "deal": value.id,
          },
        );
      }
    });
    Memoized().remove("all_deals");
  }

  Future<List<StampUserModel>> getStamps() async {
    return await pb
        .collection('stamp_users')
        .getFullList(
          expand: "stamp",
          sort: "-created",
        )
        .then(
      (value) {
        return value.map((e) {
          final data = e.toJson();
          data["expand"]["stamp"]["image"] = pb.files.getUrl(e.expand["stamp"]!.first, data["expand"]["stamp"]["image"]).toString();
          return StampUserModel.fromJson(data);
        }).toList();
      },
    );
  }

  Future addStamp(String id, String code) async {
    await pb.collection('stamp_users').create(
      body: {
        "stamp": id,
        "code": code,
        "user": pb.authStore.model.id,
      },
    );
  }

  Future<StampModel> getStamp(String id, String code) async {
    return await pb.collection('stamp').getOne(id, query: {
      "id": id,
      "code": code,
    }).then((value) {
      final data = value.toJson();
      data["image"] = pb.files.getUrl(value, data["image"]).toString();
      return StampModel.fromJson(data);
    });
  }

  Future<Map<String, String>> getMetadata() async {
    if (Memoized().has("metadata")) {
      return Memoized().get("metadata");
    }
    return await pb.collection('users_metadata').getFullList().then((value) {
      final Map<String, String> res = {};
      for (RecordModel record in value) {
        res[record.getStringValue("key").toString()] = record.getStringValue("value").toString();
      }
      Memoized().set("metadata", res);
      return res;
    });
  }

  Future<void> setMetadata(String key, String value) async {
    final settedMetadata = await pb.collection('users_metadata').getFullList(
          filter: "key='$key' && user='${pb.authStore.model.id}'",
        );
    if (settedMetadata.isNotEmpty) {
      await pb.collection('users_metadata').update(
        settedMetadata.first.id,
        body: {
          "key": key,
          "value": value,
        },
      );
    } else {
      await pb.collection('users_metadata').create(
        body: {
          "key": key,
          "value": value,
          "user": pb.authStore.model.id,
        },
      );
    }
    Memoized().remove("metadata");
  }

  Future<String> locateState(double latitude, double longitude) async {
    return dio
        .get("/api/position/state",
            queryParameters: {
              "lat": latitude,
              "lon": longitude,
            },
            options: Options(
              headers: {
                "Authorization": pb.authStore.token,
              },
            ))
        .then((value) {
      final data = value.data;
      return data["state"];
    });
  }
}
