import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:intl/intl.dart';

import '../../core/theme.dart';
import 'lead_models.dart';
import 'lead_repository.dart';

final _leadProvider = FutureProvider.autoDispose.family<LeadDetail, int>((ref, id) {
  return ref.read(leadRepositoryProvider).get(id);
});
final _activitiesProvider = FutureProvider.autoDispose.family<List<LeadActivity>, int>((ref, id) {
  return ref.read(leadRepositoryProvider).activities(id);
});

class LeadDetailScreen extends ConsumerWidget {
  const LeadDetailScreen({super.key, required this.leadId});
  final int leadId;

  String _fmt(String? iso) {
    if (iso == null) return '—';
    try {
      return DateFormat('dd MMM yyyy, HH:mm').format(DateTime.parse(iso).toLocal());
    } catch (_) {
      return iso;
    }
  }

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final leadAsync = ref.watch(_leadProvider(leadId));
    final actsAsync = ref.watch(_activitiesProvider(leadId));

    return Scaffold(
      appBar: AppBar(title: const Text('Lead')),
      body: leadAsync.when(
        loading: () => const Center(child: CircularProgressIndicator()),
        error: (e, _) => Center(child: Text('$e')),
        data: (lead) => RefreshIndicator(
          onRefresh: () async {
            ref.invalidate(_leadProvider(leadId));
            ref.invalidate(_activitiesProvider(leadId));
          },
          child: ListView(
            padding: const EdgeInsets.all(12),
            children: [
              Card(
                child: Padding(
                  padding: const EdgeInsets.all(14),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Row(
                        children: [
                          Expanded(
                            child: Text(lead.customerName,
                                style: const TextStyle(fontSize: 18, fontWeight: FontWeight.bold)),
                          ),
                          Chip(
                            label: Text(lead.status, style: const TextStyle(fontSize: 10, color: Colors.white)),
                            backgroundColor: statusColor(lead.status),
                          ),
                        ],
                      ),
                      Text(lead.reference, style: const TextStyle(color: Colors.black45, fontSize: 12)),
                      const Divider(),
                      _row('Mobile', lead.mobile),
                      _row('Email', lead.email ?? '—'),
                      _row('Source', lead.source ?? '—'),
                      _row('Budget', '${lead.budgetMin ?? '—'} – ${lead.budgetMax ?? '—'}'),
                      _row('Location', lead.preferredLocation ?? '—'),
                      _row('Assigned', lead.assignedUserName ?? 'Unassigned'),
                      _row('Next follow-up', _fmt(lead.nextFollowUpAt)),
                      if ((lead.notes ?? '').isNotEmpty) _row('Notes', lead.notes!),
                    ],
                  ),
                ),
              ),
              const SizedBox(height: 8),
              Row(
                children: [
                  Expanded(
                    child: OutlinedButton.icon(
                      icon: const Icon(Icons.call),
                      label: const Text('Log call'),
                      onPressed: () => _logCallSheet(context, ref),
                    ),
                  ),
                  const SizedBox(width: 8),
                  Expanded(
                    child: OutlinedButton.icon(
                      icon: const Icon(Icons.sync_alt),
                      label: const Text('Change status'),
                      onPressed: () => _transitionSheet(context, ref, lead.status),
                    ),
                  ),
                ],
              ),
              const SizedBox(height: 12),
              const Text('Activity timeline', style: TextStyle(fontWeight: FontWeight.bold)),
              const SizedBox(height: 6),
              actsAsync.when(
                loading: () => const Padding(padding: EdgeInsets.all(16), child: LinearProgressIndicator()),
                error: (e, _) => Text('$e'),
                data: (acts) => Column(
                  children: acts
                      .map((a) => Card(
                            child: ListTile(
                              dense: true,
                              title: Text(a.summary),
                              subtitle: Text('${a.actorName} · ${_fmt(a.occurredAt)}'
                                  '${a.detail != null ? '\n${a.detail}' : ''}'),
                              isThreeLine: a.detail != null,
                            ),
                          ))
                      .toList(),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }

  Widget _row(String k, String v) => Padding(
        padding: const EdgeInsets.symmetric(vertical: 3),
        child: Row(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            SizedBox(width: 110, child: Text(k, style: const TextStyle(color: Colors.black54))),
            Expanded(child: Text(v, style: const TextStyle(fontWeight: FontWeight.w500))),
          ],
        ),
      );

  Future<void> _logCallSheet(BuildContext context, WidgetRef ref) async {
    String outcome = 'CONNECTED';
    String disposition = 'FOLLOW_UP';
    final notes = TextEditingController();
    await showModalBottomSheet(
      context: context,
      isScrollControlled: true,
      builder: (ctx) => Padding(
        padding: EdgeInsets.only(bottom: MediaQuery.of(ctx).viewInsets.bottom, left: 16, right: 16, top: 16),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            const Text('Log call', style: TextStyle(fontWeight: FontWeight.bold, fontSize: 16)),
            DropdownButtonFormField(
              value: outcome,
              decoration: const InputDecoration(labelText: 'Outcome'),
              items: const ['CONNECTED', 'NO_ANSWER', 'BUSY', 'SWITCHED_OFF', 'CALL_BACK_LATER', 'NOT_REACHABLE']
                  .map((e) => DropdownMenuItem(value: e, child: Text(e)))
                  .toList(),
              onChanged: (v) => outcome = v ?? outcome,
            ),
            DropdownButtonFormField(
              value: disposition,
              decoration: const InputDecoration(labelText: 'Disposition'),
              items: const ['INTERESTED', 'NOT_INTERESTED', 'FOLLOW_UP', 'SITE_VISIT', 'DO_NOT_CALL']
                  .map((e) => DropdownMenuItem(value: e, child: Text(e)))
                  .toList(),
              onChanged: (v) => disposition = v ?? disposition,
            ),
            TextField(controller: notes, decoration: const InputDecoration(labelText: 'Notes')),
            const SizedBox(height: 12),
            SizedBox(
              width: double.infinity,
              child: FilledButton(
                onPressed: () async {
                  await ref.read(leadRepositoryProvider).logCall(leadId,
                      outcome: outcome, disposition: disposition, notes: notes.text);
                  if (ctx.mounted) Navigator.pop(ctx);
                  ref.invalidate(_leadProvider(leadId));
                  ref.invalidate(_activitiesProvider(leadId));
                },
                child: const Text('Save call'),
              ),
            ),
            const SizedBox(height: 16),
          ],
        ),
      ),
    );
  }

  Future<void> _transitionSheet(BuildContext context, WidgetRef ref, String current) async {
    // Common next-steps; backend validates the actual transition.
    const targets = [
      'CALLING', 'CONNECTED', 'NOT_CONNECTED', 'INTERESTED', 'NOT_INTERESTED',
      'SITE_VISIT_SCHEDULED', 'NEGOTIATION', 'LOST', 'CLOSED',
    ];
    await showModalBottomSheet(
      context: context,
      builder: (ctx) => ListView(
        shrinkWrap: true,
        children: targets
            .map((t) => ListTile(
                  title: Text(t),
                  onTap: () async {
                    Navigator.pop(ctx);
                    try {
                      String? svAt;
                      if (t == 'SITE_VISIT_SCHEDULED') {
                        svAt = DateTime.now().add(const Duration(days: 2)).toUtc().toIso8601String();
                      }
                      await ref.read(leadRepositoryProvider).transition(leadId, t, siteVisitAt: svAt);
                      ref.invalidate(_leadProvider(leadId));
                      ref.invalidate(_activitiesProvider(leadId));
                    } catch (e) {
                      if (context.mounted) {
                        ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text('$e')));
                      }
                    }
                  },
                ))
            .toList(),
      ),
    );
  }
}
