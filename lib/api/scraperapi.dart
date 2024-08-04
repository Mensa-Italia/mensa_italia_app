import 'dart:io';

import 'package:cookie_jar/cookie_jar.dart';
import 'package:dart_rss/dart_rss.dart';
import 'package:dio/dio.dart';
import 'package:dio_cookie_manager/dio_cookie_manager.dart';
import 'package:html/dom.dart';
import 'package:mensa_italia_app/api/memoized.dart';
import 'package:mensa_italia_app/model/area_document.dart';
import 'package:mensa_italia_app/model/res_soci.dart';
import 'package:mensa_italia_app/model/testelab.dart';
import 'package:native_dio_adapter/native_dio_adapter.dart';
import 'package:path_provider/path_provider.dart';
import 'package:html/parser.dart' as html;
import 'package:shared_preferences/shared_preferences.dart';

class ScraperApi {
  final Dio dio = Dio();
  ScraperApi._privateConstructor() {
    dio.httpClientAdapter = NativeAdapter();
  }

  init() async {
    dio.httpClientAdapter = NativeAdapter();
    dio.interceptors.add(await getCookieJar());
  }

  static final ScraperApi _instance = ScraperApi._privateConstructor();

  factory ScraperApi() {
    return _instance;
  }

  Future<CookieManager> getCookieJar() async {
    Directory appDocDir = await getApplicationDocumentsDirectory();
    String appDocPath = appDocDir.path;
    var cookieJar =
        PersistCookieJar(storage: FileStorage("$appDocPath/.cookies/"));
    return CookieManager(cookieJar);
  }

  Future<File> getFile(String url) async {
    Directory appDocDir = await getApplicationDocumentsDirectory();
    String appDocPath = appDocDir.path;
    await dio.download(url, "$appDocPath/pdf.pdf");
    return File("$appDocPath/pdf.pdf");
  }

  Future<Document> getData(String link) async {
    Response response;
    response = await dio.get(link,
        options: Options(
          headers: getHeader(),
          followRedirects: false,
          validateStatus: (status) {
            return (status ?? 0) < 500;
          },
        ));

    return html.parse(response.data);
  }

  Future<String> getRawData(String link, {Map<String, String>? data}) async {
    Response response;
    if (data == null) {
      response = await dio.get(link,
          options: Options(
            headers: getHeader(),
            followRedirects: false,
            validateStatus: (status) {
              return (status ?? 0) < 500;
            },
          ));
    } else {
      response = await dio.post(link,
          data: FormData.fromMap(data),
          options: Options(
            headers: getHeader(),
            followRedirects: false,
            validateStatus: (status) {
              return (status ?? 0) < 500;
            },
          ));
    }

    return response.data;
  }

  Map<String, dynamic> getHeader() {
    return {};
  }

  Future<String> getBlogEvent() async {
    if (Memoized().has("blog")) {
      return Memoized().get("blog");
    }
    Response response;

    response = await dio.get(
        "https://www.mensa.it/?call_custom_simple_rss=1&csrp_posts_per_page=20&csrp_order=DESC&csrp_cat=9&csrp_thumbnail_size=full",
        options: Options(
          headers: getHeader(),
          followRedirects: false,
          validateStatus: (status) {
            return (status ?? 0) < 500;
          },
        ));

    Memoized().set("blog", response.data);
    return response.data;
  }

  Future<Document?> doLoginAndRetrieveMain(
      String email, String password) async {
    Response response;
    response = await dio.get(
        "https://www.cloud32.it/Associazioni/utenti/login?codass=170734",
        options: Options(
          headers: getHeader(),
          followRedirects: false,
          validateStatus: (status) {
            return (status ?? 0) < 500;
          },
        ));

    String token;
    Document document;

    document = html.parse(response.data);

    if (!response.isRedirect &&
        document
            .getElementsByTagName("input")
            .where((e) => e.attributes["name"] == "_token")
            .isNotEmpty) {
      token = (document
              .getElementsByTagName("input")
              .where((e) => e.attributes["name"] == "_token")
              .first
              .attributes["value"]) ??
          "";

      FormData formData = FormData.fromMap(
          {"email": email, "password": password, "_token": token});
      response =
          await dio.post("https://www.cloud32.it/Associazioni/utenti/login",
              data: formData,
              options: Options(
                headers: getHeader(),
                followRedirects: false,
                validateStatus: (status) {
                  return (status ?? 0) < 500;
                },
              ));
    }

    response = await dio.get("https://www.cloud32.it/Associazioni/utenti/home",
        options: Options(
          headers: getHeader(),
          followRedirects: false,
          validateStatus: (status) {
            return (status ?? 0) < 500;
          },
        ));

    document = html.parse(response.data);

    if (document
        .getElementsByTagName("img")
        .where((e) => e.attributes["alt"] == "Foto")
        .isNotEmpty) {
      savePasswordEmail(email, password);
      return document;
    } else {
      return null;
    }
  }

  savePasswordEmail(String email, String password) async {
    SharedPreferences prefs = await SharedPreferences.getInstance();
    await prefs.setString("email", email);
    await prefs.setString("password", password);
  }

  Future<bool> isPasswordEmailStored() async {
    SharedPreferences prefs = await SharedPreferences.getInstance();
    return prefs.getString("email") != null &&
        prefs.getString("password") != null;
  }

  Future<String> getStoredEmail() async {
    SharedPreferences prefs = await SharedPreferences.getInstance();
    return prefs.getString("email") ?? "";
  }

  Future<String> getStoredPassword() async {
    SharedPreferences prefs = await SharedPreferences.getInstance();
    return prefs.getString("password") ?? "";
  }

  Future logout() async {
    SharedPreferences prefs = await SharedPreferences.getInstance();
    await prefs.clear();
  }

  //https://www.cloud32.it/Associazioni/utenti/testelab
  Future<List<TestelabModel>> getTestelab(
      {required int page, String? search}) async {
    if (Memoized().has("testelab_${page}_${search ?? ""}")) {
      return Memoized().get("testelab_${page}_${search ?? ""}");
    }
    try {
      Response response;
      response = await dio.get(
        "https://www.cloud32.it/Associazioni/utenti/testelab?s_id=&s_soggetto=${Uri.encodeQueryComponent(search ?? "")}&s_provincia=&s_regione=&Ricerca=Ricerca&testord=SocioDescr&testordDir=asc&page=$page",
        options: Options(
          headers: getHeader(),
          followRedirects: false,
          validateStatus: (status) {
            return (status ?? 0) < 500;
          },
        ),
      );

      Document document = html.parse(response.data);
      List<TestelabModel> testelab = [];
      document
          .getElementsByClassName("table")
          .first
          .getElementsByTagName("tr")
          .skip(1)
          .forEach((element) {
        List<String> data = element
            .getElementsByTagName("td")
            .map((e) => e.text.trim())
            .toList();
        testelab.add(TestelabModel(
          id: data[0],
          fullname: data[1],
          typeOfTest: data[2],
          modality: data[3],
          status: data[5],
          state: data[6],
        ));
      });

      Memoized().set("testelab_${page}_${search ?? ""}", testelab);
      return testelab;
    } catch (_) {
      return [];
    }
  }

  //https://www.cloud32.it/Associazioni/utenti/regsocio?s_cognome=&s_nome=&s_citta=&s_provincia=&s_regione=&Ricerca=Ricerca
  Future<List<RegSociModel>> getRegSoci(
      {required int page, String? search}) async {
    if (Memoized().has("regsoci_${page}_${search ?? ""}")) {
      return Memoized().get("regsoci_${page}_${search ?? ""}");
    }
    try {
      String nameToSearch = "";
      String surnameToSearch = "";
      if ((search ?? "").contains(" ")) {
        List<String> splitted = search!.replaceFirst(" ", "~~~").split("~~~");
        nameToSearch = splitted.last;
        surnameToSearch = splitted.first;
      } else {
        nameToSearch = search ?? "";
        surnameToSearch = "";
      }
      List<RegSociModel> testelab = [];
      List<String> idsToNotRepeat = [];

      Response response;
      response = await dio.get(
        "https://www.cloud32.it/Associazioni/utenti/regsocio?s_cognome=${surnameToSearch}&s_nome=${nameToSearch}&s_citta=&s_provincia=&s_regione=&Ricerca=Ricerca&page=$page",
        options: Options(
          headers: getHeader(),
          followRedirects: false,
          validateStatus: (status) {
            return (status ?? 0) < 500;
          },
        ),
      );

      Document document = html.parse(response.data);
      try {
        document
            .getElementsByClassName("table")
            .first
            .getElementsByTagName("tr")
            .skip(1)
            .forEach((element) {
          List<String> data = element
              .getElementsByTagName("td")
              .map((e) => e.text.trim())
              .toList();
          if (!idsToNotRepeat.contains(data[1])) {
            testelab.add(RegSociModel(
              id: data[1],
              name: data[2],
              city: data[4],
              state: data[5],
              image:
                  "https://www.cloud32.it${element.getElementsByTagName("td")[0].getElementsByTagName("img").first.attributes["src"] ?? ""}",
              linkToFullProfile:
                  "https://www.cloud32.it${element.getElementsByTagName("td")[6].getElementsByTagName("a").first.attributes["href"] ?? ""}",
            ));
            idsToNotRepeat.add(data[1]);
          }
        });
      } catch (_) {}

      if (surnameToSearch == nameToSearch) {
        testelab.sort((a, b) => a.name.compareTo(b.name));
        Memoized().set("regsoci_${page}_${search ?? ""}", testelab);

        return testelab;
      }

      response = await dio.get(
        "https://www.cloud32.it/Associazioni/utenti/regsocio?s_cognome=${nameToSearch}&s_nome=${surnameToSearch}&s_citta=&s_provincia=&s_regione=&Ricerca=Ricerca&page=$page",
        options: Options(
          headers: getHeader(),
          followRedirects: false,
          validateStatus: (status) {
            return (status ?? 0) < 500;
          },
        ),
      );
      document = html.parse(response.data);
      try {
        document
            .getElementsByClassName("table")
            .first
            .getElementsByTagName("tr")
            .skip(1)
            .forEach((element) {
          List<String> data = element
              .getElementsByTagName("td")
              .map((e) => e.text.trim())
              .toList();
          if (!idsToNotRepeat.contains(data[1])) {
            testelab.add(RegSociModel(
              id: data[1],
              name: data[2],
              city: data[4],
              state: data[5],
              image:
                  "https://www.cloud32.it${element.getElementsByTagName("td")[0].getElementsByTagName("img").first.attributes["src"] ?? ""}",
              linkToFullProfile:
                  "https://www.cloud32.it${element.getElementsByTagName("td")[6].getElementsByTagName("a").first.attributes["href"] ?? ""}",
            ));
            idsToNotRepeat.add(data[1]);
          }
        });
      } catch (_) {}
      testelab.sort((a, b) => a.name.compareTo(b.name));

      Memoized().set("regsoci_${page}_${search ?? ""}", testelab);
      return testelab;
    } catch (_) {
      return [];
    }
  }

  Future<List<AreaDocumentModel>> getAreaDocument(
      {required int page, String? search}) async {
    if (Memoized().has("areadocument_${page}_${search ?? ""}")) {
      return Memoized().get("areadocument_${page}_${search ?? ""}");
    }
    try {
      Response response;
      response = await dio.get(
        "https://www.cloud32.it/Associazioni/utenti/documenti/docs?docdescr=$search&datada=&dataa=&tags=&page=$page",
        options: Options(
          headers: getHeader(),
          followRedirects: false,
          validateStatus: (status) {
            return (status ?? 0) < 500;
          },
        ),
      );

      Document document = html.parse(response.data);
      List<AreaDocumentModel> testelab = [];
      document
          .getElementsByClassName("table")
          .first
          .getElementsByTagName("tr")
          .skip(1)
          .forEach((element) {
        List<String> data = element
            .getElementsByTagName("td")
            .map((e) => e.text.trim())
            .toList();
        testelab.add(AreaDocumentModel(
          description: data[1],
          image:
              "https://www.cloud32.it${element.getElementsByTagName("td")[4].getElementsByTagName("img").first.attributes["src"] ?? ""}",
          dimension: data[6],
          link:
              "https://www.cloud32.it${element.getElementsByTagName("td")[4].getElementsByTagName("a").first.attributes["href"] ?? ""}",
        ));
      });

      Memoized().set("areadocument_${page}_${search ?? ""}", testelab);

      return testelab;
    } catch (_) {
      return [];
    }
  }

  Future<Map<String, String>> getRegSocioDeepData(String url) async {
    if (Memoized().has("regsocio_$url")) {
      return Memoized().get("regsocio_$url");
    }
    Response response;
    response = await dio.get(
      url,
      options: Options(
        headers: getHeader(),
        followRedirects: false,
        validateStatus: (status) {
          return (status ?? 0) < 500;
        },
      ),
    );

    Document document = html.parse(response.data);

    Map<String, String> data =
        Map.fromEntries(document.getElementsByClassName("form-group").map((e) {
      try {
        var key = e.getElementsByTagName("div").first.text.trim();
        var value = "";
        try {
          value = e.getElementsByTagName("label").last.text.trim();
        } catch (_) {
          value = e.getElementsByTagName("a").last.attributes["href"] ?? "";
        }
        var mapEntry = MapEntry(key, value);
        return mapEntry;
      } catch (e) {
        return MapEntry("", "");
      }
    }).toList());

    Memoized().set("regsocio_$url", data);

    return data;
  }

  Future<String> getMyProfileSetting({required String name}) async {
    if (Memoized().has("myprofilesetting_$name")) {
      return Memoized().get("myprofilesetting_$name");
    }
    Response response;
    response = await dio.get(
      "https://www.cloud32.it/Associazioni/utenti/home",
      options: Options(
        headers: getHeader(),
        followRedirects: false,
        validateStatus: (status) {
          return (status ?? 0) < 500;
        },
      ),
    );

    Document document = html.parse(response.data);

    for (var element in document.getElementsByTagName("a")) {
      if (element.text.trim().toLowerCase() == name) {
        Memoized().set("myprofilesetting_$name",
            "https://www.cloud32.it${element.attributes["href"]}");
        return "https://www.cloud32.it${element.attributes["href"]}";
      }
    }

    return "";
  }

  Future<RssFeed> getBlog() async {
    return RssFeed.parse(await getBlogEvent());
  }
}
