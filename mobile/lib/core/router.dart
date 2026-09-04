import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import 'providers.dart';
import '../features/auth/auth_controller.dart';
import '../features/auth/login_screen.dart';
import '../features/shell/home_shell.dart';
import '../features/leads/lead_detail_screen.dart';

final routerProvider = Provider<GoRouter>((ref) {
  return GoRouter(
    initialLocation: '/',
    redirect: (context, state) {
      final auth = ref.read(authControllerProvider);
      final loggingIn = state.matchedLocation == '/login';
      if (auth.status == AuthStatus.unknown) return null;
      if (auth.status == AuthStatus.unauthenticated) return loggingIn ? null : '/login';
      if (loggingIn) return '/';
      return null;
    },
    refreshListenable: _AuthListenable(ref),
    routes: [
      GoRoute(path: '/login', builder: (_, __) => const LoginScreen()),
      GoRoute(path: '/', builder: (_, __) => const HomeShell()),
      GoRoute(
        path: '/leads/:id',
        builder: (_, s) => LeadDetailScreen(leadId: int.parse(s.pathParameters['id']!)),
      ),
    ],
  );
});

class _AuthListenable extends ChangeNotifier {
  _AuthListenable(Ref ref) {
    ref.listen(authControllerProvider, (_, __) => notifyListeners());
  }
}
