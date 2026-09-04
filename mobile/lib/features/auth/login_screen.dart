import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../core/config.dart';
import '../../core/providers.dart';

class LoginScreen extends ConsumerStatefulWidget {
  const LoginScreen({super.key});

  @override
  ConsumerState<LoginScreen> createState() => _LoginScreenState();
}

class _LoginScreenState extends ConsumerState<LoginScreen> {
  final _id = TextEditingController(text: 'caller1@trishakti.com');
  final _pwd = TextEditingController(text: 'Password@123');
  bool _busy = false;
  String? _error;

  Future<void> _submit() async {
    setState(() {
      _busy = true;
      _error = null;
    });
    try {
      await ref.read(authControllerProvider.notifier).login(_id.text.trim(), _pwd.text);
    } catch (e) {
      setState(() => _error = ref.read(apiClientProvider).errorMessage(e));
    } finally {
      if (mounted) setState(() => _busy = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: Center(
        child: SingleChildScrollView(
          padding: const EdgeInsets.all(24),
          child: ConstrainedBox(
            constraints: const BoxConstraints(maxWidth: 420),
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                const Icon(Icons.apartment, size: 48, color: Color(0xFF0F766E)),
                const SizedBox(height: 12),
                Text(AppConfig.companyName,
                    textAlign: TextAlign.center,
                    style: Theme.of(context).textTheme.titleLarge?.copyWith(fontWeight: FontWeight.bold)),
                const Text('Property Lead Management / CRM', style: TextStyle(color: Colors.black54)),
                const SizedBox(height: 24),
                Card(
                  child: Padding(
                    padding: const EdgeInsets.all(16),
                    child: Column(
                      children: [
                        TextField(
                          controller: _id,
                          decoration: const InputDecoration(labelText: 'Username or Email'),
                        ),
                        const SizedBox(height: 12),
                        TextField(
                          controller: _pwd,
                          obscureText: true,
                          decoration: const InputDecoration(labelText: 'Password'),
                        ),
                        const SizedBox(height: 8),
                        if (_error != null)
                          Text(_error!, style: const TextStyle(color: Colors.red)),
                        const SizedBox(height: 8),
                        SizedBox(
                          width: double.infinity,
                          child: FilledButton(
                            onPressed: _busy ? null : _submit,
                            child: Text(_busy ? 'Signing in…' : 'Sign in'),
                          ),
                        ),
                      ],
                    ),
                  ),
                ),
                const SizedBox(height: 12),
                const Text('Demo: caller1@trishakti.com / Password@123',
                    style: TextStyle(fontSize: 12, color: Colors.black45)),
              ],
            ),
          ),
        ),
      ),
    );
  }
}
