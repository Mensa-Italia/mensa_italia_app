import 'package:dio/dio.dart';
import 'package:dio_cookie_manager/dio_cookie_manager.dart';
import 'package:mensa_italia_app/api/api.dart';
import 'package:mensa_italia_app/api/scraperapi.dart';

class DioAreaInterceptor extends Interceptor {
  final Dio _dio = Dio();

  @override
  void onResponse(Response response, ResponseInterceptorHandler handler) async {
    if (response.requestOptions.uri.toString().toLowerCase().contains("/Associazioni/utenti/login".toLowerCase())) {
      return handler.next(response);
    }
    if (response.isRedirect && response.realUri.toString().toLowerCase().contains("/Associazioni/utenti/login".toLowerCase())) {
      final email = await ScraperApi().getStoredEmail();
      final password = await ScraperApi().getStoredPassword();
      final cookieJar = await ScraperApi().getCookieJar();
      if (_dio.interceptors.where((element) => element.runtimeType == CookieManager).isEmpty) {
        _dio.interceptors.add(cookieJar);
      }

      await cookieJar.cookieJar.deleteAll();

      print("Trying to relogin");
      Api().login(email: email, password: password).then((isLogged) {
        if (isLogged) {
          _dio.fetch(response.requestOptions).then((value) {
            handler.next(value);
          }).catchError((error) {
            handler.reject(error);
          });
        } else {
          handler.reject(DioException(
            requestOptions: response.requestOptions,
            response: response,
            error: "Auth failed",
          ));
        }
      }).catchError((error) {
        handler.reject(DioException(
          requestOptions: response.requestOptions,
          response: response,
          error: error,
        ));
      });
    } else {
      handler.next(response);
    }
  }
}
