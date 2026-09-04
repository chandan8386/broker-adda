import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:intl/intl.dart';

import '../../core/providers.dart';

final _followUpsProvider = FutureProvider.autoDispose((ref) async {
  final dio = ref.read(apiClientProvider).dio;
  final res = await dio.get('/calls/follow-ups/mine', queryParameters: {'size': 50, 'sort': 'dueAt,asc'});
  return (res.data['content'] as List).cast<Map<String, dynamic>>();
});

class FollowUpsScreen extends ConsumerWidget {
  const FollowUpsScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final async = ref.watch(_followUpsProvider);
    return async.when(
      loading: () => const Center(child: CircularProgressIndicator()),
      error: (e, _) => Center(child: Text('$e')),
      data: (items) {
        if (items.isEmpty) {
          return const Center(child: Text('No pending follow-ups 🎉'));
        }
        return RefreshIndicator(
          onRefresh: () async => ref.refresh(_followUpsProvider.future),
          child: ListView.separated(
            itemCount: items.length,
            separatorBuilder: (_, __) => const Divider(height: 1),
            itemBuilder: (context, i) {
              final f = items[i];
              final due = DateTime.tryParse(f['dueAt'] as String? ?? '');
              final overdue = due != null && due.isBefore(DateTime.now());
              return ListTile(
                title: Text(f['leadName'] as String? ?? 'Lead'),
                subtitle: Text(
                  'Due ${due != null ? DateFormat('dd MMM, HH:mm').format(due.toLocal()) : '—'} · ${f['channel']}',
                  style: TextStyle(color: overdue ? Colors.red : null, fontWeight: overdue ? FontWeight.bold : null),
                ),
                trailing: TextButton(
                  child: const Text('Done'),
                  onPressed: () async {
                    await ref.read(apiClientProvider).dio.post(
                      '/calls/follow-ups/${f['id']}/complete',
                      data: {'note': 'Completed from mobile'},
                    );
                    ref.invalidate(_followUpsProvider);
                  },
                ),
                onTap: () => context.push('/leads/${f['leadId']}'),
              );
            },
          ),
        );
      },
    );
  }
}
