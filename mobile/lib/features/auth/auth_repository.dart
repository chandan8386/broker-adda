import '../../core/api_client.dart';
import '../../core/token_store.dart';

class CurrentUser {
  CurrentUser({required this.id, required this.fullName, required this.roles});
  final int id;
  final String fullName;
  final List<String> roles;

  factory CurrentUser.fromJson(Map<String, dynamic> j) => CurrentUser(
        id: j['id'] as int,
        fullName: j['fullName'] as String,
        roles: (j['roles'] as List).map((e) => e.toString()).toList(),
      );
}

class AuthRepository {
  AuthRepository(this._api, this._tokens);
  final ApiClient _api;
  final TokenStore _tokens;

  Future<CurrentUser> login(String usernameOrEmail, String password) async {
    final res = await _api.dio.post('/auth/login', data: {
      'usernameOrEmail': usernameOrEmail,
      'password': password,
    });
    await _tokens.save(res.data['accessToken'], res.data['refreshToken']);
    return CurrentUser.fromJson(res.data['user'] as Map<String, dynamic>);
  }

  Future<CurrentUser?> me() async {
    if (await _tokens.accessToken == null) return null;
    try {
      final res = await _api.dio.get('/auth/me');
      return CurrentUser.fromJson(res.data as Map<String, dynamic>);
    } catch (_) {
      await _tokens.clear();
      return null;
    }
  }

  Future<void> logout() async {
    try {
      await _api.dio.post('/auth/logout');
    } catch (_) {}
    await _tokens.clear();
  }
}
