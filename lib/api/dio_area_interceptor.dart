import 'package:dio/dio.dart';
import 'package:dio_cookie_manager/dio_cookie_manager.dart';
import 'package:mensa_italia_app/api/api.dart';
import 'package:mensa_italia_app/api/scraperapi.dart';

class DioAreaInterceptor extends Interceptor {
  final Dio _dio = Dio();

  @override
  void onResponse(Response response, ResponseInterceptorHandler handler) async {
    final staticResponse = response.requestOptions;
    if (!response.isRedirect) {
      return handler.next(response);
    }
    final location = response.headers.value("location");
    if (!location.toString().toLowerCase().contains("Associazioni/login".toLowerCase())) {
      return handler.next(response);
    }
    if (response.isRedirect && location.toString().toLowerCase().contains("Associazioni/login".toLowerCase())) {
      final email = await ScraperApi().getStoredEmail();
      final password = await ScraperApi().getStoredPassword();
      final cookieJar = await ScraperApi().getCookieJar();
      if (_dio.interceptors.where((element) => element.runtimeType == CookieManager).isEmpty) {
        _dio.interceptors.add(cookieJar);
      }
      await cookieJar.cookieJar.deleteAll();
      return await Api().login(email: email, password: password).then((isLogged) async {
        if (isLogged) {
          return await _dio.fetch(staticResponse).then((value) {
            return handler.next(value);
          }).catchError((error) {
            return handler.reject(error);
          });
        } else {
          return handler.reject(DioException(
            requestOptions: staticResponse,
            response: response,
            error: "Auth failed",
          ));
        }
      }).catchError((error) {
        return handler.reject(DioException(
          requestOptions: staticResponse,
          response: response,
          error: error,
        ));
      });
    } else {
      return handler.next(response);
    }
  }
}
