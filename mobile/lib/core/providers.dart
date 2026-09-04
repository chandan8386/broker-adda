import 'package:flutter_riverpod/flutter_riverpod.dart';

import 'api_client.dart';
import 'token_store.dart';
import '../features/auth/auth_repository.dart';
import '../features/auth/auth_controller.dart';

final tokenStoreProvider = Provider((ref) => TokenStore());

final apiClientProvider = Provider((ref) => ApiClient(ref.watch(tokenStoreProvider)));

final authRepositoryProvider = Provider(
  (ref) => AuthRepository(ref.watch(apiClientProvider), ref.watch(tokenStoreProvider)),
);

final authControllerProvider =
    StateNotifierProvider<AuthController, AuthState>((ref) => AuthController(ref.watch(authRepositoryProvider)));
