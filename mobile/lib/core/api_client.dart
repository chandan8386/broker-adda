import 'package:dio/dio.dart';

import 'config.dart';
import 'token_store.dart';

/// Thin Dio wrapper shared by every repository. Adds the JWT bearer header and
/// transparently refreshes the access token once on a 401.
class ApiClient {
  ApiClient(this._tokens) {
    _dio = Dio(
      BaseOptions(
        baseUrl: AppConfig.apiBaseUrl,
        connectTimeout: const Duration(seconds: 15),
        receiveTimeout: const Duration(seconds: 20),
        headers: {'Content-Type': 'application/json'},
      ),
    );

    _dio.interceptors.add(
      InterceptorsWrapper(
        onRequest: (options, handler) async {
          final token = await _tokens.accessToken;
          if (token != null) {
            options.headers['Authorization'] = 'Bearer $token';
          }
          handler.next(options);
        },
        onError: (e, handler) async {
          final isAuthCall = e.requestOptions.path.contains('/auth/');
          if (e.response?.statusCode == 401 && !isAuthCall && !_retried(e)) {
            try {
              final refreshed = await _refresh();
              if (refreshed) {
                final req = e.requestOptions;
                req.extra['retried'] = true;
                final token = await _tokens.accessToken;
                req.headers['Authorization'] = 'Bearer $token';
                final clone = await _dio.fetch(req);
                return handler.resolve(clone);
              }
            } catch (_) {
              await _tokens.clear();
            }
          }
          handler.next(e);
        },
      ),
    );
  }

  final TokenStore _tokens;
  late final Dio _dio;

  Dio get dio => _dio;

  bool _retried(DioException e) => e.requestOptions.extra['retried'] == true;

  Future<bool> _refresh() async {
    final refresh = await _tokens.refreshToken;
    if (refresh == null) return false;
    final res = await Dio(BaseOptions(baseUrl: AppConfig.apiBaseUrl)).post(
      '/auth/refresh',
      data: {'refreshToken': refresh},
    );
    await _tokens.save(res.data['accessToken'], res.data['refreshToken']);
    return true;
  }

  String errorMessage(Object error) {
    if (error is DioException) {
      final data = error.response?.data;
      if (data is Map && data['message'] != null) return data['message'].toString();
      return error.message ?? 'Network error';
    }
    return error.toString();
  }
}
