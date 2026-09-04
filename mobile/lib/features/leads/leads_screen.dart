import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../core/theme.dart';
import 'lead_models.dart';
import 'lead_repository.dart';

class LeadsScreen extends ConsumerStatefulWidget {
  const LeadsScreen({super.key});

  @override
  ConsumerState<LeadsScreen> createState() => _LeadsScreenState();
}

class _LeadsScreenState extends ConsumerState<LeadsScreen> {
  final _scroll = ScrollController();
  final _search = TextEditingController();
  final List<LeadListItem> _items = [];
  int _page = 0;
  bool _loading = false;
  bool _last = false;
  String _query = '';

  @override
  void initState() {
    super.initState();
    _load(reset: true);
    _scroll.addListener(() {
      if (_scroll.position.pixels > _scroll.position.maxScrollExtent - 300) _load();
    });
  }

  Future<void> _load({bool reset = false}) async {
    if (_loading || (_last && !reset)) return;
    setState(() => _loading = true);
    if (reset) {
      _page = 0;
      _last = false;
      _items.clear();
    }
    try {
      final res = await ref.read(leadRepositoryProvider).list(page: _page, query: _query);
      setState(() {
        _items.addAll(res.content);
        _last = res.last;
        _page++;
      });
    } catch (e) {
      if (mounted) ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text('$e')));
    } finally {
      if (mounted) setState(() => _loading = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Column(
      children: [
        Padding(
          padding: const EdgeInsets.all(12),
          child: TextField(
            controller: _search,
            decoration: InputDecoration(
              hintText: 'Search name / mobile / email',
              prefixIcon: const Icon(Icons.search),
              suffixIcon: IconButton(
                icon: const Icon(Icons.arrow_forward),
                onPressed: () {
                  _query = _search.text.trim();
                  _load(reset: true);
                },
              ),
            ),
            onSubmitted: (v) {
              _query = v.trim();
              _load(reset: true);
            },
          ),
        ),
        Expanded(
          child: RefreshIndicator(
            onRefresh: () => _load(reset: true),
            child: ListView.separated(
              controller: _scroll,
              itemCount: _items.length + 1,
              separatorBuilder: (_, __) => const Divider(height: 1),
              itemBuilder: (context, i) {
                if (i == _items.length) {
                  return Padding(
                    padding: const EdgeInsets.all(16),
                    child: Center(
                      child: _loading ? const CircularProgressIndicator() : Text(_last ? 'End of list' : ''),
                    ),
                  );
                }
                final l = _items[i];
                return ListTile(
                  title: Text(l.customerName, style: const TextStyle(fontWeight: FontWeight.w600)),
                  subtitle: Text('${l.reference} · ${l.mobile}${l.assignedUserName != null ? ' · ${l.assignedUserName}' : ''}'),
                  trailing: Chip(
                    label: Text(l.status, style: const TextStyle(fontSize: 10, color: Colors.white)),
                    backgroundColor: statusColor(l.status),
                    visualDensity: VisualDensity.compact,
                  ),
                  onTap: () => context.push('/leads/${l.id}'),
                );
              },
            ),
          ),
        ),
      ],
    );
  }
}
