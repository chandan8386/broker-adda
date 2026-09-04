import 'package:flutter_secure_storage/flutter_secure_storage.dart';

class TokenStore {
  static const _storage = FlutterSecureStorage();
  static const _access = 'stir_access';
  static const _refresh = 'stir_refresh';

  Future<String?> get accessToken => _storage.read(key: _access);
  Future<String?> get refreshToken => _storage.read(key: _refresh);

  Future<void> save(String access, String refresh) async {
    await _storage.write(key: _access, value: access);
    await _storage.write(key: _refresh, value: refresh);
  }

  Future<void> clear() async {
    await _storage.delete(key: _access);
    await _storage.delete(key: _refresh);
  }
}
