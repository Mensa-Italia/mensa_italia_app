import 'package:dio/dio.dart';
import 'package:mensa_italia_app/api/scraperapi.dart';
import 'package:mensa_italia_app/model/addon.dart';
import 'package:mensa_italia_app/model/sig.dart';
import 'package:mensa_italia_app/model/user.dart';
import 'package:native_dio_adapter/native_dio_adapter.dart';
import 'package:pocketbase/pocketbase.dart';

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

  Future<List<AddonModel>> getAddons() async {
    return await pb
        .collection('addons')
        .getFullList(sort: 'name')
        .then((value) {
      return value.map((e) {
        Map<String, dynamic> data = e.toJson();
        data["icon"] = pb.files.getUrl(e, e.getStringValue("icon")).toString();
        return AddonModel.fromJson(data);
      }).toList();
    });
  }

  Future<List<SigModel>> getSigs() async {
    return await pb.collection('sigs').getFullList(sort: 'name').then((value) {
      return value.map((e) {
        Map<String, dynamic> data = e.toJson();
        data["image"] =
            pb.files.getUrl(e, e.getStringValue("image")).toString();
        return SigModel.fromJson(data);
      }).toList();
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
}
