import 'package:dio/dio.dart';
import 'package:image_picker/image_picker.dart';
import 'package:mensa_italia_app/api/memoized.dart';
import 'package:mensa_italia_app/api/scraperapi.dart';
import 'package:mensa_italia_app/model/addon.dart';
import 'package:mensa_italia_app/model/event.dart';
import 'package:mensa_italia_app/model/sig.dart';
import 'package:mensa_italia_app/model/user.dart';
import 'package:native_dio_adapter/native_dio_adapter.dart';
import 'package:pocketbase/pocketbase.dart';
import 'package:http/http.dart' as http;

class Api {
  final Dio dio = Dio(BaseOptions(baseUrl: 'https://svc.mensa.it'));
  final pb = PocketBase('https://svc.mensa.it');

  Api._privateConstructor() {
    dio.httpClientAdapter = NativeAdapter();
  }

  static final Api _instance = Api._privateConstructor();

  factory Api() {
    return _instance;
  }

  Future<bool> login({required String email, required String password}) async {
    var formData = FormData();
    formData.fields.add(MapEntry("email", email));
    formData.fields.add(MapEntry("password", password));

    return await dio
        .post("/api/cs/auth-with-area", data: formData)
        .then((value) async {
      final token = value.data["token"];
      final model = RecordModel.fromJson(value.data["record"]);
      pb.authStore.save(token, model);
      return await ScraperApi()
          .doLoginAndRetrieveMain(email, password)
          .then((value) {
        return true;
      }).catchError((e) {
        return false;
      });
    }).catchError((e) {
      return false;
    });
  }

  Future getAddonAccessData(String addonId) {
    return dio
        .get("/api/cs/sign-payload/$addonId",
            options: Options(headers: {"Authorization": pb.authStore.token}))
        .then((value) {
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
            data["image"] =
                pb.files.getUrl(e, e.getStringValue("image")).toString();
            return SigModel.fromJson(data);
          }).toList());
      return Memoized().get("all_sigs");
    });
  }

  Future<List<AddonModel>> getAddons() async {
    if (Memoized().has("all_addons")) {
      return Memoized().get("all_addons");
    }
    return await pb
        .collection('addons')
        .getFullList(sort: 'name')
        .then((value) {
      Memoized().set(
          "all_addons",
          value.map((e) {
            Map<String, dynamic> data = e.toJson();
            data["icon"] =
                pb.files.getUrl(e, e.getStringValue("icon")).toString();
            return AddonModel.fromJson(data);
          }).toList());
      return Memoized().get("all_addons");
    });
  }

  UserModel? getUser() {
    try {
      Map<String, dynamic> data = (pb.authStore.model as RecordModel).toJson();
      data["avatar"] = pb.files
          .getUrl(
              pb.authStore.model, pb.authStore.model.getStringValue("avatar"))
          .toString();
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
          sort: 'when',
          filter: "when >= '${DateTime.now().toIso8601String()}'",
          expand: "position",
        )
        .then((value) {
      Memoized().set(
          "all_events",
          value.map((e) {
            Map<String, dynamic> data = e.toJson();
            data["image"] =
                pb.files.getUrl(e, e.getStringValue("image")).toString();
            return EventModel.fromJson(data);
          }).toList());
      return Memoized().get("all_events");
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
            filter: "when >= '${DateTime.now().toIso8601String()}'",
            sort: 'when')
        .then((value) {
      Memoized().set(
          "first_next_event",
          value.items.map((e) {
            Map<String, dynamic> data = e.toJson();
            data["image"] =
                pb.files.getUrl(e, e.getStringValue("image")).toString();
            return EventModel.fromJson(data);
          }).first);
      return Memoized().get("first_next_event");
    });
  }

  Future<SigModel> getLastInsertedSig() async {
    if (Memoized().has("last_sig")) {
      return Memoized().get("last_sig");
    }
    return await pb
        .collection('sigs')
        .getList(page: 1, perPage: 1, sort: '-created')
        .then((value) {
      Memoized().set(
          "last_sig",
          value.items.map((e) {
            Map<String, dynamic> data = e.toJson();
            data["image"] =
                pb.files.getUrl(e, e.getStringValue("image")).toString();
            return SigModel.fromJson(data);
          }).first);
      return Memoized().get("last_sig");
    });
  }

  Future<bool> addSig(
      {required String name,
      required String link,
      required XFile image}) async {
    try {
      await pb.collection('sigs').create(
        body: {
          "name": name,
          "link": link,
        },
        files: [
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

  Future<bool> updateSig(
      {required String id,
      required String name,
      required String link,
      required XFile? image}) async {
    try {
      await pb.collection('sigs').update(
            id,
            body: {
              "name": name,
              "link": link,
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
}
