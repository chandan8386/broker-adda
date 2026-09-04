import 'package:flutter_riverpod/flutter_riverpod.dart';

import 'auth_repository.dart';

enum AuthStatus { unknown, authenticated, unauthenticated }

class AuthState {
  const AuthState({this.status = AuthStatus.unknown, this.user, this.error});
  final AuthStatus status;
  final CurrentUser? user;
  final String? error;

  AuthState copyWith({AuthStatus? status, CurrentUser? user, String? error}) =>
      AuthState(status: status ?? this.status, user: user ?? this.user, error: error);
}

class AuthController extends StateNotifier<AuthState> {
  AuthController(this._repo) : super(const AuthState()) {
    _bootstrap();
  }
  final AuthRepository _repo;

  Future<void> _bootstrap() async {
    final user = await _repo.me();
    state = AuthState(
      status: user == null ? AuthStatus.unauthenticated : AuthStatus.authenticated,
      user: user,
    );
  }

  Future<void> login(String id, String password) async {
    state = state.copyWith(error: null);
    try {
      final user = await _repo.login(id, password);
      state = AuthState(status: AuthStatus.authenticated, user: user);
    } catch (e) {
      state = AuthState(status: AuthStatus.unauthenticated, error: e.toString());
      rethrow;
    }
  }

  Future<void> logout() async {
    await _repo.logout();
    state = const AuthState(status: AuthStatus.unauthenticated);
  }

  bool hasRole(String role) => state.user?.roles.contains(role) ?? false;
}
