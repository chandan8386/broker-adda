import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import 'dashboard_repository.dart';

class DashboardScreen extends ConsumerWidget {
  const DashboardScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final async = ref.watch(dashboardProvider);
    return RefreshIndicator(
      onRefresh: () async => ref.refresh(dashboardProvider.future),
      child: async.when(
        loading: () => const Center(child: CircularProgressIndicator()),
        error: (e, _) => ListView(children: [Padding(padding: const EdgeInsets.all(24), child: Text('Error: $e'))]),
        data: (d) {
          final tiles = <List<dynamic>>[
            ['Total Leads', d.f('totalLeads')],
            ['New Leads', d.f('newLeads')],
            ["Today's Follow-ups", d.f('todaysFollowUps')],
            ['Overdue', d.f('overdueFollowUps')],
            ['Interested', d.f('interestedLeads')],
            ['Site Visits Sched.', d.f('siteVisitsScheduled')],
            ['Site Visits Done', d.f('siteVisitsCompleted')],
            ['Bookings', d.f('bookings')],
            ['Purchases', d.f('purchases')],
            ['Lost', d.f('lostLeads')],
            ['Conversion %', d.conversion],
            ['Sales Value', d.salesValue],
          ];
          return GridView.count(
            crossAxisCount: 2,
            padding: const EdgeInsets.all(12),
            childAspectRatio: 1.7,
            mainAxisSpacing: 10,
            crossAxisSpacing: 10,
            children: tiles
                .map((t) => Card(
                      child: Padding(
                        padding: const EdgeInsets.all(12),
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          mainAxisAlignment: MainAxisAlignment.center,
                          children: [
                            Text('${t[0]}'.toUpperCase(),
                                style: const TextStyle(fontSize: 10, color: Colors.black54, fontWeight: FontWeight.w600)),
                            const SizedBox(height: 4),
                            Text('${t[1]}', style: const TextStyle(fontSize: 22, fontWeight: FontWeight.bold)),
                          ],
                        ),
                      ),
                    ))
                .toList(),
          );
        },
      ),
    );
  }
}
